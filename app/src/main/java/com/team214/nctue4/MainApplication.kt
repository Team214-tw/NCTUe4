package com.team214.nctue4

import androidx.multidex.MultiDexApplication
import okhttp3.Cookie

class MainApplication : MultiDexApplication() {
    var newE3Session: Cookie? = null
    var oldE3Session: Cookie? = null
    var oldE3AspXAuth: Cookie? = null
    var oldE3ViewState: String = ""
    var oldE3CurrentPage: String = "notLoggedIn"
    val oldE3CourseIdMap = HashMap<String, String>()
}