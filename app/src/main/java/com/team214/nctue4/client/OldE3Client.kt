package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.team214.nctue4.model.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.*
import org.json.JSONObject
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

    override var client = OkHttpClient().newBuilder()
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
        .build()!!

    private fun get(path: String): Observable<String> {
        return Observable.fromCallable {
            if (path != "/login.aspx" && (sessionId == null || aspXAuth == null)) {
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
                    if (this.header("Location", "")!!.startsWith("/NCTU_EASY_E3P/LMS31/login.aspx")) {
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


    private fun post(
        path: String,
        data: HashMap<String, String>,
        headers: HashMap<String, String>? = null
    ): Observable<String> {
        return Observable.fromCallable {
            if (path != "/login.aspx" && (sessionId == null || aspXAuth == null)) {
                throw SessionInvalidException()
            }
            Log.d("OldE3Post", path)
            val formBodyBuilder = FormBody.Builder()
            data.forEach { entry -> formBodyBuilder.add(entry.key, entry.value) }
            formBodyBuilder.add("__VIEWSTATE", viewState)
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
                    if (this.header("Location", "")!!.startsWith("/NCTU_EASY_E3P/LMS31/login.aspx")) {
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

    private fun parseHtmlResponse(response: String): Observable<Document> {
        return Observable.fromCallable {
            Jsoup.parse(response).also {
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
        return if (currentPage == courseId) {
            Observable.just(Unit)
        } else {
            post(
                "/enter_course_index.aspx",
                hashMapOf("__EVENTTARGET" to courseIdMap[courseId]!!)
            ).flatMap {
                currentPage = courseId
                Observable.just(Unit)
            }
        }
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
                    if (it.contains("window.location.href='login.aspx';")) {
                        throw WrongCredentialsException()
                    }
                    currentPage = "home"
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
                .flatMap { toCoursePage(annItem.detailLocationHint) }
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
        return toHomePage().flatMap { document ->
            Observable.create<CourseItem> { emitter ->
                buildCourseIdMap(document)
                val courseEls = document
                    .getElementById("ctl00_ContentPlaceHolder1_gvCourse")
                    .getElementsByTag("a")
                courseEls.forEach {
                    val courseName = it.text()
                    val courseId = it.attr("courseid")
                    val additionalInfo = it.parent().parent()
                        .getElementsByTag("td")
                        .run { this[0].text() + this[1].text() }
                    emitter.onNext(CourseItem(E3Type.OLD, courseName, courseId, additionalInfo))
                }
                emitter.onComplete()
            }
        }
    }

    override fun getCourseAnns(courseItem: CourseItem): Observable<AnnItem> {
        return ensureCourseIdMap()
            .flatMap { toCoursePage(courseItem.courseId) }
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


    class FolderProperty(
        val folderType: FolderItem.Type,
        val tableElId: String,
        val eventTarget: String,
        val jsonKey: String,
        val dataNavigatorElId: String
    )

    private val folderProperties = arrayOf(
        FolderProperty(
            FolderItem.Type.Handout,
            "ctl00_ContentPlaceHolder1_dgCourseHandout",
            "ctl00\$ContentPlaceHolder1\$DataNavigator1\$ctl03",
            "ctl00\$ContentPlaceHolder1\$dgCourseHandout",
            "ctl00_ContentPlaceHolder1_DataNavigator1_ctl02"
        ),
        FolderProperty(
            FolderItem.Type.Reference,
            "ctl00_ContentPlaceHolder1_dgCourseReference",
            "ctl00\$ContentPlaceHolder1\$DataNavigator3\$ctl03",
            "ctl00\$ContentPlaceHolder1\$dgCourseReference",
            "ctl00_ContentPlaceHolder1_DataNavigator3_ctl02"
        )
    )

    override fun getCourseFolders(courseItem: CourseItem): Observable<FolderItem> {
        return ensureCourseIdMap()
            .flatMap { toCoursePage(courseItem.courseId) }
            .flatMap { get("/stu_materials_document_list.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                getCourseFolderByProperty(document, folderProperties[0]).mergeWith(
                    getCourseFolderByProperty(
                        document,
                        folderProperties[1]
                    )
                )
            }
    }

    private fun getCourseFolderByProperty(document: Document, folderProperty: FolderProperty): Observable<FolderItem> {
        return Observable.create<FolderItem> { emitter ->
            val dataNavigatorText = document
                .getElementById(folderProperty.dataNavigatorElId)
                .text()
            val totalPage = Regex("共 ([\\d]*) 頁")
                .find(dataNavigatorText)
                ?.groups?.get(1)?.value?.toInt()
            if (totalPage != null) {
                parseMaterialFolderTable(document, folderProperty, emitter)
                Observable.concatArray(
                    *(MutableList(totalPage - 1) { getRemainingFolder(folderProperty) }.toTypedArray())
                ).subscribeBy(
                    onNext = { emitter.onNext(it) },
                    onComplete = { emitter.onComplete() }
                )
            } else {
                emitter.onComplete()
            }
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
                parseMaterialFolderTable(document, folderProperty, emitter)
                emitter.onComplete()
            }
        }
    }

    private fun parseMaterialFolderTable(
        document: Document,
        folderProperty: FolderProperty,
        emitter: ObservableEmitter<FolderItem>
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
            emitter.onNext(FolderItem(name, folderId, courseId, folderProperty.folderType))
        }
    }

    override fun getFiles(folderItem: FolderItem): Observable<FileItem> {
        return ensureCourseIdMap()
            .flatMap { toCoursePage(folderItem.courseId) }
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
                            val url =
                                "$WEB_URL/common_view_standalone_file.ashx?AttachMediaId=${match!!.groups[1]!!.value}&CourseId=${match.groups[2]!!.value}"
                            emitter.onNext(FileItem(name, url))
                        }
                    emitter.onComplete()
                }
            }
    }

    override fun getScore(courseItem: CourseItem): Observable<ScoreItem> {
        return ensureCourseIdMap()
            .flatMap { toCoursePage(courseItem.courseId) }
            .flatMap { get("/stu_scores_list.aspx") }
            .flatMap { parseHtmlResponse(it) }
            .flatMap { document ->
                Observable.create<ScoreItem> { emitter ->
                    val trEls = document.selectFirst("#ctl00_tdContent > table > tbody")
                        .getElementsByTag("table")
                        .first()
                        .getElementsByTag("tr")
                        .drop(1)
                    trEls.subList(0, trEls.size - 2)
                        .forEach { el ->
                            val tdEls = el.getElementsByTag("td")
                            if (tdEls.size == 6) {
                                if (tdEls[1].text() != "-") {
                                    emitter.onNext(ScoreItem(tdEls[1].text(), tdEls[4].text()))
                                } else {
                                    emitter.onNext(ScoreItem(tdEls[0].text(), tdEls[4].text()))
                                }
                            } else if (tdEls.size == 5) {
                                emitter.onNext(ScoreItem(tdEls[0].text(), tdEls[4].text()))
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
            .flatMap { toCoursePage(courseItem.courseId) }
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
            .flatMap { toCoursePage(courseItem.courseId) }
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
            .flatMap { toCoursePage(courseItem!!.courseId) }
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

}