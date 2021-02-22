package com.team214.nycue4.login

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.team214.nycue4.LandingActivity
import com.team214.nycue4.R
import com.team214.nycue4.model.CourseDBHelper
import java.io.File

class LogoutActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prefsEditor = prefs.edit()
        prefsEditor.clear().apply()
        val dbHelper = CourseDBHelper(this)
        dbHelper.delTable()
        val path = this.getExternalFilesDir(null)
        val dir = File(path, "Download")
        dir.deleteRecursively()
        Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LandingActivity::class.java)
        startActivity(intent)
        finish()
    }
}
