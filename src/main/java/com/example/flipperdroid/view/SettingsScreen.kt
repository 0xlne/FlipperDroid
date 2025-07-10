package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
/**
 * Affiche l ecran des parametres avec plusieurs sections configurables
 *
 * Cette interface permet de gerer les preferences liees a l apparence aux notifications au retour haptique et sonore ainsi qu au stockage
 * Les options incluent le theme sombre la persistance de l ecran les notifications les vibrations les sons la suppression du cache et l export des donnees
 *
 * @param navController controleur de navigation permettant de revenir a l ecran precedent
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var darkMode by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Dark Mode") },
                            supportingContent = { Text("Enable dark theme") },
                            leadingContent = {
                                Icon(Icons.Default.DarkMode, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = darkMode,
                                    onCheckedChange = { darkMode = it }
                                )
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Keep Screen On") },
                            supportingContent = { Text("Prevent screen from turning off") },
                            leadingContent = {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = keepScreenOn,
                                    onCheckedChange = { keepScreenOn = it }
                                )
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Push Notifications") },
                        supportingContent = { Text("Enable notifications") },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = notifications,
                                onCheckedChange = { notifications = it }
                            )
                        }
                    )
                }
            }

            item {
                Text(
                    "Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Vibration") },
                            supportingContent = { Text("Enable haptic feedback") },
                            leadingContent = {
                                Icon(Icons.Default.Vibration, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = vibrationEnabled,
                                    onCheckedChange = { vibrationEnabled = it }
                                )
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Sound") },
                            supportingContent = { Text("Enable sound effects") },
                            leadingContent = {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = { soundEnabled = it }
                                )
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    "Storage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Clear Cache") },
                            supportingContent = { Text("Delete temporary files") },
                            leadingContent = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            },
                            trailingContent = {
                                IconButton(onClick = { /* TODO cache clearing */ }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                                }
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Export Data") },
                            supportingContent = { Text("Backup your data") },
                            leadingContent = {
                                Icon(Icons.Default.Save, contentDescription = null)
                            },
                            trailingContent = {
                                IconButton(onClick = { /* TODO data export */ }) {
                                    Icon(Icons.Default.Download, contentDescription = "Export")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
