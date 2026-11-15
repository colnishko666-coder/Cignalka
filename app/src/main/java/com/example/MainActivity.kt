package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.VehicleUiState
import com.example.ui.VehicleViewModel
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.ControlScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.IbtStartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VehicleViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, uiState.settings.shakeControlEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            viewModel.refreshBlePermissionsAndAdapter()
                            if (uiState.settings.shakeControlEnabled) {
                                viewModel.shakeDetector.start()
                            }
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            viewModel.shakeDetector.stop()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                if (uiState.settings.shakeControlEnabled &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ) {
                    viewModel.shakeDetector.start()
                } else {
                    viewModel.shakeDetector.stop()
                }
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    viewModel.shakeDetector.stop()
                }
            }

            IbtStartTheme(themeMode = uiState.settings.themeMode) {
                IbtStartAppScaffold(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IbtStartAppScaffold(
    uiState: VehicleUiState,
    viewModel: VehicleViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshBlePermissionsAndAdapter()
    }

    LaunchedEffect(Unit) {
        if (!uiState.bleState.hasBlePermission) {
            blePermissionLauncher.launch(viewModel.blePermissionsArray())
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeFeedback()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (uiState.currentTab) {
                                AppTab.CONTROL -> "IBT Start"
                                AppTab.AUTOMATION -> "Автозапуск и PKE"
                                AppTab.SETTINGS -> "Устройство и BLE"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = uiState.currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = uiState.currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.currentTab) {
                        AppTab.CONTROL -> ControlScreen(
                            uiState = uiState,
                            onCommand = { viewModel.sendCommand(it) },
                            onSelectWarmupMinutes = { viewModel.setDefaultWarmupMinutes(it) },
                            onTogglePke = { viewModel.setPkeEnabled(it) },
                            onToggleShake = { viewModel.setShakeControlEnabled(it) },
                            onOpenDeviceSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                            onRequestBlePermissions = {
                                blePermissionLauncher.launch(viewModel.blePermissionsArray())
                            }
                        )
                        AppTab.AUTOMATION -> AutomationScreen(
                            uiState = uiState,
                            onAddSchedule = { label, h, m, warmup, mask ->
                                viewModel.addSchedule(label, h, m, warmup, mask)
                            },
                            onToggleSchedule = { id, enabled -> viewModel.toggleSchedule(id, enabled) },
                            onDeleteSchedule = { id -> viewModel.deleteSchedule(id) },
                            onTogglePke = { viewModel.setPkeEnabled(it) },
                            onUpdatePkeThresholds = { u, l -> viewModel.setPkeThresholds(u, l) },
                            onSimulateRssi = { viewModel.updateSimulatedPkeRssi(it) },
                            onToggleShake = { viewModel.setShakeControlEnabled(it) },
                            onClearLogs = { viewModel.clearLogs() },
                            onNavigateBackToControl = { viewModel.selectTab(AppTab.CONTROL) }
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            blePermissions = viewModel.blePermissionsArray(),
                            onRefreshBleState = { viewModel.refreshBlePermissionsAndAdapter() },
                            onStartScan = { viewModel.startBleScan() },
                            onStopScan = { viewModel.stopBleScan() },
                            onConnectDevice = { viewModel.connectToBleDevice(it) },
                            onDisconnectBle = { viewModel.disconnectBle() },
                            onSetSimulationMode = { viewModel.setSimulationMode(it) },
                            onUpdateCarName = { viewModel.updateCarName(it) },
                            onUpdatePin = { viewModel.setPinCode(it) },
                            onSetHoldToConfirm = { viewModel.setHoldToConfirmEnabled(it) },
                            onSetHaptic = { viewModel.setHapticEnabled(it) },
                            onSetThemeMode = { viewModel.setThemeMode(it) },
                            onNavigateBackToControl = { viewModel.selectTab(AppTab.CONTROL) }
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.CONTROL -> if (selected) Icons.Rounded.DirectionsCar else Icons.Outlined.DirectionsCar
    AppTab.AUTOMATION -> if (selected) Icons.Rounded.Schedule else Icons.Outlined.Schedule
    AppTab.SETTINGS -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
}
