package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import io.reactivex.Observable
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class OldE3Client(context: Context) : E3Client() {
    class SessionExpiredException : Exception()

    companion object {
        const val WEB_URL = "https://e3.nctu.edu.tw/NCTU_Easy_E3P/lms31"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var sessionId: Cookie? = null
    private var aspXAuth: Cookie? = null
    private var viewState: String = ""
    private val client = OkHttpClient().newBuilder()
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): MutableList<Cookie>? {
                val cookies = mutableListOf<Cookie>()
                if (sessionId != null) cookies.add(sessionId!!)
                if (aspXAuth != null) cookies.add(aspXAuth!!)
                return cookies
            }

            override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
                for (cookie in cookies) {
                    when (cookie.name()) {
                        "ASP.NET_SessionId" -> sessionId = cookie
                        ".ASPXAUTH_NCTU_EASY_E3P_140.113.8.80" -> aspXAuth = cookie
                    }
                }
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun get(path: String): Observable<Response> {
        val request = Request.Builder()
            .url(WEB_URL + path)
            .build()
        return Observable.fromCallable {
            Log.d("OldE3Get",path)
            client.newCall(request).execute().apply {
                if (this.header("Location", "")!!.contains(Regex("(logout|login)"))) {
                    print("Throw get")
                    throw SessionExpiredException()
                }
            }
        }.retryWhen {
            it.filter { error -> error is SessionExpiredException }
                .flatMap { _ -> login() }
                .take(1)
                .concatMap { _ -> Observable.error<ServiceErrorException>(ServiceErrorException()) }
        }
    }


    private fun post(
        path: String,
        data: HashMap<String, String>
    ): Observable<Response> {
        val formBodyBuilder = FormBody.Builder()
        data.forEach { entry -> formBodyBuilder.add(entry.key, entry.value) }
        formBodyBuilder.add("__VIEWSTATE", viewState)
        val formBody = formBodyBuilder.build()
        val request = okhttp3.Request.Builder()
            .url(WEB_URL + path)
            .post(formBody)
            .build()
        return Observable.fromCallable {
            Log.d("OldE3Post",path)
            client.newCall(request).execute().apply {
                if (this.header("Location", "")!!.contains(Regex("(logout|login)"))) {
                    print("Throw post")
                    throw SessionExpiredException()
                }
            }
        }.retryWhen {
            it.filter { error -> error is SessionExpiredException }
                .flatMap { _ -> login() }
                .take(1)
                .concatMap { _ -> Observable.error<ServiceErrorException>(ServiceErrorException()) }
        }
    }

    private fun parseHtmlResponse(response: Response): Observable<Document> {
        return Observable.fromCallable {
            Jsoup.parse(response.body()!!.string()).also {
                viewState = it.select("#__VIEWSTATE").attr("value")
            }
        }
    }

    override fun login(
        studentId: String?,
        password: String?
    ): Observable<Unit> {
        sessionId = null
        aspXAuth = null
        return get("/login.aspx")
            .flatMap { it -> parseHtmlResponse(it) }
            .flatMap {
                post(
                    "/login.aspx", hashMapOf(
                        "txtLoginId" to (studentId ?: prefs.getString("studentId", "")!!),
                        "txtLoginPwd" to (password ?: prefs.getString("studentPassword", "")!!),
                        "btnLogin.x" to "0",
                        "btnLogin.y" to "0"
                    )
                )
            }
            .flatMap {
                Observable.fromCallable {
                    if (it.body()!!.string().contains("window.location.href='login.aspx';")) {
                        throw WrongCredentialsException()
                    }
                }
            }
    }

}