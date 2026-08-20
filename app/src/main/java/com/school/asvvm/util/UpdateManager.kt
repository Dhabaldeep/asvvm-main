package com.school.asvvm.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val REPO_URL = "https://api.github.com/repos/Dhabaldeep/asvvm-main/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(REPO_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val tagName = json.getString("tag_name")
                val releaseNotes = json.optString("body", "No release notes provided.")
                val remoteVersion = tagName.removePrefix("v")
                val localVersion = currentVersion.removePrefix("v")
                
                if (isNewerVersion(localVersion, remoteVersion)) {
                    val assets = json.getJSONArray("assets")
                    var downloadUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }
                    
                    if (downloadUrl.isNotEmpty()) {
                        return@withContext UpdateInfo(remoteVersion, downloadUrl, releaseNotes)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
        }
        return@withContext null
    }

    private fun isNewerVersion(local: String, remote: String): Boolean {
        try {
            val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(localParts.size, remoteParts.size)
            for (i in 0 until maxLength) {
                val l = localParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (l < r) return true
                if (l > r) return false
            }
        } catch (e: Exception) {
            // Fallback to simple string comparison if semantic version parsing fails
            return remote > local
        }
        return false
    }

    fun startDownload(context: Context, updateInfo: UpdateInfo) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(updateInfo.downloadUrl)
            
            val request = DownloadManager.Request(uri)
                .setTitle("ASVVM Update v${updateInfo.version}")
                .setDescription("Downloading latest update")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "asvvm-update-v${updateInfo.version}.apk")
                .setMimeType("application/vnd.android.package-archive")

            val downloadId = downloadManager.enqueue(request)
            
            // Register receiver to trigger install when download completes
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(context, id)
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to unregister receiver", e)
                        }
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
        }
    }

    private fun installApk(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (uriIndex != -1) {
                    val localUriString = cursor.getString(uriIndex)
                    val uri = Uri.parse(localUriString)
                    
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        context.startActivity(installIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch installer", e)
                    }
                }
            }
        }
        cursor.close()
    }
}
