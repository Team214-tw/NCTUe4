package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.model.CourseItem
import com.team214.nctue4.model.FileItem
import com.team214.nctue4.model.FolderItem
import io.reactivex.Observable
import okhttp3.*
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class NewE3WebClient(context: Context) : E3Client() {
    private var sessionId: Cookie? = null
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    class SessionInvalidException : Exception()

    private val client = OkHttpClient().newBuilder()
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): MutableList<Cookie>? {
                val cookies = mutableListOf<Cookie>()
                if (sessionId != null) cookies.add(sessionId!!)
                return cookies
            }

            override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
                for (cookie in cookies) {
                    when (cookie.name()) {
                        "MoodleSession" -> sessionId = cookie
                    }
                }
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override fun login(studentId: String?, password: String?): Observable<Unit> {
        return Observable.fromCallable {
            Log.d("NewE3Web", "Login")
            val formBody = FormBody.Builder()
                .add("username", (studentId ?: prefs.getString("studentId", "")!!))
                .add("password", (password ?: prefs.getString("studentPortalPassword", "")!!))
                .build()
            val request = okhttp3.Request
                .Builder()
                .url("https://e3new.nctu.edu.tw/login/index.php?lang=en")
                .post(formBody)
                .build()
            client.newCall(request).execute().apply {
                if (this.header("Location") == "https://e3new.nctu.edu.tw/login/index.php") {
                    throw WrongCredentialsException()
                }
            }
            Unit
        }
    }

    fun get(path: String): Observable<Response> {
        return Observable.fromCallable {
            if (sessionId == null) throw SessionInvalidException()
            Log.d("NewE3Web", path)
            val request = okhttp3.Request
                .Builder()
                .url(path)
                .get()
                .build()
            client.newCall(request).execute().apply {
                if (this.header("Location") == "https://e3new.nctu.edu.tw/login/index.php") {
                    throw SessionInvalidException()
                }

            }
        }.retryWhen {
            it.flatMap { error ->
                if (error is SessionInvalidException) login() else Observable.error(error)
            }
        }
    }

    override fun getFrontPageAnns(): Observable<MutableList<AnnItem>> {
        return get("https://e3new.nctu.edu.tw/theme/dcpc/news/index.php?lang=en").flatMap {
            Observable.fromCallable {
                val document = Jsoup.parse(it.body()!!.string()).apply {
                    if (this.selectFirst(".login > a")?.text() == "Log in" ||
                        this.selectFirst("body > div")?.text() == "Invalid user"
                    ) {
                        throw SessionInvalidException()
                    }
                }

                val annItems = mutableListOf<AnnItem>()
                val newsRowEls = document.select(".NewsRow").dropLast(1)
                val df = SimpleDateFormat("d MMM, HH:mm", Locale.US)
                newsRowEls.forEach { el ->
                    if (el.select(".colL-10").text() == "System") return@forEach
                    val date = df.parse(el.selectFirst(".colR-10").text())
                    val now = Calendar.getInstance()
                    val curMonth = now.get(Calendar.MONTH)
                    val curYear = now.get(Calendar.YEAR) - 1900
                    // 嘗試猜公告的年份為何
                    date.year =
                            if (curMonth >= 9 && date.month <= 2) curYear + 1
                            else if (curMonth <= 2 && date.month >= 9) curYear - 1
                            else curYear

                    // Detail ann link
                    val detailLocationHint =
                        Regex("location\\.href='(.*)';")
                            .find(el.select("div").attr("onclick"))!!
                            .groups[1]!!.value

                    val courseName = el.select(".colL-10")
                        .attr("title")
                        .split("\\xa0".toRegex())[1].split(" ")[0]

                    val title = el.select(".colL-19").text()
                    annItems.add(AnnItem(E3Type.NEW, title, date, courseName, "$detailLocationHint&lang=en"))
                }
                annItems
            }
        }.retryWhen {
            it.flatMap { error ->
                if (error is SessionInvalidException) login() else Observable.error(error)
            }
        }
    }

    override fun getAnn(annItem: AnnItem): Observable<AnnItem> {
        return get(annItem.detailLocationHint!!).flatMap {
            Observable.fromCallable {
                val document = Jsoup.parse(it.body()!!.string())
                val df = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US)

                val attachItems =
                    document.select(".attachments").map { el ->
                        FileItem(
                            el.select("a").last().text(),
                            el.select("a").last().attr("href")
                        )
                    }.toMutableList()

                val title = if (document.select(".name").size > 0) {
                    document.select(".name").text()
                } else {
                    document.select(".subject").text()
                }.removePrefix("&nbsp").trimStart()
                val courseName = document.select(".page-header-headings")
                    .text()
                    .replace("【.*】\\d*".toRegex(), "")
                    .replace(" .*".toRegex(), "")
                val content = document.select(".content").html()
                val date = df.parse(
                    document.select(".author")
                        .text()
                        .replace(", \\d+:\\d+.*".toRegex(), "")
                )
                AnnItem(E3Type.NEW, title, date, courseName, null, content, attachItems)
            }
        }
    }

    override fun getCookie(): MutableList<Cookie>? {
        return mutableListOf(sessionId!!)
    }

    override fun getCourseList(): Observable<MutableList<CourseItem>> {
        throw NotImplementedError()
    }

    override fun getCourseAnns(courseItem: CourseItem): Observable<MutableList<AnnItem>> {
        throw NotImplementedError()
    }

    override fun getCourseFolders(courseItem: CourseItem): Observable<FolderItem> {
        throw NotImplementedError()
    }
}