package com.reelmonster.app.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.reelmonster.app.MainActivity
import com.reelmonster.app.R
import com.reelmonster.app.data.AppDb
import com.reelmonster.app.data.AppSettings
import com.reelmonster.app.data.K
import com.reelmonster.app.data.Mode
import com.reelmonster.app.data.PKG_IG
import com.reelmonster.app.data.PKG_YT
import com.reelmonster.app.data.Repo
import com.reelmonster.app.data.Source
import com.reelmonster.app.data.ViewEvent
import com.reelmonster.app.data.dayTicker
import com.reelmonster.app.data.editSettings
import com.reelmonster.app.data.Day
import com.reelmonster.app.data.settingsFlow
import com.reelmonster.app.monster.MonsterView
import com.reelmonster.app.monster.STAGE_MESSAGES
import com.reelmonster.app.monster.milestoneMessage
import com.reelmonster.app.monster.stageIndex
import com.reelmonster.app.ui.ReelMonsterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayService : Service() {

    companion object {
        const val ACTION_STOP = "com.reelmonster.app.STOP"
        const val ACTION_TOGGLE = "com.reelmonster.app.TOGGLE_HIDE"
        private const val CHANNEL = "monster_service"
        private const val NOTIF_ID = 1

        @Volatile var running = false

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, OverlayService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val owner = OverlayOwner()
    private lateinit var wm: WindowManager

    private var bubble: ComposeView? = null
    private var bubbleLp: WindowManager.LayoutParams? = null
    private var panel: ComposeView? = null
    private var panelJob: Job? = null
    private var positionLoaded = false

    private var settings by mutableStateOf(AppSettings())
    private var total by mutableIntStateOf(0)
    private var bump by mutableIntStateOf(0)
    private var hidden by mutableStateOf(false)

    private var prevTotal = -1
    private var lastDate: java.time.LocalDate? = null
    private var lastFg: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        owner.resume()
        createChannel()
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Reel Monster needs the overlay permission", Toast.LENGTH_LONG).show()
            scope.launch { editSettings { it[K.serviceEnabled] = false } }
            stopSelf()
            return
        }
        running = true
        addBubble()
        observe()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> scope.launch {
                editSettings { it[K.serviceEnabled] = false }
                stopSelf()
            }
            ACTION_TOGGLE -> {
                hidden = !hidden
                updateNotification()
                updateVisibility()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        dismissPanel()
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        owner.destroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- data

    private fun observe() {
        scope.launch { settingsFlow().collect { settings = it; applyLayout() } }
        scope.launch {
            dayTicker(applicationContext)
                .flatMapLatest { day ->
                    AppDb.get(applicationContext).events().observeBetween(day.start, day.end).map { day to it }
                }
                .collect { (day, list) -> onData(day, list) }
        }
        scope.launch {
            while (true) {
                updateVisibility()
                delay(1000)
            }
        }
    }

    private fun onData(day: Day, list: List<ViewEvent>) {
        if (lastDate != day.date) { // new local day: evolution resets, history is untouched
            lastDate = day.date
            prevTotal = -1
        }
        val newTotal = list.size
        total = newTotal
        if (prevTotal >= 0 && newTotal > prevTotal) onCountUp(prevTotal, newTotal)
        prevTotal = newTotal
        updateNotification()
    }

    private fun onCountUp(old: Int, new: Int) {
        bump++
        if (settings.haptics) vibrate()
        val msg = milestoneMessage(new)
            ?: if (settings.goal > 0 && settings.goalWarning && new == settings.goal)
                "Daily goal of ${settings.goal} reached! Gentle nudge: stretch break?"
            else if (Random.nextInt(100) < 18) STAGE_MESSAGES[stageIndex(new)].random()
            else null
        if (msg != null && stageIndex(new) >= 0) showMessage(msg)
    }

    // ---------------------------------------------------------------- bubble

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun composeView(content: @Composable () -> Unit) = ComposeView(this).apply {
        setViewTreeLifecycleOwner(owner)
        setViewTreeViewModelStoreOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        setContent(content)
    }

    private fun addBubble() {
        val dm = resources.displayMetrics
        val size = dp(settings.sizeDp)
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dm.widthPixels - size
            y = dm.heightPixels / 3
        }
        val v = composeView { ReelMonsterTheme { BubbleContent() } }
        v.setOnTouchListener(BubbleTouch(lp))
        wm.addView(v, lp)
        bubble = v
        bubbleLp = lp
    }

    private fun applyLayout() {
        val lp = bubbleLp ?: return
        val v = bubble ?: return
        val dm = resources.displayMetrics
        val size = dp(settings.sizeDp)
        lp.width = size
        lp.height = size
        if (!positionLoaded) {
            positionLoaded = true
            if (settings.posX >= 0) {
                lp.x = settings.posX
                lp.y = settings.posY
            }
        }
        lp.x = if (lp.x + size / 2 < dm.widthPixels / 2) 0 else dm.widthPixels - size
        lp.y = lp.y.coerceIn(0, (dm.heightPixels - size).coerceAtLeast(0))
        v.alpha = settings.opacity
        runCatching { wm.updateViewLayout(v, lp) }
    }

    private inner class BubbleTouch(private val lp: WindowManager.LayoutParams) : View.OnTouchListener {
        private val slop = ViewConfiguration.get(this@OverlayService).scaledTouchSlop
        private val handler = Handler(Looper.getMainLooper())
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false
        private var longFired = false
        private val longRun = Runnable {
            if (!dragging) {
                longFired = true
                showMenu()
            }
        }

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val dm = resources.displayMetrics
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = lp.x; startY = lp.y
                    dragging = false; longFired = false
                    handler.postDelayed(longRun, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) {
                        dragging = true
                        handler.removeCallbacks(longRun)
                        dismissPanel()
                    }
                    if (dragging) {
                        lp.x = (startX + dx).toInt().coerceIn(0, (dm.widthPixels - lp.width).coerceAtLeast(0))
                        lp.y = (startY + dy).toInt().coerceIn(0, (dm.heightPixels - lp.height).coerceAtLeast(0))
                        runCatching { wm.updateViewLayout(v, lp) }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longRun)
                    if (dragging) snapToEdge() else if (!longFired) onTap()
                }
                MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(longRun)
            }
            return true
        }
    }

    private fun onTap() {
        dismissPanel()
        scope.launch { Repo.record(applicationContext, settings.currentType, Mode.MANUAL) }
    }

    private fun snapToEdge() {
        val lp = bubbleLp ?: return
        val v = bubble ?: return
        val dm = resources.displayMetrics
        val target = if (lp.x + lp.width / 2 < dm.widthPixels / 2) 0 else dm.widthPixels - lp.width
        ValueAnimator.ofInt(lp.x, target).apply {
            duration = 220
            interpolator = OvershootInterpolator()
            addUpdateListener {
                lp.x = it.animatedValue as Int
                runCatching { wm.updateViewLayout(v, lp) }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    lp.x = target
                    runCatching { wm.updateViewLayout(v, lp) }
                    savePosition()
                }
            })
            start()
        }
    }

    private fun savePosition() {
        val lp = bubbleLp ?: return
        val x = lp.x
        val y = lp.y
        scope.launch { editSettings { it[K.posX] = x; it[K.posY] = y } }
    }

    @Composable
    private fun BubbleContent() {
        Box(Modifier.fillMaxSize()) {
            MonsterView(count = total, bump = bump, modifier = Modifier.fillMaxSize())
            val isReel = settings.currentType == Source.REEL
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size((settings.sizeDp * 0.28f).dp)
                    .clip(CircleShape)
                    .background(if (isReel) Color(0xFFE1306C) else Color(0xFFE53935)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isReel) "R" else "S",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (settings.sizeDp * 0.15f).sp
                )
            }
            PlusOne()
        }
    }

    @Composable
    private fun BoxScope.PlusOne() {
        val a = remember { Animatable(0f) }
        LaunchedEffect(bump) {
            if (bump > 0) {
                a.snapTo(0f)
                a.animateTo(1f, tween(750))
            }
        }
        val p = a.value
        if (p > 0f && p < 1f) {
            Text(
                "+1",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-(settings.sizeDp * (0.12f + 0.28f * p))).dp)
                    .alpha(1f - p),
                style = TextStyle(
                    color = Color(0xFFFFEB3B),
                    fontWeight = FontWeight.Black,
                    fontSize = (settings.sizeDp * 0.3f).sp,
                    shadow = Shadow(Color.Black, Offset(0f, 2f), 6f)
                )
            )
        }
    }

    // ---------------------------------------------------------------- panel (menu + messages)

    private fun showPanel(widthDp: Int, heightDp: Int, autoDismissMs: Long, touchable: Boolean, content: @Composable () -> Unit) {
        dismissPanel()
        val b = bubbleLp ?: return
        val dm = resources.displayMetrics
        val w = dp(widthDp)
        val h = dp(heightDp)
        val left = b.x + b.width / 2 < dm.widthPixels / 2
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        flags = if (touchable) flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        else flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val lp = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (if (left) b.x + b.width + dp(6) else b.x - w - dp(6)).coerceIn(0, (dm.widthPixels - w).coerceAtLeast(0))
            y = b.y.coerceIn(0, (dm.heightPixels - h).coerceAtLeast(0))
        }
        val v = composeView { ReelMonsterTheme { content() } }
        v.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) dismissPanel()
            false
        }
        runCatching { wm.addView(v, lp) }.onSuccess { panel = v }
        if (autoDismissMs > 0) panelJob = scope.launch { delay(autoDismissMs); dismissPanel() }
    }

    private fun dismissPanel() {
        panelJob?.cancel()
        panelJob = null
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
    }

    private fun showMessage(text: String) {
        showPanel(240, 96, 3800, touchable = false) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Text(
                    text,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }
        }
    }

    private fun showMenu() {
        if (settings.haptics) vibrate()
        showPanel(220, 214, 0, touchable = true) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                tonalElevation = 4.dp
            ) {
                Column(Modifier.padding(8.dp)) {
                    MenuItem("↩  Undo last") { dismissPanel(); undoLast() }
                    MenuItem(
                        if (settings.currentType == Source.REEL) "🔁  Switch to Short" else "🔁  Switch to Reel"
                    ) { dismissPanel(); switchType() }
                    MenuItem("📊  Open dashboard") { dismissPanel(); openDashboard() }
                    MenuItem("🙈  Hide") { dismissPanel(); hide() }
                }
            }
        }
    }

    @Composable
    private fun MenuItem(label: String, onClick: () -> Unit) {
        Text(
            label,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // ---------------------------------------------------------------- menu actions

    private fun undoLast() {
        scope.launch {
            val ok = Repo.undo(applicationContext)
            showMessage(if (ok) "Undone! One fewer." else "Nothing to undo today.")
        }
    }

    private fun switchType() {
        val next = if (settings.currentType == Source.REEL) Source.SHORT else Source.REEL
        scope.launch {
            editSettings { it[K.type] = next }
            showMessage("Now counting ${Source.label(next)}s")
        }
    }

    private fun openDashboard() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    private fun hide() {
        hidden = true
        updateNotification()
        updateVisibility()
        Toast.makeText(this, "Monster hidden. Use the notification to show it again.", Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------- visibility / foreground app

    private fun updateVisibility() {
        val v = bubble ?: return
        var show = true
        if (settings.onlyTargetApps && hasUsageAccess(this)) {
            val fg = foregroundPackage()
            show = fg == PKG_IG || fg == PKG_YT
        }
        val vis = if (show && !hidden) View.VISIBLE else View.GONE
        if (v.visibility != vis) {
            v.visibility = vis
            if (vis == View.GONE) dismissPanel()
        }
    }

    @Suppress("DEPRECATION")
    private fun foregroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) lastFg = e.packageName
            else if (e.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND && e.packageName == lastFg) lastFg = null
        }
        return lastFg
    }

    // ---------------------------------------------------------------- notification, haptics

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Floating monster", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps the floating monster running"
            }
        )
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flags
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), flags
        )
        val toggle = PendingIntent.getService(
            this, 2, Intent(this, OverlayService::class.java).setAction(ACTION_TOGGLE), flags
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Reel Monster is watching")
            .setContentText("Today: $total ${if (total == 1) "view" else "views"}")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Stop", stop)
            .addAction(0, if (hidden) "Show monster" else "Hide monster", toggle)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vib: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vib.vibrate(VibrationEffect.createOneShot(30, 120))
    }
}

fun hasUsageAccess(ctx: Context): Boolean {
    val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = if (Build.VERSION.SDK_INT >= 29) {
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
    } else {
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
