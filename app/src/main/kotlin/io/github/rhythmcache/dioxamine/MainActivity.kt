package io.github.rhythmcache.dioxamine

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rhythmcache.dioxamine.adb.*
import io.github.rhythmcache.dioxamine.core.*
import io.github.rhythmcache.dioxamine.fastboot.FastbootScreen
import io.github.rhythmcache.dioxamine.fastboot.FastbootViewModel
import io.github.rhythmcache.dioxamine.fastboot.ListenForFastbootDevices
import io.github.rhythmcache.dioxamine.scrcpy.ScrcpyScreen
import io.github.rhythmcache.dioxamine.settings.SettingsScreen
import java.io.File

enum class Tab(@StringRes val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ADB(R.string.tab_adb, Icons.Filled.PhoneAndroid),
    SCRCPY(R.string.tab_scrcpy, Icons.AutoMirrored.Filled.ScreenShare),
    FASTBOOT(R.string.tab_fastboot, Icons.Filled.Bolt),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings)
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        if (getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("keep_alive_enabled", false)) {
            DioxForegroundService.start(this)
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
            var theme by remember { mutableStateOf(runCatching { AppTheme.valueOf(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM") }.getOrDefault(AppTheme.SYSTEM)) }
            var monet by remember { mutableStateOf(prefs.getBoolean("use_monet", false)) }
            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "theme_mode" -> theme = runCatching { AppTheme.valueOf(p.getString(key, "SYSTEM") ?: "SYSTEM") }.getOrDefault(AppTheme.SYSTEM)
                        "use_monet" -> monet = p.getBoolean(key, false)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            DioxamineTheme(appTheme = theme, useMonet = monet) { FluxDevXApp(filesDir) }
        }
    }
}

@Composable
fun FluxDevXApp(keyDir: File) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(Tab.ADB) }
    val vm: AdbViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdbViewModel(keyDir) as T
    })
    val fastbootVm: FastbootViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val pluginRepo = remember { io.github.rhythmcache.dioxamine.plugin.PluginRepository(context.applicationContext, scope) }
    val permissionStore = remember { io.github.rhythmcache.dioxamine.plugin.PluginPermissionStore(context.applicationContext) }
    val permissionGate = remember { io.github.rhythmcache.dioxamine.plugin.PluginPermissionGate(store = permissionStore) }
    val dialogGate = remember { io.github.rhythmcache.dioxamine.plugin.PluginDialogGate() }
    val safBridge = remember { io.github.rhythmcache.dioxamine.plugin.PluginSafBridge(context.applicationContext) }

    io.github.rhythmcache.dioxamine.plugin.PluginPermissionDialogHost(permissionGate)
    io.github.rhythmcache.dioxamine.plugin.PluginDialogHost(dialogGate)
    io.github.rhythmcache.dioxamine.plugin.PluginSafLauncherHost(safBridge)
    ListenForUsbDevices(vm)
    ListenForFastbootDevices(fastbootVm)

    val adbCount = vm.devices.values.count { it.state is ConnectionState.Connected }
    val fastbootCount = if (fastbootVm.isConnected) 1 else fastbootVm.devices.size
    LaunchedEffect(adbCount, fastbootCount) { DioxForegroundService.updateDeviceCounts(context, adbCount, fastbootCount) }

    var scrcpyFullscreen by remember { mutableStateOf(false) }
    var pluginActive by remember { mutableStateOf(false) }
    DisposableEffect(scrcpyFullscreen) {
        val window = (context as? ComponentActivity)?.window
        window?.let {
            val controller = WindowCompat.getInsetsController(it, it.decorView)
            if (scrcpyFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { (context as? ComponentActivity)?.window?.let { WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) } }
    }

    val immersive = scrcpyFullscreen || pluginActive
    BackHandler(enabled = selectedTab != Tab.ADB && !immersive) { selectedTab = Tab.ADB }
    BackHandler(enabled = scrcpyFullscreen) { scrcpyFullscreen = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { if (!immersive) FluxDevXTopBar(adbCount, fastbootCount) },
        bottomBar = {
            if (!immersive) NavigationBar(tonalElevation = 8.dp) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = if (immersive) Modifier.fillMaxSize() else Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                Tab.ADB -> Column(Modifier.fillMaxSize()) {
                    FluxDevXConnectionCard(adbCount, fastbootCount)
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        AdbScreen(vm, pluginRepo, permissionGate, dialogGate, safBridge, onPluginActiveChange = { pluginActive = it })
                    }
                }
                Tab.SCRCPY -> ScrcpyScreen(vm, onFullScreenChange = { scrcpyFullscreen = it })
                Tab.FASTBOOT -> FastbootScreen(fastbootVm)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun FluxDevXTopBar(adbCount: Int, fastbootCount: Int) {
    Surface(tonalElevation = 3.dp, shadowElevation = 5.dp) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("FluxDevX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Device command center", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(if (adbCount + fastbootCount > 0) "ONLINE" else "READY", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FluxDevXMetric(Modifier.weight(1f), Icons.Filled.PhoneAndroid, "ADB", adbCount)
                FluxDevXMetric(Modifier.weight(1f), Icons.Filled.Bolt, "FASTBOOT", fastbootCount)
                FluxDevXMetric(Modifier.weight(1f), Icons.Filled.Security, "SECURE", 1)
            }
        }
    }
}

@Composable
private fun FluxDevXMetric(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int
) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(7.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FluxDevXConnectionCard(adbCount: Int, fastbootCount: Int) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Connection overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (adbCount + fastbootCount == 0) "Connect a device to unlock tools" else "$adbCount ADB • $fastbootCount Fastboot active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (adbCount + fastbootCount > 0) Icons.Filled.CheckCircle else Icons.Filled.LinkOff, contentDescription = null, tint = if (adbCount + fastbootCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
