package com.example.flipperdroid.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Donnees representant un reseau Wifi
 *
 * @param ssid Nom du reseau Wifi
 * @param bssid Adresse MAC du point d acces
 * @param rssi Niveau du signal en dBm
 * @param frequency Frequence radio en MHz
 * @param capabilities Capacites et securite du reseau
 * @param lastSeen Heure de la detection formatee en chaine
 * @param channel Canal radio calcule a partir de la frequence
 */
data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String,
    val lastSeen: String,
    val channel: Int = frequencyToChannel(frequency)
) {
    companion object {
        fun fromScanResult(scanResult: ScanResult): WifiNetwork {
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return WifiNetwork(
                ssid = if (scanResult.SSID.isNullOrEmpty()) "<Hidden Network>" else scanResult.SSID,
                bssid = scanResult.BSSID,
                rssi = scanResult.level,
                frequency = scanResult.frequency,
                capabilities = scanResult.capabilities,
                lastSeen = dateFormat.format(Date())
            )
        }

        fun frequencyToChannel(frequency: Int): Int {
            return when {
                frequency >= 2412 && frequency <= 2484 -> (frequency - 2412) / 5 + 1
                frequency >= 5170 && frequency <= 5825 -> (frequency - 5170) / 5 + 34
                else -> 0
            }
        }
    }
}

/**
 * ViewModel gerant la detection des reseaux Wifi et la lecture des resultats de scan
 *
 * Gere la detection, la liste des reseaux disponibles, l etat du scan et des permissions
 */
class WifiDeautherViewModel : ViewModel() {
    private var wifiManager: WifiManager? = null

    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null

