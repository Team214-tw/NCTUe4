package com.team214.nctue4

import android.app.Application
import okhttp3.Cookie

class MainApplication : Application() {
    var newE3Session: Cookie? = null
    var oldE3Session: Cookie? = null
    var oldE3AspXAuth: Cookie? = null
    var oldE3ViewState: String = ""
    var oldE3CurrentPage: String = "notLoggedIn"
    val oldE3CourseIdMap = HashMap<String, String>()
}