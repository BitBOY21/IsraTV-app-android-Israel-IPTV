package com.isratv.android.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import android.os.Environment
import android.app.DownloadManager

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            
            // Assuming the download was successful, trigger the install
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "TvStreams_update.apk")
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "com.isratv.android.fileprovider", file)
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                try {
                    context.startActivity(installIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
