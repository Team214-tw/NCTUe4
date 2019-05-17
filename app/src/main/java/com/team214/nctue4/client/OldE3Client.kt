package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.team214.nctue4.model.*
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

class OldE3Client(context: Context) : E3Client() {
    class SessionInvalidException : Exception()

    companion object {
        const val WEB_URL = "https://e3.nctu.edu.tw/NCTU_Easy_E3P/lms31"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var oldE3Session: Cookie? = null
    private var oldE3AspXAuth: Cookie? = null
    private var oldE3ViewState: String = ""
    private var oldE3CurrentPage: String = "notLoggedIn"
    private val oldE3CourseIdMap = HashMap<String, String>()

    override var client = OkHttpClient().newBuilder()
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): MutableList<Cookie>? {
                val cookies = mutableListOf<Cookie>()
                if (oldE3Session != null) cookies.add(oldE3Session!!)
                if (oldE3AspXAuth != null) cookies.add(oldE3AspXAuth!!)
                return cookies
            }

            override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
                for (cookie in cookies) {
                    when (cookie.name()) {
                        "ASP.NET_SessionId" -> oldE3Session = cookie
                        ".ASPXAUTH_NCTU_EASY_E3P_140.113.8.80" -> oldE3AspXAuth = cookie
                    }
                }
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .build()!!

    private fun get(path: String): Observable<String> {
        return Observable.fromCallable {
            if (path != "/login.aspx" && (oldE3Session == null || oldE3AspXAuth == null)) {
                throw SessionInvalidException()
            }
            Log.d("OldE3Get", path)
            Request.Builder()
                .url(WEB_URL + path)
                .build()
        }.flatMap {
            clientExecute(it)
        }.flatMap { (response, responseBody) ->
            Observable.fromCallable {
                response.apply {
                    if (this.header("Location", "")!!.contains(Regex("(login)|(logout)"))) {
                        throw SessionInvalidException()
                    }
                }
                responseBody
            }
        }.retryWhen {
            it.flatMap { error ->
                if (error is SessionInvalidException) {
                    login().flatMap { restorePage() }
                } else {
                    Observable.error(error)
                }
            }
        }
    }


    private fun post(
        path: String,
        data: HashMap<String, String>,
        headers: HashMap<String, String>? = null
    ): Observable<String> {
        return Observable.fromCallable {
            if (path != "/login.aspx" && (oldE3Session == null || oldE3AspXAuth == null)) {
                throw SessionInvalidException()
            }
            Log.d("OldE3Post", path)
            val formBodyBuilder = FormBody.Builder()
            data.forEach { entry -> formBodyBuilder.add(entry.key, entry.value) }
            formBodyBuilder.add("__VIEWSTATE", oldE3ViewState)
            val formBody = formBodyBuilder.build()
            val requestBuilder = okhttp3.Request.Builder()
                .url(WEB_URL + path)
                .post(formBody)
            headers?.forEach { entry -> requestBuilder.addHeader(entry.key, entry.value) }
            requestBuilder.build()
        }.flatMap {
            clientExecute(it)
        }.flatMap { (response, responseBody) ->
            Observable.fromCallable {
                response.apply {
                    if (this.header("Location", "")!!.contains(Regex("(login)|(logout)"))) {
                        throw SessionInvalidException()
                    }
                }
                responseBody
            }
        }.retryWhen {
            it.flatMap { error ->
                if (error is SessionInvalidException) {
                    login().flatMap { restorePage() }
                } else {
                    Observable.error(error)
                }
            }
        }
    }

    private fun parseHtmlResponse(response: String): Observable<Document> {
        return Observable.fromCallable {
            Jsoup.parse(response).also {
                oldE3ViewState = it.select("#__VIEWSTATE").attr("value")
            }
        }
    }

    private fun restorePage(): Observable<Unit> {
        return when (oldE3CurrentPage) {
            "notLoggedIn" -> {
                Observable.just(Unit)
            }
            "home" -> {
                toHomePage().flatMap { Observable.just(Unit) }
            }
            else -> {
                val bkPage = oldE3CurrentPage
                toHomePage().flatMap { toCoursePage(bkPage) }
            }
        }
    }

