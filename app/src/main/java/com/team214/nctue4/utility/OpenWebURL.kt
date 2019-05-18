package com.team214.nctue4.utility

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

fun openWebURL(context: Context, url: String) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    if (prefs.getString("open_link_in", "CUSTOM_TAB") == "CUSTOM_TAB") {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } else {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        ContextCompat.startActivity(context, intent, null)
    }

}