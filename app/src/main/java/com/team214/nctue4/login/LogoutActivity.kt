package com.team214.nctue4.login

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.team214.nctue4.R
import com.team214.nctue4.model.CourseDBHelper
import java.io.File

class LogoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prefsEditor = prefs.edit()
        prefsEditor.clear().apply()
        val dbHelper = CourseDBHelper(this)
        dbHelper.delTable()
        val path = this.getExternalFilesDir(null)
        val dir = File(path, "Download")
        dir.deleteRecursively()
        Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
