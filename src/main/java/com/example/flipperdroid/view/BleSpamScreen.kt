package com.example.flipperdroid.view

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.BleSpamViewModel
import androidx.compose.ui.graphics.Color

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleSpamScreen(
    navController: NavController? = null,
    viewModel: BleSpamViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val advertisementSets by viewModel.advertisementSets.collectAsState()
    val isActive by viewModel.isActive.collectAsState()
    val brand by viewModel.brand.collectAsState()
    var permissionsGranted by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val checkedStates = remember(advertisementSets) {
        mutableStateListOf<Boolean>().apply { clear(); addAll(List(advertisementSets.size) { true }) }
    }
    val spamLogs by viewModel.spamLogs.collectAsState()

    // Permissions à demander
    val permissions = mutableListOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ).apply {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
        if (!permissionsGranted) showSnackbar = "Permissions Bluetooth requises pour spammer."
    }

    // Vérifie les permissions au lancement
    LaunchedEffect(Unit) {
        val granted = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        permissionsGranted = granted
        if (!granted) permissionLauncher.launch(permissions)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val backButton: @Composable (() -> Unit) = {
        IconButton(onClick = { navController?.navigateUp() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
        }
    }

    Scaffold(
        topBar = {
            if (navController != null) {
                TopAppBar(
                    title = { Text("BLE Spam") },
                    navigationIcon = backButton
                )
            } else {
                TopAppBar(
                    title = { Text("BLE Spam") }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Switch Apple/Samsung
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                val options = listOf(
                    BleSpamViewModel.BleSpamBrand.APPLE to "Apple",
                    BleSpamViewModel.BleSpamBrand.SAMSUNG to "Samsung",
                    BleSpamViewModel.BleSpamBrand.ALL to "All"
                )
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .selectable(
                                selected = brand == value,
                                onClick = { viewModel.setBrand(value) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = brand == value,
                            onClick = { viewModel.setBrand(value) }
                        )
                        Text(label)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!permissionsGranted) {
                Text(
                    "Permissions Bluetooth requises pour utiliser le spam BLE.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = {
                    permissionLauncher.launch(permissions)
                    if (!permissionsGranted) showSnackbar = "Permissions refusées. Impossible d'utiliser le spam BLE."
                }) {
                    Text("Demander les permissions")
                }
                return@Column
            }
            Text("Sélectionnez les payloads à spammer :", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(advertisementSets.size) { idx ->
                    val set = advertisementSets[idx]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .toggleable(
                                value = checkedStates.getOrNull(idx) == true,
                                enabled = !isActive,
                                onValueChange = { checked -> if (idx < checkedStates.size) checkedStates[idx] = checked }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkedStates.getOrNull(idx) == true,
                            onCheckedChange = null,
                            enabled = !isActive
                        )
                        Text(set.title, Modifier.padding(start = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        if (checkedStates.none { it }) {
                            showSnackbar = "Sélectionnez au moins un payload."
                            return@Button
                        } else {
                            viewModel.setCheckedPayloads(checkedStates.toList())
                            viewModel.startSpam()
                            showSnackbar = "Spam BLE démarré."
                        }
                    },
                    enabled = !isActive && permissionsGranted
                ) { Text("Démarrer le spam") }
                Button(
                    onClick = {
                        viewModel.stopSpam()
                        // Réinitialise la sélection après arrêt
                        checkedStates.clear(); checkedStates.addAll(List(advertisementSets.size) { true })
                        showSnackbar = "Spam BLE arrêté."
                    },
                    enabled = isActive && permissionsGranted
                ) { Text("Arrêter") }
            }
            if (showSnackbar != null) {
                LaunchedEffect(showSnackbar) {
                    val msg = showSnackbar
                    showSnackbar = null
                    msg?.let { snackbarHostState.showSnackbar(it) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Spams envoyés : ${spamLogs.size}", style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, Color.Gray).padding(4.dp)) {
                val scrollState = rememberScrollState()
                LaunchedEffect(spamLogs.size) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Column(Modifier.verticalScroll(scrollState)) {
                    spamLogs.forEach { log ->
                        Text(log, fontSize = 12.sp)
                    }
                }
            }
        }
    }
} 