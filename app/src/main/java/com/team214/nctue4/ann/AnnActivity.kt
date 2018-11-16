package com.team214.nctue4.ann

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.client.E3ClientFactory
import com.team214.nctue4.client.E3Type
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.utility.downloadFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_ann.*
import kotlinx.android.synthetic.main.status_error.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.PatternSyntaxException

class AnnActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var url: String
    private lateinit var fileName: String
    private lateinit var client: E3Client
    private var disposable: Disposable? = null

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
        val annItem = intent?.extras?.getParcelable<AnnItem>("annItem")
        client = E3ClientFactory.createFromAnn(this, annItem!!)
        disposable = client.getAnn(annItem)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { showData(it) },
                onError = { showError() }
            )
    }

    private fun showError() {
        error_request?.visibility = View.VISIBLE
        error_request_retry?.setOnClickListener { getData() }
        progress_bar?.visibility = View.GONE
    }

    private fun showData(annItem: AnnItem) {
        error_request.visibility = View.GONE
        // replace <img src="/...> to <img src="http://e3.nctu.edu.tw/..."
        val content =
            if (annItem.e3Type == E3Type.OLD) {
                try {
                    Regex("(?<=(<img[.\\s\\S^>]{0,300}src[ \n]{0,300}=[ \n]{0,300}\"))(/)(?=([^/]))")
                        .replace(annItem.content!!, "http://e3.nctu.edu.tw/")
                } catch (e: PatternSyntaxException) {
                    annItem.content
                }
            } else annItem.content
        ann_title.text = annItem.title
        ann_courseName.text = annItem.courseName
        if (annItem.date != null) {
            ann_date.text = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN).format(annItem.date)
        }
        ann_content_web_view.settings.defaultTextEncodingName = "utf-8"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ann_content_web_view.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        ann_content_web_view.loadData(content, "text/html; charset=utf-8", "UTF-8")
        ann_content_web_view.setBackgroundColor(Color.TRANSPARENT)
        announcement_attach.layoutManager = LinearLayoutManager(this)
        announcement_attach.adapter = AnnAttachmentAdapter(this, annItem.attachItems) {
            url = it.url
            fileName = it.name
            downloadFile(fileName, url, this, this, ann_root, client.getCookie()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                }
            }

        }
        ann_container?.visibility = View.VISIBLE
        progress_bar?.visibility = View.GONE
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
                    downloadFile(fileName, url, this, this, ann_root, client.getCookie(), null)
                }
                return
            }
        }
    }
}