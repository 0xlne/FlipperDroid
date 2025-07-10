package com.example.flipperdroid.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.NfcViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/**
 * Donnee representant une fonctionnalite de l application a afficher dans l ecran d accueil
 *
 * @param title Nom de la fonctionnalite
 * @param icon Icone a afficher pour representer la fonctionnalite
 * @param route Nom de la route de navigation pour acceder a l ecran correspondant
 * @param enabled Indique si la fonctionnalite est activee ou non
 */
data class FeatureItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val enabled: Boolean = true
)

/**
 * Composable affichant l ecran principal de l application FlipperDroid
 *
 * Il affiche les fonctionnalites disponibles sous forme de grille
 * Chaque carte represente une fonctionnalite et permet de naviguer vers l ecran associe
 *
 * @param navController Controleur de navigation
 * @param nfcViewModel ViewModel NFC injecte mais non utilise ici
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    nfcViewModel: NfcViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("legal_prefs", Context.MODE_PRIVATE) }
    var legalAccepted by remember { mutableStateOf(prefs.getBoolean("legalAccepted", false)) }
    var showCGU by remember { mutableStateOf(false) }
    var showMIT by remember { mutableStateOf(false) }
    var showMentions by remember { mutableStateOf(false) }

    if (!legalAccepted) {
        AlertDialog(
            onDismissRequest = {}, // Blocking
            title = { Text("Legal information required") },
            text = {
                Column {
                    Text("You must accept the legal terms to use the application. Please read the following documents:")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { navController.navigate("legal_cgu") }, modifier = Modifier.fillMaxWidth()) { Text("View Terms of Use") }
                    Button(onClick = { navController.navigate("legal_mit") }, modifier = Modifier.fillMaxWidth()) { Text("View MIT License") }
                    Button(onClick = { navController.navigate("legal_mentions") }, modifier = Modifier.fillMaxWidth()) { Text("View Legal Notice") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit { putBoolean("legalAccepted", true) }
                    legalAccepted = true
                }) {
                    Text("Accept and continue")
                }
            },
            dismissButton = {}
        )
    }
    if (showCGU) {
        AlertDialog(
            onDismissRequest = { showCGU = false },
            title = { Text("CGU - Exemple") },
            text = { Text("Voici un exemple de Conditions Générales d'Utilisation. Remplace ce texte par tes propres CGU.") },
            confirmButton = {
                TextButton(onClick = { showCGU = false }) { Text("Fermer") }
            }
        )
    }
    if (showMIT) {
        AlertDialog(
            onDismissRequest = { showMIT = false },
            title = { Text("Licence MIT - Exemple") },
            text = { Text("MIT License\n\nCopyright (c) 2024\n\nPermission is hereby granted, free of charge, to any person obtaining a copy ... (remplace par le texte complet de la licence)") },
            confirmButton = {
                TextButton(onClick = { showMIT = false }) { Text("Fermer") }
            }
        )
    }
    if (showMentions) {
        AlertDialog(
            onDismissRequest = { showMentions = false },
            title = { Text("Mentions légales - Exemple") },
            text = { Text("Voici un exemple de mentions légales. Remplace ce texte par tes propres mentions légales.") },
            confirmButton = {
                TextButton(onClick = { showMentions = false }) { Text("Fermer") }
            }
        )
    }

    val features = listOf(
        FeatureItem("NFC", Icons.Default.Nfc, "nfc"),
        FeatureItem("BadUSB", Icons.Default.Usb, "badusb"),
        FeatureItem("Bluetooth", Icons.Default.Bluetooth, "bluetooth"),
        FeatureItem("Network Tools", Icons.Default.Router, "network"),
        FeatureItem("Wifi Deauther", Icons.Default.WifiOff, "wifi_deauther"),
        FeatureItem("Infrared", Icons.Default.SettingsRemote, "ir"),
        FeatureItem("Password Generator", Icons.Default.Key, "password_generator"),
        FeatureItem("Settings", Icons.Default.Settings, "settings"),
        FeatureItem("About", Icons.Default.Info, "about")
    )

    Scaffold(
        topBar = {
            /**
             * Barre superieure avec le nom de l application
             */
            TopAppBar(
                title = { Text("FlipperDroid") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        /**
         * Colonne contenant la grille de fonctionnalites
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(features) { feature ->
                    /**
                     * Carte individuelle representant une fonctionnalite
                     * Non clickable si la fonctionnalite est desactivee
                     */
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (feature.enabled)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        onClick = {
                            if (feature.enabled) {
                                navController.navigate(feature.route)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = feature.title,
                                modifier = Modifier.size(48.dp),
                                tint = if (feature.enabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feature.title,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = if (feature.enabled)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            /*
            Button(
                onClick = {
                    prefs.edit().clear().apply()
                    legalAccepted = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Reset CGU (debug)")
            }*/
        }
    }
}
