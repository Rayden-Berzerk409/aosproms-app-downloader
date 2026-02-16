package com.aosprom.appdownloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

data class GithubRelease(
    val tagName: String,
    val downloadUrl: String
)

object GithubUpdateChecker {
    suspend fun getLatestRelease(repo: String): GithubRelease? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/$repo/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val json = JSONObject(response.toString())
                    val tagName = json.getString("tag_name")
                    val assets = json.getJSONArray("assets")
                    
                    var downloadUrl = ""
                    if (assets.length() > 0) {
                        val candidates = mutableListOf<String>()
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val url = asset.getString("browser_download_url")
                            if (url.endsWith(".apk", ignoreCase = true)) {
                                candidates.add(url)
                            }
                        }

                        // Filter out debug (unless no other option)
                        var filtered = candidates.filter { !it.contains("debug", ignoreCase = true) }
                        if (filtered.isEmpty()) filtered = candidates

                        if (repo.equals("KernelSU-Next/KernelSU-Next", ignoreCase = true)) {
                            // KernelSU Next: Prefer "spoofed" variant
                            val spoofed = filtered.find { it.contains("spoofed", ignoreCase = true) }
                            downloadUrl = spoofed ?: filtered.firstOrNull() ?: ""
                        } else {
                            // General: Avoid "-hw" variant (Huawei)
                            val standard = filtered.find { !it.contains("-hw", ignoreCase = true) }
                            downloadUrl = standard ?: filtered.firstOrNull() ?: ""
                        }

                        // Fallback to first asset if no APK found (unlikely but safe)
                        if (downloadUrl.isEmpty()) {
                            downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                        }
                    }
                    
                    if (downloadUrl.isNotEmpty()) {
                        GithubRelease(tagName, downloadUrl)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
