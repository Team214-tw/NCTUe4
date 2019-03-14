package com.team214.nctue4

import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import okhttp3.Cookie


class MainApplication : MultiDexApplication() {
    var newE3Session: Cookie? = null
    var oldE3Session: Cookie? = null
    var oldE3AspXAuth: Cookie? = null
    var oldE3ViewState: String = ""
    var oldE3CurrentPage: String = "notLoggedIn"
    val oldE3CourseIdMap = HashMap<String, String>()

    override fun onCreate() {
        super.onCreate()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        when (prefs.getString("night_mode", "MODE_NIGHT_FOLLOW_SYSTEM")) {
            "MODE_NIGHT_FOLLOW_SYSTEM" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "MODE_NIGHT_YES" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "MODE_NIGHT_NO" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}