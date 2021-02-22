package com.team214.nycue4.utility

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat.startActivity

class NestedWebView : WebView {
    private var startx: Float = 0.toFloat()
    private var starty: Float = 0.toFloat()

    init {
        this.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                if (url.startsWith("http")) {
                    openWebURL(context, url)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(context, intent, null)
                }
                return true
            }
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                startx = event.x
                starty = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (Math.abs(event.x - startx) > Math.abs(event.y - starty)) {
                    parent.requestDisallowInterceptTouchEvent(true)
                } else {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
