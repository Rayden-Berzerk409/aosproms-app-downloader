package com.aosprom.appdownloader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneApkScreen(
    modifier: Modifier = Modifier,
    packageManager: android.content.pm.PackageManager,
    viewModel: DownloadViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    refreshKey: Int = 0
) {
    val context = LocalContext.current
    val appStates = viewModel.appStates
    val scope = rememberCoroutineScope()
    
    // Local refresh state for Pull-to-Refresh
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    
    // Combined key for launcher info refresh
    // We increment this when pull-to-refresh happens to force re-reading installed apps
    var internalRefreshKey by remember { mutableIntStateOf(0) }

    // Initial check for updates
    androidx.compose.runtime.LaunchedEffect(Unit) {
         val info = getLauncherInfo(packageManager)
         viewModel.checkUpdates(packageManager, info)
    }
    
    val currentRefreshKey = refreshKey + internalRefreshKey

    val launcherInfo = remember(currentRefreshKey) {
        getLauncherInfo(packageManager)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                // Re-fetch info and check updates
                internalRefreshKey++
                val info = getLauncherInfo(packageManager)
                viewModel.checkUpdates(packageManager, info)
                isRefreshing = false
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.tab_standalone_apk),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Apps instalables por APK (GitHub u otras URLs)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_list_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ApkApps.list.forEach { app ->
                val effectivePackage = remember(app, currentRefreshKey, launcherInfo) {
                    if (app.identifyByDisplayName) {
                        findPackageByDisplayName(launcherInfo, app.displayName)
                    } else {
                        app.packageName
                    }
                }
                val isInstalled = remember(effectivePackage, launcherInfo) {
                    if (app.identifyByDisplayName) {
                        effectivePackage != null
                    } else {
                        isAppInstalled(packageManager, app.packageName, launcherInfo.packageNames)
                    }
                }
                
                val appState = appStates[app.packageName] ?: AppState()
                
                // Installation Wizard State
                var showInstallWizard by remember { mutableStateOf(false) }
                
                // Permission Check
                val canInstallPackages = remember(currentRefreshKey, launcherInfo) { // Update check on refresh
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.packageManager.canRequestPackageInstalls()
                    } else {
                        true
                    }
                }
                val isLawnchair = app.packageName == "app.lawnchair"

                // Trigger logic when file is ready
                androidx.compose.runtime.LaunchedEffect(appState.downloadedFile) {
                    if (appState.downloadedFile != null && !showInstallWizard) {
                        if (isLawnchair) {
                            showInstallWizard = true
                        } else if (!canInstallPackages) {
                             showInstallWizard = true
                        } else {
                            // Direct install
                            val intent = viewModel.getInstallIntent(context, appState.downloadedFile)
                            if (intent != null) {
                                try {
                                    context.startActivity(intent)
                                    viewModel.resetState(app.packageName)
                                } catch (e: Exception) {
                                    // Fallback to manually showing wizard if direct launch fails (rare)
                                    showInstallWizard = true
                                }
                            }
                        }
                    }
                }

                if (showInstallWizard && appState.downloadedFile != null) {
                    InstallationWizardDialog(
                        onDismiss = { 
                            showInstallWizard = false
                            viewModel.resetState(app.packageName)
                        },
                        onInstallRequest = {
                            val intent = viewModel.getInstallIntent(context, appState.downloadedFile)
                            if (intent != null) {
                                try {
                                    context.startActivity(intent)
                                    showInstallWizard = false
                                    viewModel.resetState(app.packageName)
                                } catch (e: Exception) {
                                    // Show error
                                }
                            }
                        },
                        showPlayProtect = isLawnchair // Only show Play Protect step for Lawnchair
                    )
                }

                val appIcon = remember(effectivePackage, currentRefreshKey, launcherInfo) {
                    if (effectivePackage != null) {
                        getAppIcon(packageManager, effectivePackage)
                            ?: launcherInfo.iconsByPackage[effectivePackage]
                    } else {
                        null
                    }
                }
                
                val packageLabel = if (app.identifyByDisplayName) {
                    if (effectivePackage != null) effectivePackage else "—"
                } else {
                    app.packageName
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInstalled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (appIcon != null) {
                                val bitmap = remember(appIcon) {
                                    drawableToBitmap(appIcon).asImageBitmap()
                                }
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = app.displayName,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = packageLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val statusText = when {
                                    appState.isDownloading -> "Descargando ${(appState.progress * 100).toInt()}%"
                                    appState.isUpdateAvailable -> "Actualización disponible: ${appState.latestVersion}"
                                    isInstalled -> stringResource(R.string.installed)
                                    else -> stringResource(R.string.not_installed)
                                }
                                
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isInstalled && !appState.isUpdateAvailable)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else if (appState.isUpdateAvailable)
                                         MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Box(contentAlignment = Alignment.Center) {
                            if (appState.isDownloading) {
                                CircularProgressIndicator(
                                    progress = { appState.progress },
                                    modifier = Modifier.size(40.dp),
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        viewModel.startDownload(context, app)
                                    },
                                    enabled = !appState.isDownloading
                                ) {
                                    if (appState.isUpdateAvailable) {
                                         Icon(
                                            androidx.compose.material.icons.Icons.Default.SystemUpdate,
                                            contentDescription = "Actualizar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isInstalled) {
                                        // If installed and no update, show check or nothing.
                                        // User might want to re-install? Let's keep download option but maybe different icon
                                        // Or just checkmark.
                                        Icon(
                                            androidx.compose.material.icons.Icons.Default.Check,
                                            contentDescription = "Instalado"
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = stringResource(R.string.download_and_install)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstallationWizardDialog(
    onDismiss: () -> Unit,
    onInstallRequest: () -> Unit,
    showPlayProtect: Boolean
) {
    var currentStep by remember { mutableStateOf(1) }
    val context = LocalContext.current
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pasos de Instalación") },
        text = {
            Column {
                if (showPlayProtect) {
                     WizardStep(
                        stepNumber = 1,
                        text = "Abrir Play Protect",
                        description = "Verifica que Play Protect no bloquee la instalación.",
                        isActive = currentStep == 1,
                        isCompleted = currentStep > 1,
                        action = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("market://details?id=com.google.android.gms") 
                                intent.setClassName("com.android.vending", "com.google.android.finsky.protect.PlayProtectHomeActivity")
                                 try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
                                    context.startActivity(fallback)
                                }
                            } catch (e: Exception) {
                            }
                            currentStep = 2
                        }
                    )
                     WizardStep(
                        stepNumber = 2,
                        text = "Permitir Fuentes Desconocidas",
                        description = "Ve a Configuración y permite instalar apps desconocidas para AppDownloader.",
                        isActive = currentStep == 2,
                        isCompleted = currentStep > 2,
                         action = {
                            try {
                                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                 } else {
                                      val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                      context.startActivity(intent)
                                 }
                            } catch (e: Exception) {
                            }
                            currentStep = 3
                        }
                    )
                     WizardStep(
                        stepNumber = 3,
                        text = "Instalar APK",
                        description = "Iniciar la instalación del paquete.",
                        isActive = currentStep == 3,
                        isCompleted = false, 
                        action = onInstallRequest,
                        actionLabel = "Instalar"
                    )
                } else {
                     // 2-Step Flow (Skipping Play Protect)
                     WizardStep(
                        stepNumber = 1,
                        text = "Permitir Fuentes Desconocidas",
                        description = "Ve a Configuración y permite instalar apps desconocidas para AppDownloader.",
                        isActive = currentStep == 1,
                        isCompleted = currentStep > 1,
                         action = {
                            try {
                                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                 } else {
                                      val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                      context.startActivity(intent)
                                 }
                            } catch (e: Exception) {
                            }
                            currentStep = 2
                        }
                    )
                     WizardStep(
                        stepNumber = 2,
                        text = "Instalar APK",
                        description = "Iniciar la instalación del paquete.",
                        isActive = currentStep == 2,
                        isCompleted = false, 
                        action = onInstallRequest,
                        actionLabel = "Instalar"
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun WizardStep(
    stepNumber: Int, 
    text: String, 
    description: String, 
    isActive: Boolean, 
    isCompleted: Boolean,
    action: () -> Unit,
    actionLabel: String = "Abrir"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         Box(
             modifier = Modifier
                 .size(24.dp)
                 .background(
                     if (isCompleted) MaterialTheme.colorScheme.primary 
                     else if (isActive) MaterialTheme.colorScheme.secondary 
                     else MaterialTheme.colorScheme.surfaceVariant,
                     androidx.compose.foundation.shape.CircleShape
                 ),
             contentAlignment = Alignment.Center
         ) {
             if (isCompleted) {
                 Icon(
                     androidx.compose.material.icons.Icons.Default.Check, 
                     contentDescription = null, 
                     tint = MaterialTheme.colorScheme.onPrimary,
                     modifier = Modifier.size(16.dp)
                 )
             } else {
                 Text(
                     text = stepNumber.toString(),
                     color = if (isActive) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                     style = MaterialTheme.typography.bodySmall
                 )
             }
         }
         Spacer(modifier = Modifier.size(12.dp))
         Column(modifier = Modifier.weight(1f)) {
             Text(
                 text = text,
                 style = MaterialTheme.typography.titleSmall,
                 color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
             )
             if (isActive) {
                 Text(
                     text = description,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
             }
         }
         if (isActive) {
             androidx.compose.material3.Button(
                 onClick = action,
                 modifier = Modifier.height(36.dp),
                 contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
             ) {
                 Text(actionLabel, style = MaterialTheme.typography.labelSmall)
             }
         }
    }
}



