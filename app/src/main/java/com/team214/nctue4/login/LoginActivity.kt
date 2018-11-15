package com.team214.nctue4.login

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.client.NewE3ApiClient
import com.team214.nctue4.client.OldE3Client
import com.team214.nctue4.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //detect soft keyboard
        login_root.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = login_root.rootView.height - login_root.height
            if (heightDiff > dpToPx(200f)) {
                login_scroll_view.smoothScrollBy(0, heightDiff)
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (prefs.getString("studentId", "") != "") {
            student_id.isEnabled = false
            logout_button.visibility = View.VISIBLE
            student_id.setText(prefs.getString("studentId", null))
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

        val oldE3Client = OldE3Client(this)
        val newE3ApiClient = NewE3ApiClient(this)
        login_button?.setOnClickListener {
            disableInput()

            val studentId = student_id.text.toString().trim()
            val studentPassword = student_password.text.toString()
            var studentPortalPassword = student_portal_password.text.toString()
            if (studentPortalPassword == "") studentPortalPassword = studentPassword

            oldE3Client.login(studentId, studentPassword)
                .mergeWith(
                    newE3ApiClient.login(
                        studentId,
                        studentPortalPassword
                    ).flatMap { _ -> newE3ApiClient.saveUserId() })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        val prefsEditor = prefs.edit()
                        prefsEditor.putString("studentPassword", studentPassword)
                        prefsEditor.putString("studentId", studentId)
                        prefsEditor.putString("studentPortalPassword", studentPortalPassword)
                        prefsEditor.apply()
                        handleLoginSuccess()
                    },
                    onError = { error ->
                        when (error) {
                            is E3Client.WrongCredentialsException -> handleWrongCredentials()
                            else -> handleServiceError()
                        }
                    }
                )
        }
    }


    private fun handleLoginSuccess() {
        Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleWrongCredentials() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prefsEditor = prefs.edit()
        prefsEditor.remove("studentPassword")
        prefsEditor.remove("studentId")
        prefsEditor.remove("studentPortalPassword")
        prefsEditor.apply()
        login_error_text_view.text = getString(R.string.login_id_or_password_error)
        enableInput()
    }

    private fun handleServiceError() {
        login_error_text_view.text = getString(R.string.generic_error)
        enableInput()
    }

    private fun enableInput() {
        login_error_text_view?.visibility = View.VISIBLE
        login_progressbar?.visibility = View.GONE
        login_button?.text = getString(R.string.login)
        student_id.isEnabled = true
        student_password.isEnabled = true
        student_portal_password.isEnabled = true
        login_button?.isEnabled = true
    }

    private fun disableInput() {
        student_id.isEnabled = false
        student_password.isEnabled = false
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
