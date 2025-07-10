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
import android.util.Log
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
     * Initialise le ViewModel avec le contexte de l application
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * Copie le binaire nmap depuis les assets vers le dossier interne si besoin, et le rend exécutable
     * @return Chemin absolu du binaire nmap prêt à l emploi
     */
    fun copyAssetToFile(context: Context, assetName: String, destFile: File): Boolean {
        return try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("NMAP_COPY", "Erreur lors de la copie de l asset $assetName : ${e.message}")
            false
        }
    }

    /**
     * Copie récursivement tout le contenu du dossier assets/nmap/ vers /data/local/tmp/nmap/
     * @return Chemin absolu du dossier nmap prêt à l emploi
     */
    fun copyNmapToTmp(context: Context): String {
        val nmapDir = File("/data/local/tmp")
        val internalNmapDir = File(context.filesDir, "nmap")
        
        try {
            // Suppression du dossier
            if (internalNmapDir.exists()) {
                Log.d("NMAP_COPY", "Suppression de l ancien dossier nmap dans ${internalNmapDir.absolutePath}")
                deleteRecursively(internalNmapDir)
            }
            
            // Créer le dossier interne
            internalNmapDir.mkdirs()
            
            Log.d("NMAP_COPY", "Copie depuis assets/nmap/ vers ${internalNmapDir.absolutePath}")
            copyAssetsRecursively(context, "nmap", internalNmapDir)
            
            // copie ver /data/local/tmp avec root
            val copyCmd = "cp -r ${internalNmapDir.absolutePath}/* /data/local/tmp/"
            val chmodCmd = "chmod -R 755 /data/local/tmp"
            
            Log.d("NMAP_COPY", "Commande de copie : $copyCmd")
            val copyResult = Runtime.getRuntime().exec(arrayOf("su", "-c", copyCmd)).waitFor()
            Log.d("NMAP_COPY", "Résultat de la copie : $copyResult")
            
            Log.d("NMAP_COPY", "Commande chmod : $chmodCmd")
            val chmodResult = Runtime.getRuntime().exec(arrayOf("su", "-c", chmodCmd)).waitFor()
            Log.d("NMAP_COPY", "Résultat du chmod : $chmodResult")
            

            createMissingConfigFiles()
            
            if (!File("/data/local/tmp/nmap").exists()) throw Exception("Le binaire nmap n'a pas été copié dans /data/local/tmp/")
            
            return nmapDir.absolutePath
        } catch (e: Exception) {
            //Log.e("NMAP_COPY", "Erreur : ${e.message}")
            return "ERROR : ${e.message}"
        }
    }
    
    /**
     * Copie récursivement tout le contenu d un dossier d assets vers un dossier de destination
     */
    private fun copyAssetsRecursively(context: Context, assetPath: String, destDir: File) {
        try {
            val assets = context.assets.list(assetPath)
            //Log.d("NMAP_COPY", "Asset path: $assetPath, assets: ${assets?.joinToString(", ") ?: "null"}")
            
            if (assets == null || assets.isEmpty()) {
                //Log.d("NMAP_COPY", "Copie du fichier $assetPath vers ${destDir.absolutePath}")
                val success = copyAssetToFile(context, assetPath, destDir)
                Log.d("NMAP_COPY", "Résultat de la copie de $assetPath : $success")
            } else {
                //Log.d("NMAP_COPY", "Création du dossier ${destDir.absolutePath}")
                destDir.mkdirs()
                for (item in assets) {
                    val newAssetPath = if (assetPath.isEmpty()) item else "$assetPath/$item"
                    val newDestFile = File(destDir, item)
                    //Log.d("NMAP_COPY", "Traitement de $newAssetPath vers ${newDestFile.absolutePath}")
                    copyAssetsRecursively(context, newAssetPath, newDestFile)
                }
            }
        } catch (e: Exception) {
            //Log.e("NMAP_COPY", "Erreur lors de la copie de $assetPath : ${e.message}")
        }
    }
    
 
    
    /**
     * Crée les fichiers de configuration manquants pour éviter les warnings Termux
     */
    private fun createMissingConfigFiles() {
        try {
            val resolvContent = """
                nameserver 8.8.8.8
                nameserver 8.8.4.4
            """.trimIndent()
            
            val resolvCmd = "echo '$resolvContent' > /data/local/tmp/resolv.conf"
            Runtime.getRuntime().exec(arrayOf("su", "-c", resolvCmd)).waitFor()
            //Log.d("NMAP_COPY", "Fichier resolv.conf créé")
            
            val nsswitchContent = """
                hosts:      files dns
                networks:   files dns
                services:   files
                protocols:  files
                rpc:        files
            """.trimIndent()
            
            val nsswitchCmd = "echo '$nsswitchContent' > /data/local/tmp/nsswitch.conf"
            Runtime.getRuntime().exec(arrayOf("su", "-c", nsswitchCmd)).waitFor()
            //Log.d("NMAP_COPY", "Fichier nsswitch.conf créé")
            
        } catch (e: Exception) {
            //Log.w("NMAP_COPY", "Erreur lors de la création des fichiers de config : ${e.message}")
        }
    }
    
    /**
     * Supprime récursivement un dossier et son contenu
     */
    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
    /**
     * Lance une commande ping vers l adresse ou nom d hote fourni
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
     * Scan les ports entre startPort et endPort sur l hote fourni
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
     * Effectue une recherche DNS pour l hote fourni
     *
     * Recupere les adresses associees et met a jour les résultats
     *
     * @param host nom d hôte a rechercher
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
     * @param host adresse IP ou nom d hôte cible
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
     * Execute une commande nmap avec les options choisies sur l hôte fourni
     *
     * Recupere la sortie et met a jour les résultats
     *
     * @param host adresse IP ou nom d hôte cible
     * @param parameters les options choisies par l utilisateur (ex: -sV -T4)
     */


    fun nmap(host: String, parameters: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val ctx = context ?: throw Exception("Contexte non initialisé")
                val nmapDir = copyNmapToTmp(ctx)
                val tmpDir = "/data/local/tmp"

                // Utiliser des options par défaut si aucun paramètre n'est fourni
                val defaultParams = if (parameters.isEmpty()) "-sn" else parameters
                val fullCommand = "cd $tmpDir/ && LD_LIBRARY_PATH=$tmpDir/ RESOLV_CONF=$tmpDir/resolv.conf NSSWITCH_CONF=$tmpDir/nsswitch.conf ./nmap $defaultParams $host"
                val command = arrayOf("su", "-c", fullCommand)

                //Log.d("NMAP_EXEC", "Commande exécutée : $fullCommand")
                //Log.d("NMAP_EXEC", "Répertoire courant : ${System.getProperty("user.dir")}")
                
                // Vérifier que les fichiers existent
                val checkCmd = "ls -la $tmpDir"
                val checkProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", checkCmd))
                val checkReader = BufferedReader(InputStreamReader(checkProcess.inputStream))

                val process = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val output = StringBuilder()

                output.append("Commande exécutée : $fullCommand\n\n")

                // Lire la sortie standard
                val outputThread = Thread {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }
                }
                outputThread.start()

                // Lire la sortie d erreur
                val errorThread = Thread {
                    var line: String?
                    while (errorReader.readLine().also { line = it } != null) {
                        output.append("ERR: $line\n")
                    }
                }
                errorThread.start()

                // Attendre la fin avec timeout
                val timeout = 30000L // 30 secondes
                val startTime = System.currentTimeMillis()
                var exitCode = -1
                
                while (System.currentTimeMillis() - startTime < timeout) {
                    try {
                        exitCode = process.waitFor()
                        break
                    } catch (e: InterruptedException) {
                        process.destroy()
                        output.append("\n[Timeout après ${timeout/1000} secondes]\n")
                        break
                    }
                }
                
                outputThread.join(2000)
                errorThread.join(2000)
                
                
                if (output.toString().trim().isEmpty() || output.toString().trim() == "Commande exécutée : $fullCommand") {
                    output.append("Aucune sortie détectée. Vérifiez que nmap fonctionne correctement.\n")
                }
                
                _results.value = _results.value + NetworkResult(
                    command = fullCommand,
                    output = output.toString(),
                    isError = exitCode != 0
                )
            } catch (e: Exception) {
                val message = if (e.message?.contains("No such file") == true || e.message?.contains("not found") == true) {
                    "Erreur : le binaire nmap n'a pas été trouvé ou copié. Vérifiez sa présence dans les assets."
                } else {
                    e.message ?: "Erreur lors de l exécution de nmap"
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
