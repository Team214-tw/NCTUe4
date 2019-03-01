package com.team214.nctue4.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.team214.nctue4.BaseActivity
import com.team214.nctue4.BuildConfig
import com.team214.nctue4.R
import com.team214.nctue4.SettingsFragment
import com.team214.nctue4.client.E3Type
import com.team214.nctue4.client.NewE3ApiClient
import com.team214.nctue4.client.NewE3WebClient
import com.team214.nctue4.client.OldE3Client
import com.team214.nctue4.login.LogoutActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var oldE3Client: OldE3Client
    lateinit var newE3ApiClient: NewE3ApiClient
    lateinit var newE3WebClient: NewE3WebClient
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val studentEmail = prefs.getString("studentEmail", "")
        val studentName = prefs.getString("studentName", "")

        oldE3Client = OldE3Client(this)
        newE3ApiClient = NewE3ApiClient(this)
        newE3WebClient = NewE3WebClient(this)

        if (savedInstanceState == null) {
            switchFragment(
                when (intent?.getStringExtra("shortcut")) {
                    "nav_ann" -> R.id.nav_ann
                    "nav_bookmarked" -> R.id.nav_bookmarked
                    "nav_download" -> R.id.nav_download
                    "nav_old_e3" -> R.id.nav_old_e3
                    "nav_new_e3" -> R.id.nav_new_e3
                    else -> R.id.nav_home
                }
            )
        }

        nav_view.getHeaderView(0).findViewById<TextView>(R.id.student_name).text = studentName
        nav_view.getHeaderView(0).findViewById<TextView>(R.id.student_email).text = studentEmail
    }

    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) == null -> switchFragment(R.id.nav_home)
            else -> super.onBackPressed()
        }
    }

    fun switchFragment(id: Int) {
        nav_view.setCheckedItem(id)
        val fragment = when (id) {
            R.id.nav_home -> {
                firebaseAnalytics
                    .setCurrentScreen(this, "HomeFragment", HomeFragment::class.java.simpleName)
                HomeFragment()
            }
            R.id.nav_ann -> {
                firebaseAnalytics
                    .setCurrentScreen(this, "HomeAnnFragment", HomeAnnFragment::class.java.simpleName)
                HomeAnnFragment()
            }
            R.id.nav_bookmarked -> {
                firebaseAnalytics
                    .setCurrentScreen(this, "BookmarkedFragment", BookmarkedFragment::class.java.simpleName)
                BookmarkedFragment()
            }
            R.id.nav_download -> {
                firebaseAnalytics
                    .setCurrentScreen(this, "DownloadFragment", DownloadFragment::class.java.simpleName)
                DownloadFragment()
            }
            R.id.nav_old_e3 -> {
                firebaseAnalytics
                    .setCurrentScreen(this, "OldE3Fragment", CourseListFragment::class.java.simpleName)
                CourseListFragment().apply {
                    val bundle = Bundle()
                    bundle.putSerializable("e3Type", E3Type.OLD)
                    this.arguments = bundle
                }
            }
            R.id.nav_new_e3 -> {
                firebaseAnalytics
                    .setCurrentScreen(this, "NewE3Fragment", CourseListFragment::class.java.simpleName)
                CourseListFragment().apply {
                    val bundle = Bundle()
                    bundle.putSerializable("e3Type", E3Type.NEW)
                    this.arguments = bundle
                }
            }
            R.id.settings -> {
                firebaseAnalytics
                    .setCurrentScreen(this, "SettingsFragment", CourseListFragment::class.java.simpleName)
                SettingsFragment()
            }
            R.id.nav_log_out -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_confirm)
                    .setPositiveButton(R.string.positive) { _, _ ->
                        val intent = Intent(this, LogoutActivity::class.java)
                        startActivity(intent)
                        finish()
                    }.setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }.show()
                null
            }
            R.id.nav_feedback -> {
                val emailUri = "mailto: team214.csv06@gmail.com?subject=NCTUE4Feedback&body=" +
                        "\n\n\nAPI Level: ${android.os.Build.VERSION.SDK_INT}\n" +
                        "Device: ${android.os.Build.DEVICE}\n" +
                        "Model: ${android.os.Build.MODEL}\n" +
                        "Build: ${android.os.Build.DISPLAY}\n" +
                        "App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse(emailUri)
                startActivity(intent)
                null
            }
            R.id.nav_about -> {
                firebaseAnalytics.setCurrentScreen(this, "LicenseDialog", LicenseDialog::class.java.simpleName)
                LicenseDialog().show(supportFragmentManager, "TAG")
                null
            }
            else -> {
                firebaseAnalytics.setCurrentScreen(this, "HomeFragment", HomeAnnFragment::class.java.simpleName)
                HomeFragment()
            }
        }
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment, fragment.javaClass.simpleName).commit()
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        switchFragment(item.itemId)
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}
