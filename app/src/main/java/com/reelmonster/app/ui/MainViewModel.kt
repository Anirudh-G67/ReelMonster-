package com.reelmonster.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.MutablePreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reelmonster.app.data.AppDb
import com.reelmonster.app.data.AppSettings
import com.reelmonster.app.data.DashState
import com.reelmonster.app.data.K
import com.reelmonster.app.data.buildDash
import com.reelmonster.app.data.dayTicker
import com.reelmonster.app.data.editSettings
import com.reelmonster.app.data.settingsFlow
import com.reelmonster.app.overlay.OverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app

    /** Bumped from Activity.onResume so permission status is re-read after returning from system settings. */
    var tick by mutableIntStateOf(0)
        private set

    fun refresh() { tick++ }

    val settings: StateFlow<AppSettings> =
        ctx.settingsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val dash: StateFlow<DashState?> =
        combine(dayTicker(ctx), settings) { d, s -> d to s }
            .flatMapLatest { (d, s) ->
                AppDb.get(ctx).events()
                    .observeSince(d.startOf(d.date.minusDays(364)))
                    .map { buildDash(it, d, s) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun edit(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch { ctx.editSettings(block) }
    }

    fun startMonster() {
        edit { it[K.serviceEnabled] = true }
        OverlayService.start(ctx)
    }

    fun stopMonster() {
        edit { it[K.serviceEnabled] = false }
        ctx.stopService(Intent(ctx, OverlayService::class.java))
    }

    fun clearAll() {
        viewModelScope.launch { AppDb.get(ctx).events().clear() }
    }

    fun exportCsv(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val list = AppDb.get(ctx).events().all()
                val z = ZoneId.systemDefault()
                ctx.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { w ->
                    w.write("timestamp_iso,local_date,local_time,source,mode\n")
                    for (e in list) {
                        val t = Instant.ofEpochMilli(e.timestamp).atZone(z)
                        w.write("${t.toOffsetDateTime()},${t.toLocalDate()},${t.toLocalTime().withNano(0)},${e.source},${e.mode}\n")
                    }
                }
            }.isSuccess
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }
}
