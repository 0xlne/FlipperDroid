package com.example.flipperdroid.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import android.content.Context
import java.io.File
import java.io.FileOutputStream


data class NetworkResult(
    val command: String,
    val output: String,
    val isError: Boolean = false
)
/**
 * ViewModel gerant des outils reseau
 *
 * Cette classe execute les commandes reseau et expose les résultats
 * via des StateFlow
 */
class NetworkToolsViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<NetworkResult>>(emptyList())
    val results: StateFlow<List<NetworkResult>> = _results

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    /**
     * Contexte Android pour accéder aux assets et au dossier interne
     */
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null

    /**
     * Initialise le ViewModel avec le contexte de l'application
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * Copie le binaire nmap depuis les assets vers le dossier interne si besoin, et le rend exécutable
     * @return Chemin absolu du binaire nmap prêt à l'emploi
     */
    private fun copyNmapBinary(context: Context): String {
        val nmapFile = File(context.filesDir, "nmap")
        if (!nmapFile.exists()) {
            context.assets.open("nmap").use { input ->
                FileOutputStream(nmapFile).use { output ->
                    input.copyTo(output)
                }
            }
            nmapFile.setExecutable(true)
        }
        return nmapFile.absolutePath
    }

    /**
     * Lance une commande ping vers l'adresse ou nom d'hote fourni
     *
     * Recupere la sortie de la commande et met a jour les résultats
     *
     * @param host adresse IP ou nom d hote a tester
     */
    fun ping(host: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val process = Runtime.getRuntime().exec("ping -c 4 $host")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                
                val exitCode = process.waitFor()
                _results.value = _results.value + NetworkResult(
                    command = "ping $host",
                    output = output.toString(),
                    isError = exitCode != 0
                )
            } catch (e: Exception) {
                _results.value = _results.value + NetworkResult(
                    command = "ping $host",
                    output = e.message ?: "Error executing ping",
                    isError = true
                )
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Scanne les ports entre startPort et endPort sur l'hote fourni
     *
     * Detecte les ports ouverts et met a jour les résultats
     *
     * @param host adresse IP ou nom d hote a scanner
     * @param startPort port de debut de plage
     * @param endPort port de fin de plage
     */
    fun scanPorts(host: String, startPort: Int, endPort: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            val openPorts = mutableListOf<Int>()
            
            try {
                for (port in startPort..endPort) {
                    try {
                        Socket(host, port).use {
                            openPorts.add(port)
                        }
                    } catch (e: Exception) {
                        // Port is closed or unreachable
                    }
                }
                
                val output = if (openPorts.isEmpty()) {
                    "No open ports found in range $startPort-$endPort"
                } else {
                    "Open ports: ${openPorts.joinToString(", ")}"
                }
                
                _results.value = _results.value + NetworkResult(
                    command = "Port scan $host:$startPort-$endPort",
                    output = output
                )
            } catch (e: Exception) {
                _results.value = _results.value + NetworkResult(
                    command = "Port scan $host",
                    output = e.message ?: "Error during port scan",
                    isError = true
                )
            } finally {
                _isScanning.value = false
            }
        }
    }
    /**
     * Effectue une recherche DNS pour l'hote fourni
     *
     * Recupere les adresses associees et met a jour les résultats
     *
     * @param host nom d'hôte a rechercher
     */
    fun dnsLookup(host: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val addresses = InetAddress.getAllByName(host)
                val output = StringBuilder()
                output.append("DNS lookup results for $host:\n")
                addresses.forEach { address ->
                    output.append("${address.hostName} -> ${address.hostAddress}\n")
                }
                
                _results.value = _results.value + NetworkResult(
                    command = "DNS lookup $host",
                    output = output.toString()
                )
            } catch (e: UnknownHostException) {
                _results.value = _results.value + NetworkResult(
                    command = "DNS lookup $host",
                    output = "Host not found: $host",
                    isError = true
                )
            } catch (e: Exception) {
                _results.value = _results.value + NetworkResult(
                    command = "DNS lookup $host",
                    output = e.message ?: "Error during DNS lookup",
                    isError = true
                )
            } finally {
                _isScanning.value = false
            }
        }
    }
    /**
     * Lance une commande traceroute vers l hote fourni
     *
     * Recupere la sortie et met a jour les résultats
     *
     * @param host adresse IP ou nom d'hôte cible
     */
    fun traceroute(host: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val process = Runtime.getRuntime().exec("traceroute $host")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                
                val exitCode = process.waitFor()
                _results.value = _results.value + NetworkResult(
                    command = "traceroute $host",
                    output = output.toString(),
                    isError = exitCode != 0
                )
            } catch (e: Exception) {
                _results.value = _results.value + NetworkResult(
                    command = "traceroute $host",
                    output = e.message ?: "Error executing traceroute",
                    isError = true
                )
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Execute une commande nmap avec les options choisies sur l'hôte fourni
     *
     * Recupere la sortie et met a jour les résultats
     *
     * @param host adresse IP ou nom d'hôte cible
     * @param parameters les options choisies par l'utilisateur (ex: -sV -T4)
     */
    fun nmap(host: String, parameters: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val ctx = context ?: throw Exception("Contexte non initialisé")
                val nmapPath = copyNmapBinary(ctx)
                val command = "$nmapPath $parameters $host"
                val process = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?


                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                val exitCode = process.waitFor()
                _results.value = _results.value + NetworkResult(
                    command = command,
                    output = output.toString(),
                    isError = exitCode != 0
                )
            } catch (e: Exception) {
                val message = if (e.message?.contains("No such file") == true || e.message?.contains("not found") == true) {
                    "Erreur : le binaire nmap n'a pas été trouvé ou copié. Vérifiez sa présence dans les assets."
                } else {
                    e.message ?: "Erreur lors de l'exécution de nmap"
                }
                _results.value = _results.value + NetworkResult(
                    command = "nmap $parameters $host",
                    output = message,
                    isError = true
                )
            } finally {
                _isScanning.value = false
            }
        }
    }
    /**
     * Vide la liste des résultats precedents
     */
    fun clearResults() {
        _results.value = emptyList()
    }
} 