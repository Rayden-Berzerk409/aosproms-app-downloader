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
                        // 1. Try to find an APK that is NOT a Huawei variant
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val url = asset.getString("browser_download_url")
                            if (url.endsWith(".apk", ignoreCase = true) && !url.contains("-hw", ignoreCase = true)) {
                                downloadUrl = url
                                break
                            }
                        }

                        // 2. If no non-hw APK found, try any APK
                        if (downloadUrl.isEmpty()) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val url = asset.getString("browser_download_url")
                                if (url.endsWith(".apk", ignoreCase = true)) {
                                    downloadUrl = url
                                    break
                                }
                            }
                        }

                        // 3. Fallback to first asset if no APK found
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
