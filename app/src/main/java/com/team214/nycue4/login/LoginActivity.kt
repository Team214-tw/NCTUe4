package com.team214.nycue4.login

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.team214.nycue4.R
import com.team214.nycue4.client.E3Client
import com.team214.nycue4.client.E3Clients
import com.team214.nycue4.client.NewE3ApiClient
import com.team214.nycue4.main.MainActivity
import com.team214.nycue4.model.CourseDBHelper
import com.team214.nycue4.model.CourseItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var disposable: Disposable? = null
    private lateinit var newE3ApiClient: NewE3ApiClient
    private var loggedInBefore = false

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        //detect soft keyboard
        login_root.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = login_root.rootView.height - login_root.height
            if (heightDiff > dpToPx(200f)) {
                login_scroll_view.smoothScrollBy(0, heightDiff)
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (prefs.getString("studentId", "") != "") {
            loggedInBefore = true
            student_id.isEnabled = false
            logout_button.visibility = View.VISIBLE
            student_id.setText(prefs.getString("studentId", null))
            student_portal_password.setText(prefs.getString("studentPortalPassword", ""))
        }

        login_help.paintFlags = login_help.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        login_help?.setOnClickListener { LoginHelpFragment().show(supportFragmentManager, "LoginHelpFragment") }

        logout_button?.setOnClickListener {
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
        }

        newE3ApiClient = E3Clients.getNewE3ApiClient(this)
        login_button?.setOnClickListener {
            disableInput()

            val studentId = student_id.text.toString().trim()
            val studentPortalPassword = student_portal_password.text.toString()

            var observable = newE3ApiClient.login(studentId, studentPortalPassword)

            disposable = observable
                .flatMap { newE3ApiClient.saveUserInfo(studentId) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        val prefsEditor = prefs.edit()
                        prefsEditor.putString("studentId", studentId)
                        prefsEditor.putString("studentPortalPassword", studentPortalPassword)
                        prefsEditor.apply()
                        getCourseList()
                    },
                    onError = { error ->
                        when (error) {
                            is E3Client.WrongCredentialsException -> handleWrongCredentials()
                            is NewE3ApiClient.SitePolicyNotAgreedException -> handleSitePolicyNotAgreedException()
                            else -> handleServiceError()
                        }
                    }
                )
        }
    }

    private fun getCourseList() {
        val courseDBHelper = CourseDBHelper(this)
        if (!courseDBHelper.isCoursesTableEmpty()) {
            handleLoginSuccess()
            return
        }
        var observable = newE3ApiClient.getCourseList()
        disposable = observable
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(mutableListOf<CourseItem>()) { courseItems, courseItem -> courseItems.add(courseItem) }
            .subscribeBy(
                onSuccess = {
                    courseDBHelper.addCourses(it)
                    handleLoginSuccess()
                },
                onError = { handleLoginSuccess() }
            )
    }


    private fun handleLoginSuccess() {
        Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleWrongCredentials() {
        login_error_text_view.setOnClickListener(null)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prefsEditor = prefs.edit()
        prefsEditor.remove("studentPassword")
        prefsEditor.remove("studentId")
        prefsEditor.remove("studentPortalPassword")
        prefsEditor.apply()
        login_error_text_view.text = getString(R.string.login_id_or_password_error)
        enableInput()
    }

    private fun handleSitePolicyNotAgreedException() {
        login_error_text_view.text = getString(R.string.click_to_agree_site_policy)
        login_error_text_view.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://e3new.nctu.edu.tw/user/policy.php")))
        }
        enableInput()
    }

    private fun handleServiceError() {
        login_error_text_view.setOnClickListener(null)
        login_error_text_view.text = getString(R.string.generic_error)
        enableInput()
    }

    private fun enableInput() {
        login_error_text_view?.visibility = View.VISIBLE
        login_progressbar?.visibility = View.GONE
        login_button?.text = getString(R.string.login)
        if (!loggedInBefore) student_id.isEnabled = true
        student_portal_password.isEnabled = true
        login_button?.isEnabled = true
    }

    private fun disableInput() {
        student_id.isEnabled = false
        student_portal_password.isEnabled = false
        login_progressbar?.visibility = View.VISIBLE
        login_button?.text = ""
        login_button?.isEnabled = false
    }

    private fun dpToPx(valueInDp: Float): Float {
        val metrics = resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics)
    }
}
