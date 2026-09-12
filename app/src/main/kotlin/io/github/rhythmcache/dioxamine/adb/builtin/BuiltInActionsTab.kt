package io.github.rhythmcache.dioxamine.adb.builtin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rhythmcache.dioxamine.R
import io.github.rhythmcache.dioxamine.adb.AdbViewModel

import io.github.rhythmcache.dioxamine.adb.builtin.filemanager.FileManagerScreen
import io.github.rhythmcache.dioxamine.adb.builtin.filemanager.FileManagerTile
import io.github.rhythmcache.dioxamine.adb.builtin.misc.MiscScreen
import io.github.rhythmcache.dioxamine.adb.builtin.misc.MiscTile
import io.github.rhythmcache.dioxamine.adb.builtin.packagemanager.PackageManagerScreen
import io.github.rhythmcache.dioxamine.adb.builtin.packagemanager.PackageManagerTile
import io.github.rhythmcache.dioxamine.adb.builtin.processmanager.ProcessManagerScreen
import io.github.rhythmcache.dioxamine.adb.builtin.processmanager.ProcessManagerTile
import io.github.rhythmcache.dioxamine.adb.builtin.reboot.RebootScreen
import io.github.rhythmcache.dioxamine.adb.builtin.reboot.RebootTile
import io.github.rhythmcache.dioxamine.adb.builtin.remotecontrol.RemoteControlScreen
import io.github.rhythmcache.dioxamine.adb.builtin.remotecontrol.RemoteControlTile
import io.github.rhythmcache.dioxamine.adb.builtin.screencap.ScreencapScreen
import io.github.rhythmcache.dioxamine.adb.builtin.screencap.ScreencapTile
import io.github.rhythmcache.dioxamine.adb.builtin.touchpad.TouchpadScreen
import io.github.rhythmcache.dioxamine.adb.builtin.touchpad.TouchpadTile

sealed class BuiltInSubScreen {
    object TilesList : BuiltInSubScreen()
    object DeviceInfo : BuiltInSubScreen()
    object RemoteControl : BuiltInSubScreen()
    object Touchpad : BuiltInSubScreen()
    object FileManager : BuiltInSubScreen()
    object PackageManager : BuiltInSubScreen()
    object ProcessManager : BuiltInSubScreen()
    object Misc : BuiltInSubScreen()
    object Screenshot : BuiltInSubScreen()
    object Reboot : BuiltInSubScreen()
}

@Composable
fun BuiltInActionsTab(vm: AdbViewModel) {
    var activeSubScreen by remember { mutableStateOf<BuiltInSubScreen>(BuiltInSubScreen.TilesList) }
    val isConnected = vm.activeClient() != null

    BackHandler(enabled = activeSubScreen != BuiltInSubScreen.TilesList) {
        activeSubScreen = BuiltInSubScreen.TilesList
    }

    when (activeSubScreen) {
        BuiltInSubScreen.TilesList -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { PremiumDeviceDashboardHeader(isConnected) }

                if (!isConnected) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.DevicesOther, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.connect_device_warning),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                item { ToolsSectionLabel("DEVICE") }
                item {
                    DeviceInformationTile(
                        vm = vm,
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.DeviceInfo }
                    )
                }

                item { ToolsSectionLabel("CONTROL") }
                item {
                    RemoteControlTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.RemoteControl }
                    )
                }
                item {
                    TouchpadTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.Touchpad }
                    )
                }
                item {
                    ScreencapTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.Screenshot }
                    )
                }

                item { ToolsSectionLabel("MANAGEMENT") }
                item {
                    FileManagerTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.FileManager }
                    )
                }
                item {
                    PackageManagerTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.PackageManager }
                    )
                }
                item {
                    ProcessManagerTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.ProcessManager }
                    )
                }

                item { ToolsSectionLabel("SYSTEM") }
                item {
                    MiscTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.Misc }
                    )
                }
                item {
                    RebootTile(
                        isConnected = isConnected,
                        onClick = { activeSubScreen = BuiltInSubScreen.Reboot }
                    )
                }
            }
        }
        BuiltInSubScreen.DeviceInfo -> DeviceInformationDetailScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
        BuiltInSubScreen.RemoteControl -> RemoteControlScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList }, onOpenTouchpad = { activeSubScreen = BuiltInSubScreen.Touchpad })
        BuiltInSubScreen.Touchpad -> TouchpadScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
        BuiltInSubScreen.FileManager -> FileManagerScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
        BuiltInSubScreen.PackageManager -> PackageManagerScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
        BuiltInSubScreen.ProcessManager -> ProcessManagerScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
        BuiltInSubScreen.Misc -> MiscScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
        BuiltInSubScreen.Screenshot -> ScreencapScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
        BuiltInSubScreen.Reboot -> RebootScreen(vm = vm, onBack = { activeSubScreen = BuiltInSubScreen.TilesList })
    }
}

@Composable
private fun PremiumDeviceDashboardHeader(isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "ADB Command Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Premium Android device-control dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isConnected) Icons.Filled.CheckCircle else Icons.Filled.DevicesOther,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (isConnected) "ONLINE" else "READY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.DevicesOther,
                    label = "ADB",
                    value = if (isConnected) "ACTIVE" else "WAIT"
                )
                DashboardMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Terminal,
                    label = "TOOLS",
                    value = "10"
                )
                DashboardMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Security,
                    label = "ACCESS",
                    value = "READY"
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isConnected) Icons.Filled.Bolt else Icons.Filled.DevicesOther,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isConnected) "Device connected • tools are ready to use" else "Connect an Android device via USB or wireless ADB",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ToolsSectionLabel(label: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    }
}
