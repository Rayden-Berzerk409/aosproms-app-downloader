package com.aosprom.appdownloader

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.content.FileProvider
import android.content.Intent
import android.os.Build

data class AppState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadId: Long? = null,
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String? = null,
    val latestDownloadUrl: String? = null,
    val downloadedFile: File? = null
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _appStates = mutableStateMapOf<String, AppState>()
    val appStates: Map<String, AppState> = _appStates

    private val downloadManager = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var progressJob: Job? = null

    suspend fun checkUpdates(packageManager: PackageManager, launcherInfo: LauncherInfo) {
        ApkApps.list.forEach { app ->
            // Resolve effective package name (handle spoofing)
            val effectivePackageName = if (app.identifyByDisplayName) {
                findPackageByDisplayName(launcherInfo, app.displayName) ?: app.packageName
            } else {
                app.packageName
            }

            // Check if installed
            val installedVersion = getInstalledVersion(packageManager, effectivePackageName)
            val isInstalled = installedVersion != null

            // Cleanup APK if installed
            if (isInstalled) {
                cleanupApk(app.packageName)
            }

            if (app.githubRepo != null) {
                val release = GithubUpdateChecker.getLatestRelease(app.githubRepo) // Suspend function
                if (release != null) {
                    if (installedVersion != null) {
                        val cleanTag = release.tagName.removePrefix("v").removePrefix("V")
                            .substringBefore("_").trim()

                        // Remove "-spoofed" and build numbers/suffixes for clean comparison
                        val cleanInstalled = installedVersion.removePrefix("v").removePrefix("V")
                             .replace("-spoofed", "", ignoreCase = true)
                             .substringBefore("_")
                             .substringBefore("-release")
                             .trim()
                        
                        if (cleanTag != cleanInstalled) {
                            updateAppState(app.packageName) {
                                it.copy(
                                    isUpdateAvailable = true,
                                    latestVersion = release.tagName,
                                    latestDownloadUrl = release.downloadUrl
                                )
                            }
                        } else {
                            // Apps match! Clear update flag
                            updateAppState(app.packageName) {
                                it.copy(
                                    isUpdateAvailable = false,
                                    latestVersion = null,
                                    latestDownloadUrl = null
                                )
                            }
                        }
                    } else {
                        // Not installed, so not an update available (it's a new install)
                         updateAppState(app.packageName) {
                            it.copy(isUpdateAvailable = false)
                        }
                    }
                }
            }
        }
    }

    private fun cleanupApk(packageName: String) {
        try {
            val fileName = "appdownloader_${packageName.replace(".", "_")}.apk"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (file.exists()) {
                file.delete()
                // Update state to remove reference to file if it was there
                updateAppState(packageName) {
                    it.copy(downloadedFile = null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getInstalledVersion(pm: PackageManager, packageName: String): String? {
        return try {
            val pInfo = pm.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun startDownload(context: Context, app: ApkAppEntry) {
        // ... (existing startDownload logic)
        val url = appStates[app.packageName]?.latestDownloadUrl ?: app.downloadUrl
        
        try {
            val fileName = "appdownloader_${app.packageName.replace(".", "_")}.apk"
            // Delete existing if any
            val existing = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (existing.exists()) existing.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Downloading ${app.displayName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")
            
            val id = downloadManager.enqueue(request)
            
            updateAppState(app.packageName) {
                it.copy(isDownloading = true, downloadId = id, progress = 0f, downloadedFile = null)
            }
            
            startProgressMonitoring()
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error (maybe expose a shared flow for errors)
        }
    }

    // ... (rest of the file)
    private fun startProgressMonitoring() {
        if (progressJob?.isActive == true) return
        
        progressJob = viewModelScope.launch {
            while (isActive) {
                val activeDownloads = _appStates.filter { it.value.isDownloading && it.value.downloadId != null }
                if (activeDownloads.isEmpty()) break
                
                activeDownloads.forEach { (pkg, state) ->
                    val query = DownloadManager.Query().setFilterById(state.downloadId!!)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                             val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                             val fileUriString = cursor.getString(uriIndex)
                             val file = if (fileUriString != null) File(Uri.parse(fileUriString).path!!) else null

                            updateAppState(pkg) {
                                it.copy(isDownloading = false, progress = 1f, downloadedFile = file)
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            updateAppState(pkg) {
                                it.copy(isDownloading = false, progress = 0f)
                            }
                        } else {
                            val progress = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f
                            updateAppState(pkg) {
                                it.copy(progress = progress)
                            }
                        }
                    } else {
                        // Download cancelled or gone
                         updateAppState(pkg) {
                             it.copy(isDownloading = false)
                         }
                    }
                    cursor.close()
                }
                delay(500)
            }
        }
    }
    
    fun getInstallIntent(context: Context, file: File): Intent? {
        if (!file.exists()) return null
        
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
        
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun updateAppState(packageName: String, update: (AppState) -> AppState) {
        val current = _appStates[packageName] ?: AppState()
        _appStates[packageName] = update(current)
    }
    
    fun resetState(packageName: String) {
        updateAppState(packageName) {
             it.copy(isDownloading = false, progress = 0f, downloadId = null, downloadedFile = null)
        }
    }
}