    private fun toCoursePage(courseId: String): Observable<Unit> {
        if (courseId in oldE3CourseIdMap) {
            return post(
                "/enter_course_index.aspx",
                hashMapOf("__EVENTTARGET" to oldE3CourseIdMap[courseId]!!)
            ).flatMap {
                oldE3CurrentPage = courseId
                Observable.just(Unit)
            }
        }
        return toCourseHistoryPage()
            .flatMap { document ->
                val dataNavigatorText = document
                    .getElementById("ctl00_ContentPlaceHolder1_DataNavigator1_ctl02")
                    .text()
                val totalPage = Regex("共 ([\\d]*) 頁")
                    .find(dataNavigatorText)
                    ?.groups?.get(1)?.value?.toInt()
                val courseTrs = document.getElementById("ctl00_ContentPlaceHolder1_dg")
                    ?.getElementsByTag("tr")
                    ?.drop(1)
                courseTrs?.forEach { tr ->
                    val a = tr.getElementsByTag("td")[6].getElementsByTag("a").firstOrNull()
                    if (a != null && a.attr("courseid") == courseId) {
                        return@flatMap post(
                            "/enter_course_history.aspx",
                            hashMapOf("__EVENTTARGET" to a.id().replace("_", "$"))
                        ).flatMap {
                            Observable.just(Unit)
                        }
                    }
                }

                return@flatMap Observable.concatArray(
                    *(MutableList(totalPage!! - 1) { getRemainingCourseList1() }.toTypedArray())
                ).filter { it.first == courseId }
                    .take(1)
                    .flatMap {
                        Log.d("course", it.toString())
                        post(
                            "/enter_course_history.aspx",
                            hashMapOf("__EVENTTARGET" to it.second)
                        )
                    }.flatMap {
                        Observable.just(Unit)
                    }
            }
    }


    private fun getRemainingCourseList1(): Observable<Pair<String, String>> {
        return post(
            "/enter_course_history.aspx?Anthem_CallBack=true", hashMapOf(
                "Anthem_UpdatePage" to "true",
                "__EVENTTARGET" to "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03",
                "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03.x" to "0",
                "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03.y" to "0"
            )
        ).flatMap { response ->
            Observable.create<Pair<String, String>> { emitter ->
                val documentStr = JSONObject(response)
                    .getJSONObject("controls")
                    .getString("ctl00\$ContentPlaceHolder1\$dg")
                val document = Jsoup.parse(documentStr)
                parseCourseTable1(document, emitter)
                emitter.onComplete()
            }
        }
    }

    private fun parseCourseTable1(document: Document, emitter: Emitter<Pair<String, String>>) {
        val courseTrs = document.getElementById("ctl00_ContentPlaceHolder1_dg")
            ?.getElementsByTag("tr")
            ?.drop(1)
        courseTrs?.forEach {
            val tds = it.getElementsByTag("td")
            if (tds[6].getElementsByTag("a").isEmpty()) return@forEach
            val a = tds[6].getElementsByTag("a").first()
            val courseId = a.attr("courseid")
            val id = a.id()
            emitter.onNext(Pair(courseId, id.replace("_", "$")))
        }
    }

    private fun toHomePage(): Observable<Document> {
        return get("/enter_course_index.aspx").flatMap { response ->
            oldE3CurrentPage = "home"
            Observable.just(response)
        }.flatMap { parseHtmlResponse(it) }
    }

    private fun toCourseHistoryPage(): Observable<Document> {
        return get("/enter_course_history.aspx").flatMap { response ->
            oldE3CurrentPage = "home"
            Observable.just(response)
        }.flatMap { parseHtmlResponse(it) }
    }

    private fun ensureCoursePage(courseId: String): Observable<Unit> {
        return when {
            oldE3CurrentPage == courseId -> Observable.just(Unit)
            oldE3CurrentPage == "home" -> toCoursePage(courseId)
            else -> toHomePage().flatMap { toCoursePage(courseId) }
        }
    }

