package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.model.CourseItem
import com.team214.nctue4.model.FileItem
import io.reactivex.Observable
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class OldE3Client(context: Context) : E3Client() {
    class SessionInvalidException : Exception()

    companion object {
        const val WEB_URL = "https://e3.nctu.edu.tw/NCTU_Easy_E3P/lms31"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var sessionId: Cookie? = null
    private var aspXAuth: Cookie? = null
    private var viewState: String = ""
    private var currentPage: String = "home"
    private var courseIdMap = HashMap<String, String>()

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

    private fun get(path: String): Observable<Response> {
        return Observable.fromCallable {
            if (path != "/login.aspx" && (sessionId == null || aspXAuth == null)) throw SessionInvalidException()
            Log.d("OldE3Get", path)
            val request = Request.Builder()
                .url(WEB_URL + path)
                .build()
            client.newCall(request).execute().apply {
                if (this.header("Location", "")!!.startsWith("/NCTU_EASY_E3P/LMS31/login.aspx")) {
                    throw SessionInvalidException()
                }
            }
        }.retryWhen {
            it.flatMap { error ->
                return@flatMap if (error is SessionInvalidException) login() else Observable.error(error)
            }
        }
    }


    private fun post(
        path: String,
        data: HashMap<String, String>
    ): Observable<Response> {
        return Observable.fromCallable {
            if (path != "/login.aspx" && (sessionId == null || aspXAuth == null)) throw SessionInvalidException()
            Log.d("OldE3Post", path)
            val formBodyBuilder = FormBody.Builder()
            data.forEach { entry -> formBodyBuilder.add(entry.key, entry.value) }
            formBodyBuilder.add("__VIEWSTATE", viewState)
            val formBody = formBodyBuilder.build()
            val request = okhttp3.Request.Builder()
                .url(WEB_URL + path)
                .post(formBody)
                .build()
            client.newCall(request).execute().apply {
                if (this.header("Location", "")!!.startsWith("/NCTU_EASY_E3P/LMS31/login.aspx")) {
                    throw SessionInvalidException()
                }
            }
        }.retryWhen {
            it.flatMap { error ->
                return@flatMap if (error is SessionInvalidException) login() else Observable.error(error)
            }
        }
    }

    private fun parseHtmlResponse(response: Response): Observable<Document> {
        return Observable.fromCallable {
            Jsoup.parse(response.body()!!.string()).also {
                viewState = it.select("#__VIEWSTATE").attr("value")
            }
        }
    }


    private fun toHomePage(): Observable<Document> {
        return get("/enter_course_index.aspx").flatMap { response ->
            currentPage = "home"
            Observable.just(response)
        }.flatMap { parseHtmlResponse(it) }
    }

    private fun toCoursePage(courseId: String): Observable<Unit> {
        return post(
            "/enter_course_index.aspx",
            hashMapOf("__EVENTTARGET" to courseIdMap[courseId]!!)
        ).flatMap { Observable.just(Unit) }
    }

    private fun ensureCourseIdMap(): Observable<Unit> {
        return if (courseIdMap.isEmpty()) {
            toHomePage().flatMap { Observable.fromCallable { buildCourseIdMap(it) } }
        } else {
            Observable.just(Unit)
        }
    }

    private fun buildCourseIdMap(document: Document) {
        if (!courseIdMap.isEmpty()) return
        val courseEls = document
            .getElementById("ctl00_ContentPlaceHolder1_gvCourse")
            .getElementsByTag("a")
        courseEls.forEach {
            courseIdMap[it.attr("courseid")] =
                    it.attr("id").replace('_', '$')
        }
        Log.d("E3Map", courseIdMap.toString())
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
                    currentPage = "home"
                }
            }
    }

    override fun getFrontPageAnn(): Observable<MutableList<AnnItem>> {
        return toHomePage().flatMap { document ->
            Observable.fromCallable {
                buildCourseIdMap(document)
                val annItems = mutableListOf<AnnItem>()
                val dateCourseElId = "ctl00_ContentPlaceHolder1_rptNewList_ctl%02d_lbCourseNa"
                val titleElId = "ctl00_ContentPlaceHolder1_rptNewList_ctl%02d_lnkMore"
                val contentElId = "ctl00_ContentPlaceHolder1_rptNewList_ctl%02d_lbContent"
                val df = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                var idx = 0
                while (true) {
                    val dateCourse = document.getElementById(dateCourseElId.format(idx))?.text()
                    val titleEl = document.getElementById(titleElId.format(idx))
                    val content = document.getElementById(contentElId.format(idx))?.html()
                    if (dateCourse == null || titleEl == null || content == null) break
                    idx++
                    val courseName = dateCourse.split("【")[1].removeSuffix("】")
                    val date = df.parse(dateCourse.split("【")[0])
                    val title = titleEl.text()
                    val courseId = titleEl.attr("courseid")
                    annItems.add(AnnItem(E3Type.OLD, title, date, courseName, courseId))
                }
                annItems
            }
        }
    }

    override fun getAnn(annItem: AnnItem): Observable<AnnItem> {
        return ensureCourseIdMap()
            .flatMap { toCoursePage(annItem.detailLocationHint!!) }
            .flatMap { get("/stu_announcement_online.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                Observable.fromCallable {
                    val courseName = document.getElementById("ctl00_lbCurrentCourseName").text()
                    val targets = arrayOf("tpLatest_rpNew", "tpExpire_rptExpire")
                    val titleElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_lbCaption"
                    val contentElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_lbContent"
                    val fileElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl01_hlAttachDesFile"
                    for (target in targets) {
                        var idx = 0
                        while (true) {
                            val titleEl = document.getElementById(titleElId.format(target, idx))
                            titleEl ?: break
                            if (titleEl.text() != annItem.title) {
                                idx++
                                continue
                            }
                            val fileItems = mutableListOf<FileItem>()
                            val fileEl = document.getElementById(fileElId.format(target, idx))
                            if (fileEl != null) {
                                val fileName = fileEl.text()
                                val fileUrl = WEB_URL + fileEl.attr("href")
                                    .replace(
                                        "common_get_content_media_attach_file.ashx",
                                        "/common_view_standalone_file.ashx"
                                    )
                                Log.d("E3file", fileUrl)
                                fileItems.add(FileItem(fileName, fileUrl))
                            }
                            val content = document.getElementById(contentElId.format(target, idx)).html()
                            return@fromCallable AnnItem(
                                E3Type.OLD,
                                titleEl.text(),
                                annItem.date,
                                courseName,
                                null,
                                content,
                                fileItems
                            )
                        }
                    }
                    throw ServiceErrorException()
                }
            }
    }

    override fun getCourseList(): Observable<MutableList<CourseItem>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCookie(): MutableList<Cookie>? {
        return null
    }

}