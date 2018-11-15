package com.team214.nctue4

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import com.team214.nctue4.login.LoginActivity

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)  //End Splash Screen
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        prefs.edit().putInt("versionCode", packageManager.getPackageInfo(packageName, 0).versionCode).apply()

        val studentId = prefs.getString("studentId", null)
        val studentPassword = prefs.getString("studentPassword", null)
        val studentPortalPassword = prefs.getString("studentPortalPassword", null)
        val token = prefs.getString("newE3Token", null)
        val userId = prefs.getString("newE3UserId", null)
        val studentName = prefs.getString("studentName", null)

        if (studentId == null || studentPassword == null || studentPortalPassword == null ||
            token == null || userId == null || studentName == null
        ) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
