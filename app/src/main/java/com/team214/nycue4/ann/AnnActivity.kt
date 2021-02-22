package com.team214.nycue4.ann

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.team214.nycue4.BaseActivity
import com.team214.nycue4.R
import com.team214.nycue4.client.E3Client
import com.team214.nycue4.client.E3ClientFactory
import com.team214.nycue4.course.CourseActivity
import com.team214.nycue4.model.AnnItem
import com.team214.nycue4.model.CourseDBHelper
import com.team214.nycue4.utility.downloadFile
import com.team214.nycue4.utility.injectHtml
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_ann.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_error.*
import java.text.SimpleDateFormat
import java.util.*


class AnnActivity : BaseActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var url: String
    private lateinit var fileName: String
    private lateinit var client: E3Client
    private var disposable: Disposable? = null
    private lateinit var annItem: AnnItem

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        setContentView(R.layout.activity_ann)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        getData()
    }

    private fun getData() {
        error_request.visibility = View.GONE
        progress_bar?.visibility = View.VISIBLE
        annItem = intent?.extras?.getParcelable("annItem")!!
        client = E3ClientFactory.createFromAnn(this, annItem)
        disposable = client.getAnn(annItem)
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onNext = { showData(it) },
                onError = { showError() }
            )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (intent.extras!!.getBoolean("fromHome", false))
            menuInflater.inflate(R.menu.goto_course, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_goto_course -> {
                val courseItem = CourseDBHelper(this).getCourseFromName(annItem.courseName, annItem.e3Type)
                if (courseItem != null) {
                    val intent = Intent()
                    intent.setClass(this, CourseActivity::class.java)
                    intent.putExtra("courseItem", courseItem)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, getString(R.string.course_not_found_in_db), Toast.LENGTH_LONG).show()
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showError() {
        error_request?.visibility = View.VISIBLE
        error_request_retry?.setOnClickListener { getData() }
    }

    private fun showData(annItem: AnnItem) {
        error_request.visibility = View.GONE
        ann_title.text = annItem.title
        ann_courseName.text = annItem.courseName
        ann_date.text =
            if (annItem.date != null) {
                SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN).format(annItem.date)
            } else ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ann_content_web_view.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val cookies = client.getCookie()
        cookieManager.removeAllCookie()
        cookies?.forEach { cookie ->
            val cookieString = cookie.name() + "=" + cookie.value() + "; Domain=" + cookie.domain()
            cookieManager.setCookie(cookie.domain(), cookieString)
        }

        ann_content_web_view.loadDataWithBaseURL(
            client.getBaseUrl(),
            injectHtml(annItem.content, this),
            "text/html",
            "UTF-8",
            null
        )

        val typedValue = TypedValue()
        this.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        ann_content_web_view.setBackgroundColor(typedValue.data)
        ann_content_web_view.settings.javaScriptEnabled = true
        ann_content_web_view.scrollBarDefaultDelayBeforeFade = 0

        announcement_attach.layoutManager = LinearLayoutManager(this)
        announcement_attach.adapter = AnnAttachmentAdapter(this, annItem.attachItems) {
            url = it.url
            fileName = it.name
            downloadFile(fileName, url, this, client.getCookie()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                }
            }

        }
        ann_container?.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    downloadFile(fileName, url, this, client.getCookie(), null)
                }
                return
            }
        }
    }
}
