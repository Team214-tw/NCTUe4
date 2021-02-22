package com.team214.nycue4.utility

import android.Manifest
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.team214.nycue4.R
import okhttp3.Cookie
import java.io.File

fun openFile(fileName: String, file: File, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW)
    val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
    val type =
        if (extension != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else {
            null
        }
    val fileUri = FileProvider.getUriForFile(
        context, "${context.applicationContext.packageName}.provider", file
    )
    intent.setDataAndType(fileUri, type)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_compatible_app, Toast.LENGTH_SHORT).show()
    }
}

fun downloadFile(
    fileName: String, uri: String, context: Context,
    cookies: MutableList<Cookie>? = null, requestPermissions: (() -> Unit?)?
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED
    ) {
        requestPermissions?.invoke()
    } else {
        val path = context.getExternalFilesDir(null)
        val dir = File(path, "Download")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        if (file.exists()) {
            AlertDialog.Builder(context)
                .setTitle(R.string.warning)
                .setMessage(context.getString(R.string.detect_same_file))
                .setPositiveButton(R.string.download_again) { _, _ ->
                    file.delete()
                    downloadFile(fileName, uri, context, cookies, requestPermissions)
                }
                .setNegativeButton(R.string.open_existed) { _, _ ->
                    openFile(fileName, file, context)
                }
                .show()
        } else {
            val request = DownloadManager.Request(Uri.parse(uri))
            if (cookies != null) {
                var cookieString = ""
                cookies.forEach { cookieString += "${it.name()}=${it.value()};" }
                request.addRequestHeader("Cookie", cookieString)
            }
            request.setTitle(fileName)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            request.setDestinationUri(Uri.fromFile(file))
            request.setVisibleInDownloadsUi(true)
            Toast.makeText(context, R.string.download_start, Toast.LENGTH_SHORT).show()
            manager.enqueue(request)
        }
    }
}