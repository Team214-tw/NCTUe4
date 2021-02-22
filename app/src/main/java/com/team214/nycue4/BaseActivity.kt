package com.team214.nycue4

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.team214.nycue4.utility.openFile
import java.io.File
import java.net.URI

abstract class BaseActivity(rootViewId: Int? = null) : AppCompatActivity() {
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
            if (cursor.moveToFirst()) {
                val title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
                val localURI = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))

                Snackbar.make(
                    findViewById(rootViewId ?: android.R.id.content),
                    "$title ${context.getString(R.string.download_completed)}",
                    Snackbar.LENGTH_LONG
                ).setAction(context.getString(R.string.open_file)) {
                    openFile(title, File(URI(localURI)), context)
                }.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(downloadCompleteReceiver)
    }
}