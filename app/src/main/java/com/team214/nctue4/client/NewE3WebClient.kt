package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.team214.nctue4.MainApplication
import com.team214.nctue4.model.*
import io.reactivex.Observable
import okhttp3.*
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class NewE3WebClient(context: Context) : E3Client() {
    private val app = (context.applicationContext as MainApplication)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    class SessionInvalidException : Exception()

    override val client = OkHttpClient().newBuilder()
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): MutableList<Cookie>? {
                val cookies = mutableListOf<Cookie>()
                if (app.newE3Session != null) cookies.add(app.newE3Session!!)
                return cookies
            }

            override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
                for (cookie in cookies) {
                    when (cookie.name()) {
                        "MoodleSession" -> app.newE3Session = cookie
                    }
                }
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .build()!!

    override fun login(studentId: String?, password: String?): Observable<Unit> {
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
        return clientExecute(request).flatMap { (response, _) ->
            Observable.fromCallable {
                response.apply {
                    if (this.header("Location") == "https://e3new.nctu.edu.tw/login/index.php") {
                        throw WrongCredentialsException()
                    }
                }
                Unit
            }
        }
    }

    fun get(path: String): Observable<String> {
        return Observable.fromCallable {
            if (app.newE3Session == null) throw SessionInvalidException()
            Log.d("NewE3WebGet", path)
            okhttp3.Request
                .Builder()
                .url(path)
                .get()
                .build()
        }.flatMap { request ->
            clientExecute(request)
        }.flatMap { (response, responseBody) ->
            Observable.fromCallable {
                response.apply {
                    if (this.header("Location") == "https://e3new.nctu.edu.tw/login/index.php") {
                        throw SessionInvalidException()
                    }
                }
                responseBody
            }
        }.retryWhen {
            it.flatMap { error ->
                if (error is SessionInvalidException) login() else Observable.error(error)
            }
        }
    }

    override fun getFrontPageAnns(): Observable<AnnItem> {
        return get("https://e3new.nctu.edu.tw/theme/dcpc/news/index.php?lang=en").flatMap {
            Observable.create<AnnItem> { emitter ->
                val document = Jsoup.parse(it).apply {
                    if (this.selectFirst(".login > a")?.text() == "Log in" ||
                        this.selectFirst("body > div")?.text() == "Invalid user"
                    ) {
                        throw SessionInvalidException()
                    }
                }
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
                            if (curMonth >= 7 && date.month <= 1) curYear + 1
                            else if (curMonth <= 1 && date.month >= 7) curYear - 1
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
                    emitter.onNext(AnnItem(E3Type.NEW, title, date, courseName, "$detailLocationHint&lang=en"))
                }
                emitter.onComplete()
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
                val document = Jsoup.parse(it)
                val df = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US)

                val attachItems = mutableListOf<FileItem>()

                document.getElementsByClass("attachments")
                    ?.first()
                    ?.getElementsByTag("a")
                    ?.filter { el -> el.hasText() }
                    ?.forEach { el ->
                        attachItems.add(
                            FileItem(el.text(), el.attr("href"))
                        )
                    }

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
        return mutableListOf(app.newE3Session!!)
    }

    override fun getCourseList(): Observable<CourseItem> {
        throw NotImplementedError()
    }

    override fun getCourseAnns(courseItem: CourseItem): Observable<AnnItem> {
        throw NotImplementedError()
    }

    override fun getCourseFolders(courseItem: CourseItem, folderType: FolderItem.Type): Observable<FolderItem> {
        throw NotImplementedError()
    }

    override fun getFiles(folderItem: FolderItem): Observable<FileItem> {
        throw NotImplementedError()
    }

    override fun getScore(courseItem: CourseItem): Observable<ScoreItem> {
        throw NotImplementedError()
    }

    override fun getMembers(courseItem: CourseItem): Observable<MemberItem> {
        throw NotImplementedError()
    }

    override fun getCourseHwk(courseItem: CourseItem): Observable<HwkItem> {
        throw NotImplementedError()
    }

    override fun getHwkSubmitFiles(hwkItem: HwkItem): Observable<FileItem> {
        throw NotImplementedError()
    }

    override fun getHwkDetail(hwkItem: HwkItem, courseItem: CourseItem?): Observable<HwkItem> {
        throw NotImplementedError()
    }

    override fun getBaseUrl(): String? {
        return "https://e3new.nctu.edu.tw"
    }

    override fun prepareCourseFolders(courseItem: CourseItem): Observable<Unit> {
        throw NotImplementedError()
    }
}