    private var scanReceiver: BroadcastReceiver? = null

    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val networks: StateFlow<List<WifiNetwork>> = _networks

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE
    ).apply {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    /**
     * Initialise le ViewModel avec le contexte et configure le WifiManager et le BroadcastReceiver
     *
     * @param context Contexte Android
     */

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun initialize(context: Context) {
        this.context = context
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        setupScanReceiver()
        checkPermissions()
    }
    /**
     * Configure et enregistre le BroadcastReceiver pour intercepter les resultats de scan Wifi
     */
    private fun setupScanReceiver() {
        try {
            scanReceiver?.let { receiver ->
                context?.unregisterReceiver(receiver)
            }
        } catch (e: Exception) {
            Log.e("WifiDeauther", "Error unregistering existing receiver", e)
        }

        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                        val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                        Log.d("WifiDeauther", "Scan results received. Success: $success")
                        if (success) {
                            scanSuccess()
                        } else {
                            scanFailure()
                        }
                    }
                }
            }
        }

        context?.registerReceiver(
            scanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
    }
    /**
     * Traite les resultats du scan lorsqu ils sont disponibles
     */
    private fun scanSuccess() {
        viewModelScope.launch {
            try {
                wifiManager?.let { wifi ->
                    val results = wifi.scanResults
                    Log.d("WifiDeauther", "Scan success, found ${results.size} networks")
                    results.forEach { result ->
                        Log.d("WifiDeauther", "Network: SSID=${result.SSID}, BSSID=${result.BSSID}, RSSI=${result.level}, Capabilities=${result.capabilities}")
                    }
                    
                    if (results.isEmpty()) {
                        Log.w("WifiDeauther", "No networks found in scan results")
                        // Try to get cached results
                        scanFailure()
                        return@launch
                    }

                    val networks = results
                        .filter { !it.SSID.isNullOrEmpty() && !it.BSSID.isNullOrEmpty() }
                        .map { WifiNetwork.fromScanResult(it) }
                        .distinctBy { it.bssid }
                        .sortedByDescending { it.rssi }
                    
                    Log.d("WifiDeauther", "Emitting ${networks.size} networks")
                    _networks.emit(networks)
                } ?: run {
                    Log.e("WifiDeauther", "WifiManager is null")
                }
            } catch (e: SecurityException) {
                Log.e("WifiDeauther", "Security exception during scan success", e)
                _permissionsGranted.emit(false)
            } finally {
                _isScanning.emit(false)
            }
        }
    }

    /**
     * Traite les resultats caches ou precedents si le scan echoue
     */
    private fun scanFailure() {
        viewModelScope.launch {
            try {
                wifiManager?.let { wifi ->
                    val results = wifi.scanResults
                    Log.d("WifiDeauther", "Using cached results, found ${results.size} networks")
                    
                    val networks = results
                        .filter { !it.SSID.isNullOrEmpty() && !it.BSSID.isNullOrEmpty() }
                        .map { WifiNetwork.fromScanResult(it) }
                        .distinctBy { it.bssid }
                        .sortedByDescending { it.rssi }
                    
                    Log.d("WifiDeauther", "Emitting ${networks.size} cached networks")
                    _networks.emit(networks)
                } ?: run {
                    Log.e("WifiDeauther", "WifiManager is null")
                }
            } catch (e: SecurityException) {
                Log.e("WifiDeauther", "Security exception during scan failure", e)
                _permissionsGranted.emit(false)
            } finally {
                _isScanning.emit(false)
            }
        }
    }
    /**
     * Verifie si toutes les permissions necessaires sont accordées
     * (ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE, etc.)
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermissions() {
        context?.let { ctx ->
            val missingPermissions = requiredPermissions.filter { permission ->
                ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED
            }

            val allGranted = missingPermissions.isEmpty()
            
            Log.d("WifiDeauther", "Permissions check: all granted = $allGranted")
            if (!allGranted) {
                Log.w("WifiDeauther", "Missing permissions: ${missingPermissions.joinToString()}")
            }

            viewModelScope.launch {
                _permissionsGranted.emit(allGranted)
            }
        }
    }

    /**
     * Lance un scan Wifi si les permissions sont accordees et que le Wifi est active
     */
    fun startScan() {
        if (!permissionsGranted.value) {
            Log.d("WifiDeauther", "Cannot start scan: permissions not granted")
            return
        }

        viewModelScope.launch {
            try {
                _isScanning.emit(true)
                wifiManager?.let { wifi ->
                    if (!wifi.isWifiEnabled) {
                        Log.d("WifiDeauther", "WiFi is disabled, enabling...")
                        wifi.isWifiEnabled = true
                        
                        // Attendre que le WiFi soit activé
                        var attempts = 0
                        while (!wifi.isWifiEnabled && attempts < 10) {
                            Log.d("WifiDeauther", "Waiting for WiFi to enable... attempt $attempts")
                            kotlinx.coroutines.delay(500)
                            attempts++
                        }

                        if (!wifi.isWifiEnabled) {
                            Log.d("WifiDeauther", "Failed to enable WiFi")
                            _networks.emit(emptyList())
                            _isScanning.emit(false)
                            return@launch
                        }
                    }

                    Log.d("WifiDeauther", "Starting scan...")
                    val scanStarted = wifi.startScan()
                    if (!scanStarted) {
                        Log.w("WifiDeauther", "Failed to start scan, using cached results")
                        scanFailure()
                    } else {
                        Log.d("WifiDeauther", "Scan started successfully")
                    }
                } ?: run {
                    Log.e("WifiDeauther", "WifiManager is null")
                    _isScanning.emit(false)
                }
            } catch (e: SecurityException) {
                Log.e("WifiDeauther", "Security exception during scan start", e)
                _permissionsGranted.emit(false)
                _isScanning.emit(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            scanReceiver?.let { receiver ->
                context?.unregisterReceiver(receiver)
            }
        } catch (e: Exception) {
            Log.e("WifiDeauther", "Error unregistering receiver", e)
        }
    }

    /**
     * Retourne une evaluation qualitative de la puissance du signal Wifi
     *
     * @param rssi Niveau du signal en dBm
     * @return Enum representant la qualite du signal
     */
    fun getNetworkStrength(rssi: Int): NetworkStrength {
        return when {
            rssi >= -50 -> NetworkStrength.EXCELLENT
            rssi >= -60 -> NetworkStrength.GOOD
            rssi >= -70 -> NetworkStrength.FAIR
            else -> NetworkStrength.POOR
        }
    }
    /**
     * Analyse la chaine des capacites pour determiner le type de securite du reseau Wifi
     *
     * @param capabilities Chaine des capacites du reseau
     * @return Enum representant le type de securite
     */
    fun getSecurityType(capabilities: String): SecurityType {
        return when {
            capabilities.contains("WEP") -> SecurityType.WEP
            capabilities.contains("WPA3") -> SecurityType.WPA3
            capabilities.contains("WPA2") -> SecurityType.WPA2
            capabilities.contains("WPA") -> SecurityType.WPA
            else -> SecurityType.OPEN
        }
    }
}

enum class NetworkStrength {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

enum class SecurityType {
    OPEN,
    WEP,
    WPA,
    WPA2,
    WPA3
} 