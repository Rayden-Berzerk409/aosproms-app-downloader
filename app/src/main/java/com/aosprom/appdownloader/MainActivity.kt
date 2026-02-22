package com.aosprom.appdownloader

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aosprom.appdownloader.ui.theme.AppDownloaderTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch

enum class Tab {
    PRESET_APPS,
    STANDALONE_APK
}



class MainActivity : AppCompatActivity() {

    /** Índice de la siguiente app a abrir en Play Store cuando el usuario vuelve. */
    private var nextPlayStoreIndex: Int = -1

    /** Se incrementa al volver de Play Store para refrescar el estado "Instalada" en la lista. */
    private var listRefreshKey = mutableIntStateOf(0)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_language_check", true)
        
        if (isFirstLaunch && AppCompatDelegate.getApplicationLocales().isEmpty) {
            val sysLocales = android.content.res.Resources.getSystem().configuration.locales
            if (!sysLocales.isEmpty) {
                val sysLocale = sysLocales.get(0)
                val sysLanguage = sysLocale.language
                if (sysLanguage != "es" && sysLanguage != "en") {
                    prefs.edit()
                        .putBoolean("show_unsupported_lang_dialog", true)
                        .putString("detected_lang", sysLocale.displayLanguage)
                        .putBoolean("first_language_check", false)
                        .apply()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"))
                    return // Activity will recreate
                } else {
                    prefs.edit().putBoolean("first_language_check", false).apply()
                }
            }
        }
        
        enableEdgeToEdge()
        setContent {
            var showUnsupportedDialog by remember { mutableStateOf(prefs.getBoolean("show_unsupported_lang_dialog", false)) }
            val detectedLang = prefs.getString("detected_lang", "") ?: ""
            
            if (showUnsupportedDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        showUnsupportedDialog = false
                        prefs.edit().putBoolean("show_unsupported_lang_dialog", false).apply()
                    },
                    title = { Text(stringResource(R.string.lang_unsupported_title)) },
                    text = { Text(stringResource(R.string.lang_unsupported_desc, detectedLang)) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            showUnsupportedDialog = false
                            prefs.edit().putBoolean("show_unsupported_lang_dialog", false).apply()
                        }) {
                            Text(stringResource(R.string.ok_button))
                        }
                    }
                )
            }

            val refreshKey by listRefreshKey
            var selectedTab by remember { mutableStateOf(Tab.PRESET_APPS) }
            
            // State for Material You Dynamic Colors
            var isDynamicColorEnabled by remember { mutableStateOf(false) }
            
            AppDownloaderTheme(dynamicColor = isDynamicColorEnabled) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = {
                                var expandedLanguage by remember { mutableStateOf(false) }
                                
                                Box {
                                    IconButton(onClick = { expandedLanguage = true }) {
                                        Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language))
                                    }
                                    DropdownMenu(
                                        expanded = expandedLanguage,
                                        onDismissRequest = { expandedLanguage = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Español") },
                                            onClick = {
                                                expandedLanguage = false
                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("English") },
                                            onClick = {
                                                expandedLanguage = false
                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en-US"))
                                            }
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.material_you),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Switch(
                                        checked = isDynamicColorEnabled,
                                        onCheckedChange = { isDynamicColorEnabled = it }
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == Tab.PRESET_APPS,
                                onClick = { selectedTab = Tab.PRESET_APPS },
                                icon = { Icon(Icons.Default.Android, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_preset_apps)) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == Tab.STANDALONE_APK,
                                onClick = { selectedTab = Tab.STANDALONE_APK },
                                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_standalone_apk)) }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        Tab.PRESET_APPS -> PresetAppsScreen(
                            modifier = Modifier.padding(innerPadding),
                            packageManager = packageManager,
                            refreshKey = refreshKey,
                            onGetAppsClick = { startPlayStoreSequence() }
                        )
                        Tab.STANDALONE_APK -> StandaloneApkScreen(
                            modifier = Modifier.padding(innerPadding),
                            packageManager = packageManager,
                            refreshKey = refreshKey
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        listRefreshKey.value = listRefreshKey.value + 1
        // Cuando el usuario vuelve de Play Store, abrir la siguiente app de la lista.
        if (nextPlayStoreIndex in 0 until PredeterminedApps.list.size) {
            openPlayStoreFor(PredeterminedApps.list[nextPlayStoreIndex].packageName)
            nextPlayStoreIndex++
            if (nextPlayStoreIndex >= PredeterminedApps.list.size) {
                nextPlayStoreIndex = -1
            }
        }
        // Nota: El estado de instalación de APK se resetea automáticamente cuando
        // el usuario vuelve a la app (el composable se reconstruye con estado inicial)
    }

    private fun startPlayStoreSequence() {
        nextPlayStoreIndex = 0
        if (PredeterminedApps.list.isNotEmpty()) {
            openPlayStoreFor(PredeterminedApps.list[0].packageName)
            nextPlayStoreIndex = 1
        }
    }

    private fun openPlayStoreFor(packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Si no hay Play Store, abrir en el navegador.
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}

@Composable
fun PresetAppsScreen(
    modifier: Modifier = Modifier,
    packageManager: PackageManager,
    refreshKey: Int = 0,
    onGetAppsClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.get_apps_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGetAppsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(stringResource(R.string.get_apps_button))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.get_apps_disclaimer),
            style = MaterialTheme.typography.bodySmall,
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

        val launcherInfo = remember(refreshKey) {
            getLauncherInfo(packageManager)
        }
        PredeterminedApps.list.forEach { app ->
            val isInstalled = remember(app.packageName, refreshKey, launcherInfo) {
                isAppInstalled(packageManager, app.packageName, launcherInfo.packageNames)
            }
            val appIcon = remember(app.packageName, refreshKey, launcherInfo) {
                getAppIcon(packageManager, app.packageName)
                    ?: launcherInfo.iconsByPackage[app.packageName]
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
                            // Placeholder si no hay icono
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
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (isInstalled)
                            stringResource(R.string.installed)
                        else
                            stringResource(R.string.not_installed),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isInstalled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
