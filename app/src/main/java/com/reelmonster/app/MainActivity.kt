package com.reelmonster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reelmonster.app.ui.AutoScreen
import com.reelmonster.app.ui.DashboardScreen
import com.reelmonster.app.ui.HelpScreen
import com.reelmonster.app.ui.MainViewModel
import com.reelmonster.app.ui.ReelMonsterTheme
import com.reelmonster.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ReelMonsterTheme { AppRoot(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("auto", "Auto", Icons.Rounded.AutoAwesome),
    Tab("help", "Why we need this", Icons.Rounded.Info),
    Tab("settings", "Settings", Icons.Rounded.Settings)
)

@Composable
private fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(if (t.route == "help") "Help" else t.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { DashboardScreen(vm) }
            composable("auto") { AutoScreen(vm) }
            composable("help") { HelpScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}
