package com.reelmonster.app.auto

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.reelmonster.app.data.AppSettings
import com.reelmonster.app.data.Mode
import com.reelmonster.app.data.PKG_IG
import com.reelmonster.app.data.PKG_YT
import com.reelmonster.app.data.Repo
import com.reelmonster.app.data.Source
import com.reelmonster.app.data.settingsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque

data class DebugEntry(val time: Long, val pkg: String, val type: String, val note: String)

/** In-memory only. Contains event types, package, class names and view-ID names. Never text content. */
object DebugLog {
    private val _entries = MutableStateFlow<List<DebugEntry>>(emptyList())
    val entries: StateFlow<List<DebugEntry>> = _entries

    fun add(pkg: String, type: String, note: String) {
        val short = when (pkg) { PKG_IG -> "IG"; PKG_YT -> "YT"; else -> pkg }
        _entries.update { (it + DebugEntry(System.currentTimeMillis(), short, type, note)).takeLast(300) }
    }

    fun clear() { _entries.value = emptyList() }
}

object AutoBridge {
    @Volatile var service: ReelAccessibilityService? = null
}

/**
 * LEVEL 2 (EXPERIMENTAL / UNTESTED). Restricted by accessibility_service_config.xml to Instagram and YouTube,
 * and inert unless the user gave consent in the app (autoEnabled).
 *
 * Rule: a SCROLL event in a target app, while a view whose ID contains a configured keyword is on screen,
 * counts as "new Reel/Short", subject to a debounce window and an in-memory same-content check.
 * Only view-ID names are matched. Text is never stored, logged or sent (a transient hash is used for the
 * same-content check and is kept in memory only).
 */
class ReelAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var cfg = AppSettings()
    private var lastCountAt = 0L
    private var lastSig = 0
    private var lastLogAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        AutoBridge.service = this
        scope.launch { settingsFlow().collect { cfg = it } }
        DebugLog.add("-", "SERVICE", "connected")
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AutoBridge.service = null
        DebugLog.add("-", "SERVICE", "unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AutoBridge.service = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        val source = when (pkg) {
            PKG_IG -> Source.REEL
            PKG_YT -> Source.SHORT
            else -> return
        }
        val c = cfg
        if (!c.autoEnabled) return
        val now = SystemClock.elapsedRealtime()
        when (e.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(e, pkg, source, c, now)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (now - lastLogAt > 2000) {
                    lastLogAt = now
                    DebugLog.add(pkg, typeName(e.eventType), "class=${e.className}")
                }
            }
        }
    }

    private fun handleScroll(e: AccessibilityEvent, pkg: String, source: String, c: AppSettings, now: Long) {
        val root = rootInActiveWindow
        if (root == null) {
            DebugLog.add(pkg, "SCROLL", "ignored: no window root")
            return
        }
        if (root.packageName?.toString() != pkg) {
            recycleSafe(root)
            DebugLog.add(pkg, "SCROLL", "ignored: active window is another package")
            return
        }
        val kw = (if (source == Source.REEL) c.igKeywords else c.ytKeywords)
            .split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val (matched, sig) = scan(root, kw)
        recycleSafe(root)
        if (matched == null) {
            DebugLog.add(pkg, "SCROLL", "ignored: no view ID matched keywords ($kw) class=${e.className}")
            return
        }
        if (now - lastCountAt < c.debounceMs) {
            DebugLog.add(pkg, "SCROLL", "ignored: debounce (<${c.debounceMs}ms) id=$matched")
            return
        }
        if (sig != 0 && sig == lastSig) {
            DebugLog.add(pkg, "SCROLL", "ignored: same content as last count id=$matched")
            return
        }
        lastCountAt = now
        lastSig = sig
        DebugLog.add(pkg, "SCROLL", "COUNTED id=$matched")
        scope.launch { Repo.record(applicationContext, source, Mode.AUTO) }
    }

    /** Returns (first matching view-ID name, in-memory content signature). */
    private fun scan(root: AccessibilityNodeInfo, keywords: List<String>): Pair<String?, Int> {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        var matched: String? = null
        var sig = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val n = queue.poll() ?: break
            visited++
            val id = n.viewIdResourceName
            if (id != null) {
                val low = id.lowercase()
                if (keywords.any { low.contains(it) }) {
                    if (matched == null) matched = id.substringAfter(":id/")
                    sig = sig * 31 + (n.contentDescription?.hashCode() ?: 0) + (n.text?.hashCode() ?: 0)
                }
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it) }
            if (n !== root) recycleSafe(n)
        }
        while (queue.isNotEmpty()) queue.poll()?.let { recycleSafe(it) }
        return matched to sig
    }

    /** Waits so the user can switch to Instagram/YouTube, then captures. */
    fun captureSnapshotDelayed(delayMs: Long) {
        DebugLog.add("-", "SNAPSHOT", "will capture in ${delayMs / 1000}s, switch to Instagram or YouTube now")
        scope.launch {
            kotlinx.coroutines.delay(delayMs)
            captureSnapshot()
        }
    }

    /** Debug aid for Phase 2: logs view-ID names and class names (never text) of the current screen. */
    fun captureSnapshot() {
        val root = rootInActiveWindow
        val pkg = root?.packageName?.toString()
        if (root == null || (pkg != PKG_IG && pkg != PKG_YT)) {
            root?.let { recycleSafe(it) }
            DebugLog.add("-", "SNAPSHOT", "open Instagram or YouTube first (active window is not a target app)")
            return
        }
        val seen = LinkedHashSet<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 800) {
            val n = queue.poll() ?: break
            visited++
            val id = n.viewIdResourceName
            if (id != null) seen.add("${id.substringAfter(":id/")} [${n.className?.toString()?.substringAfterLast('.')}]")
            for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it) }
            if (n !== root) recycleSafe(n)
        }
        while (queue.isNotEmpty()) queue.poll()?.let { recycleSafe(it) }
        recycleSafe(root)
        DebugLog.add(pkg, "SNAPSHOT", "${seen.size} distinct view IDs (nodes visited: $visited)")
        seen.take(60).forEach { DebugLog.add(pkg, "SNAPSHOT", it) }
    }

    @Suppress("DEPRECATION")
    private fun recycleSafe(n: AccessibilityNodeInfo) {
        runCatching { n.recycle() }
    }

    private fun typeName(t: Int) = when (t) {
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "SCROLL"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "CONTENT"
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW"
        else -> "OTHER($t)"
    }

    companion object {
        private const val MAX_NODES = 500
    }
}
