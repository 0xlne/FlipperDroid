package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Ecran de controle infrarouge permettant de selectionner un type de telecommande
 * et d afficher les boutons correspondants a la fonction choisie
 *
 * @param navController Controleur de navigation pour gerer les ecrans
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfraredScreen(navController: NavController) {
    var selectedDevice by remember { mutableStateOf<String?>(null) }

    /**
     * Liste des types de telecommandes disponibles avec leur icone associee
     */
    val remoteTypes = listOf(
        "TV" to Icons.Default.Tv,
        "Air Conditioner" to Icons.Default.AcUnit,
        "Audio System" to Icons.AutoMirrored.Filled.VolumeUp,
        "Projector" to Icons.Default.Cast,
        "Custom Remote" to Icons.Default.Add
    )

    Scaffold(
        topBar = {
            /**
             * Barre superieure avec bouton retour et titre
             */
            TopAppBar(
                title = { Text("IR Remote") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        /**
         * Liste verticale affichant les types de telecommandes
         */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(remoteTypes) { (type, icon) ->
                ElevatedCard(
                    onClick = { selectedDevice = type },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(type) },
                        supportingContent = {
                            Text(
                                when (type) {
                                    "TV" -> "Control your TV"
                                    "Air Conditioner" -> "Control your AC"
                                    "Audio System" -> "Control your audio system"
                                    "Projector" -> "Control your projector"
                                    else -> "Add custom remote"
                                }
                            )
                        },
                        leadingContent = {
                            Icon(icon, contentDescription = null)
                        },
                        trailingContent = {
                            if (selectedDevice == type) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                /**
                 * Affichage des boutons de controle si un type est selectionne
                 * Sauf pour le type Custom Remote
                 */
                if (selectedDevice != null && selectedDevice != "Custom Remote") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = { /* Implement power action */ },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.Default.Power,
                                    contentDescription = "Power",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = { /* Implement volume up */ },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Volume Up",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { /* Implement mute */ },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Mute",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { /* Implement volume down */ },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeDown,
                                    contentDescription = "Volume Down",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = { /* Implement channel up */ },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Channel Up",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { /* Implement input source */ },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Input,
                                    contentDescription = "Input Source",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { /* Implement channel down */ },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Channel Down",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