    private fun ensureCourseIdMap(): Observable<Unit> {
        return if (oldE3CourseIdMap.isEmpty()) {
            toHomePage().flatMap { Observable.fromCallable { buildCourseIdMap(it) } }
        } else {
            Observable.just(Unit)
        }
    }

    private fun buildCourseIdMap(document: Document) {
        if (!oldE3CourseIdMap.isEmpty()) return
        val courseEls = document
            .getElementById("ctl00_ContentPlaceHolder1_gvCourse")
            ?.getElementsByTag("a")
        courseEls?.forEach {
            oldE3CourseIdMap[it.attr("courseid")] =
                it.attr("id").replace('_', '$')
        }
    }

    override fun login(
        studentId: String?,
        password: String?
    ): Observable<Unit> {
        oldE3Session = null
        oldE3AspXAuth = null
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
                    if (it.contains("window.location.href='login.aspx';")) {
                        throw WrongCredentialsException()
                    }
                }
            }
    }

    override fun getFrontPageAnns(): Observable<AnnItem> {
        return toHomePage().flatMap { document ->
            Observable.create<AnnItem> { emitter ->
                buildCourseIdMap(document)
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
                    emitter.onNext(AnnItem(E3Type.OLD, title, date, courseName, courseId))
                }
                emitter.onComplete()
            }
        }
    }

    override fun getAnn(annItem: AnnItem): Observable<AnnItem> {
        return if (annItem.detailLocationHint == null) {
            Observable.just(annItem)
        } else {
            ensureCourseIdMap()
                .flatMap { ensureCoursePage(annItem.detailLocationHint!!) }
                .flatMap { get("/stu_announcement_online.aspx") }
                .flatMap { parseHtmlResponse(it) }
                .flatMap { document ->
                    Observable.fromCallable {
                        val courseName = document.getElementById("ctl00_lbCurrentCourseName").text()
                        val targets = arrayOf("tpLatest_rpNew", "tpExpire_rptExpire")
                        val titleElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_lbCaption"
                        val contentElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_lbContent"
                        val fileElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_hlAttachDesFile"
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
    }

    override fun getCourseList(): Observable<CourseItem> {
        return toCourseHistoryPage().flatMap { document ->
            Observable.create<CourseItem> { emitter ->
                val dataNavigatorText = document
                    .getElementById("ctl00_ContentPlaceHolder1_DataNavigator1_ctl02")
                    .text()
                val totalPage = Regex("共 ([\\d]*) 頁")
                    .find(dataNavigatorText)
                    ?.groups?.get(1)?.value?.toInt()
                if (totalPage != null) {
                    parseCourseTable(document, emitter)
                    Observable.concatArray(
                        *(MutableList(totalPage - 1) { getRemainingCourseList() }.toTypedArray())
                    ).subscribeBy(
                        onNext = { emitter.onNext(it) },
                        onComplete = { emitter.onComplete() }
                    )
                } else {
                    emitter.onComplete()
                }
            }
        }
    }

    private fun getRemainingCourseList(): Observable<CourseItem> {
        return post(
            "/enter_course_history.aspx?Anthem_CallBack=true", hashMapOf(
                "Anthem_UpdatePage" to "true",
                "__EVENTTARGET" to "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03",
                "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03.x" to "0",
                "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03.y" to "0"
            )
        ).flatMap { response ->
            Observable.create<CourseItem> { emitter ->
                val documentStr = JSONObject(response)
                    .getJSONObject("controls")
                    .getString("ctl00\$ContentPlaceHolder1\$dg")
                val document = Jsoup.parse(documentStr)
                parseCourseTable(document, emitter)
                emitter.onComplete()
            }
        }
    }

    private fun parseCourseTable(document: Document, emitter: Emitter<CourseItem>) {
        val courseTrs = document.getElementById("ctl00_ContentPlaceHolder1_dg")
            ?.getElementsByTag("tr")
            ?.drop(1)
        courseTrs?.forEach {
            val tds = it.getElementsByTag("td")
            if (tds[6].getElementsByTag("a").isEmpty()) return@forEach
            val courseName = tds[6].text().split(" 進入課程").first()
            val courseId = tds[6].getElementsByTag("a").first().attr("courseid")
            val sortKey = (tds[0].text() +
                    when (tds[1].text()) {
                        "上學期" -> "0"
                        "下學期" -> "1"
                        "暑修" -> "2"
                        else -> "3"
                    }).toLong()
            val additionalInfo = tds[0].text() + " " + tds[1].text()
            emitter.onNext(CourseItem(E3Type.OLD, courseName, courseId, additionalInfo, sortKey))
        }
    }

    override fun getCourseAnns(courseItem: CourseItem): Observable<AnnItem> {
        return ensureCourseIdMap()
            .flatMap { ensureCoursePage(courseItem.courseId) }
            .flatMap { get("/stu_announcement_online.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                Observable.create<AnnItem> { emitter ->
                    val courseName = document.getElementById("ctl00_lbCurrentCourseName").text()
                    val targets = arrayOf("tpLatest_rpNew", "tpExpire_rptExpire")
                    val titleElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_lbCaption"
                    val contentElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_lbContent"
                    val fileElId = "ctl00_ContentPlaceHolder1_tabAnnouncement_%s_ctl%02d_hlAttachDesFile"
                    for (target in targets) {
                        var idx = 0
                        while (true) {
                            val titleEl = document.getElementById(titleElId.format(target, idx))
                            titleEl ?: break
                            val fileItems = mutableListOf<FileItem>()
                            val fileEl = document.getElementById(fileElId.format(target, idx))
                            if (fileEl != null) {
                                val fileName = fileEl.text()
                                val fileUrl = WEB_URL + fileEl.attr("href")
                                    .replace(
                                        "common_get_content_media_attach_file.ashx",
                                        "/common_view_standalone_file.ashx"
                                    )
                                fileItems.add(FileItem(fileName, fileUrl))
                            }
                            val content = document.getElementById(contentElId.format(target, idx)).html()
                            emitter.onNext(
                                AnnItem(
                                    E3Type.OLD,
                                    titleEl.text(),
                                    null,
                                    courseName,
                                    null,
                                    content,
                                    fileItems
                                )
                            )
                            idx++
                        }
                    }
                    emitter.onComplete()
                }
            }
    }


    data class FolderProperty(
        val folderType: FolderItem.Type,
        val tableElId: String,
        val eventTarget: String,
        val jsonKey: String,
        val dataNavigatorElId: String,
        val index: Int
    )

    private val folderProperties = arrayOf(
        FolderProperty(
            FolderItem.Type.Handout,
            "ctl00_ContentPlaceHolder1_dgCourseHandout",
            "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03",
            "ctl00\$ContentPlaceHolder1\$dgCourseHandout",
            "ctl00_ContentPlaceHolder1_DataNavigator1_ctl02",
            0
        ),
        FolderProperty(
            FolderItem.Type.Reference,
            "ctl00_ContentPlaceHolder1_dgCourseReference",
            "ctl00\$ContentPlaceHolder1\$DataNavigator3\$ctl03",
            "ctl00\$ContentPlaceHolder1\$dgCourseReference",
            "ctl00_ContentPlaceHolder1_DataNavigator3_ctl02",
            1
        )
    )

    private var courseFolderLatches: List<CountDownLatch>? = null
    private var courseFolderCache: List<MutableList<FolderItem>>? = null
    private var courseFolderPrepared = false

    override fun prepareCourseFolders(courseItem: CourseItem): Observable<Unit> {
        courseFolderPrepared = true
        courseFolderLatches = listOf(CountDownLatch(1), CountDownLatch(1))
        courseFolderCache = listOf(mutableListOf(), mutableListOf())
        return ensureCourseIdMap()
            .flatMap { ensureCoursePage(courseItem.courseId) }
            .flatMap { get("/stu_materials_document_list.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                getCourseFolderByProperty(document, folderProperties[0])
                getCourseFolderByProperty(document, folderProperties[1])
                Observable.just(Unit)
            }
    }

    override fun getCourseFolders(courseItem: CourseItem, folderType: FolderItem.Type): Observable<FolderItem> {
        if (!courseFolderPrepared) prepareCourseFolders(courseItem).subscribe()
        val folderTypeIdx = when (folderType) {
            FolderItem.Type.Handout -> 0
            FolderItem.Type.Reference -> 1
        }
        return Observable.create<FolderItem> { emitter ->
            try {
                courseFolderLatches!![folderTypeIdx].await()
                courseFolderCache!![folderTypeIdx].forEach { emitter.onNext(it) }
            } catch (e: InterruptedException) {
                if (!emitter.isDisposed) emitter.onError(e)
            } finally {
                emitter.onComplete()
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun getCourseFolderByProperty(document: Document, folderProperty: FolderProperty) {

        val dataNavigatorText = document
            .getElementById(folderProperty.dataNavigatorElId)
            .text()
        val totalPage = Regex("共 ([\\d]*) 頁")
            .find(dataNavigatorText)
            ?.groups?.get(1)?.value?.toInt()
        if (totalPage != null) {
            parseMaterialFolderTable(document, folderProperty)
            Observable.concatArray(
                *(MutableList(totalPage - 1) { getRemainingFolder(folderProperty) }.toTypedArray())
            ).subscribeBy(
                onNext = { courseFolderCache!![folderProperty.index].add(it) },
                onComplete = { courseFolderLatches!![folderProperty.index].countDown() }
            )
        } else {
            courseFolderLatches!![folderProperty.index].countDown()
        }

    }

    private fun getRemainingFolder(folderProperty: FolderProperty): Observable<FolderItem> {
        return post(
            "/stu_materials_document_list.aspx?Anthem_CallBack=true", hashMapOf(
                "Anthem_UpdatePage" to "true",
                "__EVENTTARGET" to folderProperty.eventTarget,
                "ctl00\$ContentPlaceHolder1\$EasyView" to "rdoView1",
                "${folderProperty.eventTarget}.x" to "0",
                "${folderProperty.eventTarget}.y" to "0"
            )
        ).flatMap { response ->
            Observable.create<FolderItem> { emitter ->
                val json = JSONObject(response)
                val document = Jsoup.parse(
                    json.getJSONObject("controls")
                        .getString(folderProperty.jsonKey)
                )
                parseMaterialFolderTable(document, folderProperty)
                emitter.onComplete()
            }
        }
    }

    private fun parseMaterialFolderTable(
        document: Document,
        folderProperty: FolderProperty
    ) {
        val tableEl = document.getElementById(folderProperty.tableElId)
        val trEls = tableEl.getElementsByTag("tr").drop(1)
        for (trEl in trEls) {
            val tdEls = trEl.getElementsByTag("td")
            val name = tdEls[0].text()
            val tdElOnclick = tdEls[3]
                .getElementsByTag("a")[0]
                .attr("onclick")
            val matched = Regex("ReferenceSourceId=([^;,']*).*CurrentCourseId=([^;,']*)").find(tdElOnclick)
            val folderId = matched!!.groups[1]!!.value
            val courseId = matched.groups[2]!!.value
            val df = SimpleDateFormat("yyyy/MM/dd", Locale.US)
            courseFolderCache!![folderProperty.index].add(
                FolderItem(name, folderId, courseId, folderProperty.folderType, df.parse(tdEls[2].text()))
            )
        }
    }

    override fun getFiles(folderItem: FolderItem): Observable<FileItem> {
        return ensureCourseIdMap()
            .flatMap { ensureCoursePage(folderItem.courseId) }
            .flatMap { get("/dialog_common_view_attach_media_list.aspx?ReferenceSourceId=${folderItem.folderId}&CurrentCourseId=${folderItem.courseId}") }
            .flatMap { response ->
                Observable.create<FileItem> { emitter ->
                    val document = Jsoup.parse(response)
                    document.getElementById("ctl00_ContentPlaceHolder1_fileList2_dgFileList")
                        .getElementsByTag("tr")
                        .drop(1).forEach { el ->
                            val tdEls = el.getElementsByTag("td")
                            val name = tdEls[1].text()
                            val match = Regex("AttachMediaId=([^;,']*).*CourseId=([^;,']*)")
                                .find(tdEls[5].selectFirst("a").attr("onclick"))
                            if (match == null) {
                                emitter.onError(SessionInvalidException())
                                emitter.onComplete()
                                return@create
                            }
                            val url =
                                "$WEB_URL/common_view_standalone_file.ashx?AttachMediaId=${match.groups[1]!!.value}&CourseId=${match.groups[2]!!.value}"
                            emitter.onNext(FileItem(name, url))
                        }
                    emitter.onComplete()
                }
            }.retryWhen {
                it.flatMap { error ->
                    if (error is SessionInvalidException) {
                        login().flatMap { restorePage() }
                    } else {
                        Observable.error(error)
                    }
                }
            }
    }

    override fun getScore(courseItem: CourseItem): Observable<ScoreItem> {
        return ensureCourseIdMap()
            .flatMap { ensureCoursePage(courseItem.courseId) }
            .flatMap { get("/stu_scores_list.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                Observable.create<ScoreItem> { emitter ->
                    val trEls = document.selectFirst("#ctl00_tdContent > table > tbody")
                        .getElementsByTag("table")
                        .first()
                        .getElementsByTag("tr")
                        .drop(1)
                    trEls.take(trEls.size - 1)
                        .forEach { el ->
                            val tdEls = el.getElementsByTag("td")
                            if (tdEls.size == 6) {
                                if (tdEls[1].text() != "-") {
                                    emitter.onNext(ScoreItem(tdEls[1].text(), tdEls[4].text()))
                                } else {
                                    emitter.onNext(ScoreItem(tdEls[0].text(), tdEls[4].text()))
                                }
                            } else if (tdEls.size == 5) {
                                emitter.onNext(ScoreItem(tdEls[0].text(), tdEls[3].text()))
                            }
                        }
                    val tdEls = trEls.last().getElementsByTag("td")
                    emitter.onNext(ScoreItem(tdEls[1].text(), tdEls[2].text()))
                    emitter.onComplete()
                }
            }
    }

    override fun getMembers(courseItem: CourseItem): Observable<MemberItem> {
        return ensureCourseIdMap()
            .flatMap { ensureCoursePage(courseItem.courseId) }
            .flatMap { get("/stu_classintro_teacher.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                Observable.create<MemberItem> { emitter ->
                    document.getElementById("ctl00_ContentPlaceHolder1_tabMember_tpTeacher_dg")
                        ?.getElementsByTag("tr")
                        ?.drop(1)
                        ?.forEach { el ->
                            val tdEls = el.getElementsByTag("td")
                            emitter.onNext(
                                MemberItem(
                                    tdEls[0].text(),
                                    "",
                                    "",
                                    if (tdEls[1].text().contains("教師")) MemberItem.Type.Teacher else MemberItem.Type.TA
                                )
                            )
                        }
                    document.getElementById("ctl00_ContentPlaceHolder1_tabMember_tpStudent_dg3")
                        ?.getElementsByTag("tr")
                        ?.drop(1)
                        ?.forEach { el ->
                            val tdEls = el.getElementsByTag("td")
                            emitter.onNext(
                                MemberItem(
                                    tdEls[0].text(),
                                    tdEls[1].text(),
                                    tdEls[4].text(),
                                    MemberItem.Type.Student
                                )
                            )
                        }
                    document.getElementById("ctl00_ContentPlaceHolder1_tabMember_tpAuditor_dg2")
                        ?.getElementsByTag("tr")
                        ?.drop(1)
                        ?.forEach { el ->
                            val tdEls = el.getElementsByTag("td")
                            emitter.onNext(
                                MemberItem(
                                    tdEls[0].text(),
                                    tdEls[1].text(),
                                    tdEls[4].text(),
                                    MemberItem.Type.Audit
                                )
                            )
                        }
                    emitter.onComplete()
                }
            }
    }

    override fun getCourseHwk(courseItem: CourseItem): Observable<HwkItem> {
        return ensureCourseIdMap()
            .flatMap { ensureCoursePage(courseItem.courseId) }
            .flatMap { get("/stu_materials_homework_list.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                Observable.create<HwkItem> { emitter ->
                    val df = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
                    listOf(
                        "ctl00_ContentPlaceHolder1_dgHandin",
                        "ctl00_ContentPlaceHolder1_dgJudge",
                        "ctl00_ContentPlaceHolder1_dgLate",
                        "ctl00_ContentPlaceHolder1_dgAlready"
                    ).forEach { id ->
                        document.getElementById(id)
                            ?.getElementsByTag("tr")
                            ?.drop(1)
                            ?.forEach { el ->
                                val tdEls = el.getElementsByTag("td")
                                val name = tdEls.first().text()
                                val hwkId = Regex("crsHwkId=([^;,']*)").find(
                                    tdEls.last()
                                        .getElementsByTag("a")
                                        .first()
                                        .attr("onclick")
                                )!!.groups[1]!!.value
                                val endDate = df.parse(tdEls.asReversed()[2].text())
                                val startDate = df.parse(tdEls.asReversed()[3].text())
                                emitter.onNext(HwkItem(E3Type.OLD, name, hwkId, startDate, endDate, null))
                            }
                    }
                    emitter.onComplete()
                }
            }
    }

    override fun getHwkDetail(hwkItem: HwkItem, courseItem: CourseItem?): Observable<HwkItem> {
        return ensureCourseIdMap()
            .flatMap { ensureCoursePage(courseItem!!.courseId) }
            .flatMap {
                post(
                    "/ajaxpro/WebPageBase,App_Code.ashx",
                    hashMapOf(),
                    hashMapOf("X-AjaxPro-Method" to "setToken")
                )
            }
            .flatMap {
                val token = JSONObject(it).getString("value")
                get("/dialog_stu_homework_view.aspx?crsHwkId=${hwkItem.hwkId}&TokenId=$token")
            }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                Observable.fromCallable {
                    val content = document.getElementById("ctl00_ContentPlaceHolder1_HwkInfo1_lbContent").html()
                    val attachItems = mutableListOf<FileItem>()
                    val submitItems = mutableListOf<FileItem>()
                    document.getElementById("Anthem_ctl00_ContentPlaceHolder1_HwkInfo1_fileAttachManageLite_rpFileList__")
                        ?.getElementsByTag("a")
                        ?.forEach { el ->
                            attachItems.add(
                                FileItem(
                                    el.text(),
                                    WEB_URL + el.attr("href").replace(
                                        "common_get_content_media_attach_file.ashx",
                                        "/common_view_standalone_file.ashx"
                                    )
                                )
                            )
                        }
                    document.getElementById("Anthem_ctl00_ContentPlaceHolder1_fileAttachManageLite_rpFileList__")
                        ?.getElementsByTag("a")
                        ?.forEach { el ->
                            submitItems.add(
                                FileItem(
                                    el.text(),
                                    WEB_URL + el.attr("href").replace(
                                        "common_get_content_media_attach_file.ashx",
                                        "/common_view_standalone_file.ashx"
                                    )
                                )
                            )
                        }
                    HwkItem(
                        E3Type.OLD,
                        hwkItem.name,
                        hwkItem.hwkId,
                        hwkItem.startDate,
                        hwkItem.endDate,
                        content,
                        attachItems,
                        submitItems
                    )
                }
            }
    }

    override fun getHwkSubmitFiles(hwkItem: HwkItem): Observable<FileItem> {
        return Observable.fromIterable(hwkItem.submitItems)
    }

    override fun getCookie(): MutableList<Cookie>? {
        return null
    }

    override fun getBaseUrl(): String? {
        return "https://e3.nctu.edu.tw"
    }

}
