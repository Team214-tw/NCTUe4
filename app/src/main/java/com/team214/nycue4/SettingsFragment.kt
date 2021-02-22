package com.team214.nycue4

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat


class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref)
        activity!!.setTitle(R.string.settings)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        when (key) {
            "night_mode" -> {
                when (prefs.getString("night_mode", "MODE_NIGHT_FOLLOW_SYSTEM")) {
                    "MODE_NIGHT_FOLLOW_SYSTEM" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    "MODE_NIGHT_YES" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "MODE_NIGHT_NO" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                activity?.recreate()
            }
            "ann_enable_new_e3_system" -> prefs.edit().putLong("home_ann_last_refresh", -1).apply()
        }
    }

}
