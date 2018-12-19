package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.team214.nctue4.model.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CountDownLatch

class NewE3ApiClient(context: Context) : E3Client() {
    class TokenInvalidException : Exception()

    companion object {
        const val API_URL = "https://e3new.nctu.edu.tw/webservice/rest/server.php?moodlewsrestformat=json"
    }

    override val client = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()!!
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var token = prefs.getString("newE3Token", null)
    private var userId = prefs.getString("newE3UserId", "")

    private fun post(
        data: HashMap<String, String>
    ): Observable<String> {
        if (token == null) throw TokenInvalidException()
        data["wstoken"] = token
        val formBodyBuilder = FormBody.Builder()
        data.forEach { entry -> formBodyBuilder.add(entry.key, entry.value) }
        val formBody = formBodyBuilder.build()
        val request = okhttp3.Request.Builder().url(API_URL).post(formBody).build()
        Log.d("NewE3ApiPost", data["wsfunction"] ?: "")
        return clientExecute(request).flatMap { (_, response) ->
            Observable.fromCallable {
                response.apply {
                    try {
                        val resJson = JSONObject(this)
                        if (resJson.has("errorcode") &&
                            resJson.getString("errorcode") == "invalidtoken"
                        ) {
                            throw TokenInvalidException()
                        }
                    } catch (e: JSONException) {
                        // Response is a JsonArray, Pass
                    }
                }
            }
        }.retryWhen {
            it.flatMap { error ->
                if (error is TokenInvalidException) login() else Observable.error(error)
            }
        }
    }

    override fun login(studentId: String?, password: String?): Observable<Unit> {

        val formBody = FormBody.Builder()
            .add("username", (studentId ?: prefs.getString("studentId", "")!!))
            .add("password", (password ?: prefs.getString("studentPortalPassword", "")!!))
            .add("service", "moodle_mobile_app")
            .build()
        val request = Request.Builder()
            .url("https://e3new.nctu.edu.tw/login/token.php")
            .post(formBody)
            .build()
        return clientExecute(request).flatMap { (_, response) ->
            Observable.fromCallable {
                val resJson = JSONObject(response)
                if (resJson.has("token")) {
                    token = resJson.getString("token")
                    prefs.edit().putString("newE3Token", token).apply()
                } else {
                    throw WrongCredentialsException()
                }
            }
        }
    }

    fun saveUserInfo(studentId: String): Observable<Unit> {
        return post(
            hashMapOf(
                "wsfunction" to "core_user_get_users_by_field",
                "values[0]" to studentId,
                "field" to "username"
            )
        ).flatMap {
            val resJson = JSONArray(it).getJSONObject(0)
            val name = resJson.getString("fullname")
            val email = resJson.getString("email")
            userId = resJson.getString("id")
            prefs.edit().putString("newE3UserId", userId).apply()
            prefs.edit().putString("studentEmail", email).apply()
            prefs.edit().putString("studentName", name).apply()
            Observable.just(Unit)
        }
    }

    override fun getCourseList(): Observable<CourseItem> {
        return post(
            hashMapOf(
                "wsfunction" to "core_enrol_get_users_courses",
                "userid" to userId
            )
        ).flatMap { response ->
            Observable.create<CourseItem> { emitter ->
                val resJson = JSONArray(response)
                (0 until resJson.length())
                    .map { resJson.get(it) as JSONObject }
                    .filter { it.getLong("enddate") > System.currentTimeMillis() / 1000 }
                    .forEach {
                        val courseName = it.getString("fullname").split(".").run {
                            if (this.size >= 3) this[2].split(" ").first() else this[0]
                        }
                        val courseId = it.getString("id")
                        val additionalInfo = it.getString("shortname")
                        emitter.onNext(CourseItem(E3Type.NEW, courseName, courseId, additionalInfo))
                    }
                emitter.onComplete()
            }
        }
    }

    override fun getCourseAnns(courseItem: CourseItem): Observable<AnnItem> {
        return post(
            hashMapOf(
                "wsfunction" to "mod_forum_get_forums_by_courses",
                "courseids[0]" to courseItem.courseId
            )
        ).flatMap {
            post(
                hashMapOf(
                    "wsfunction" to "mod_forum_get_forum_discussions_paginated",
                    "forumid" to JSONArray(it).getJSONObject(0).getString("id"),
                    "sortdirection" to "DESC",
                    "perpage" to "100",
                    "sortby" to "timemodified"
                )
            )
        }.flatMap { response ->
            Observable.create<AnnItem> { emitter ->
                val resJson = JSONObject(response).getJSONArray("discussions")
                (0 until resJson.length()).map { resJson.get(it) as JSONObject }.forEach { ann ->
                    val title = ann.getString("name")
                    var content = ann.getString("message")
                    val date = Date(ann.getLong("timemodified") * 1000)
                    if (ann.has("messageinlinefiles")) {
                        val files = ann.getJSONArray("messageinlinefiles")
                        (0 until files.length()).map { files.getJSONObject(it) }.forEach {
                            content = content.replace(
                                it.getString("fileurl"),
                                it.getString("fileurl") + "?token=$token"
                            )
                        }
                    }
                    val attachItems = mutableListOf<FileItem>()
                    if (ann.has("attachments")) {
                        val files = ann.getJSONArray("attachments")
                        (0 until files.length()).map { files.getJSONObject(it) }.forEach {
                            attachItems.add(
                                FileItem(
                                    it.getString("filename"),
                                    it.getString("fileurl") + "?token=$token"
                                )
                            )
                        }
                    }
                    emitter.onNext(
                        AnnItem(
                            E3Type.NEW,
                            title,
                            date,
                            courseItem.courseName,
                            null,
                            content,
                            attachItems
                        )
                    )
                }
                emitter.onComplete()
            }
        }
    }

    private var courseFolderLatch: CountDownLatch? = null
    private var courseFolderCache: List<MutableList<FolderItem>>? = null
    private var courseFolderPrepared = false

    override fun prepareCourseFolders(courseItem: CourseItem): Observable<Unit> {
        courseFolderPrepared = true
        courseFolderLatch = CountDownLatch(1)
        courseFolderCache = listOf(mutableListOf(), mutableListOf())
        return post(
            hashMapOf(
                "wsfunction" to "mod_folder_get_folders_by_courses",
                "courseids[0]" to courseItem.courseId
            )
        ).flatMap { response ->
            val resJson = JSONObject(response).getJSONArray("folders")
            (0 until resJson.length()).map { resJson.get(it) as JSONObject }.forEach {
                var name = it.getString("name")
                val folderType =
                    when {
                        name.startsWith("[參考資料]") -> {
                            name = name.removePrefix("[參考資料]")
                            FolderItem.Type.Reference
                        }
                        name.startsWith("[Reference]") -> {
                            name = name.removePrefix("[Reference]")
                            FolderItem.Type.Reference
                        }
                        else -> FolderItem.Type.Handout
                    }
                val folderItem = FolderItem(
                    name,
                    it.getString("coursemodule"),
                    courseItem.courseId,
                    folderType,
                    Date(it.getLong("timemodified") * 1000)
                )
                when (folderType) {
                    FolderItem.Type.Handout -> courseFolderCache!![0].add(folderItem)
                    FolderItem.Type.Reference -> courseFolderCache!![1].add(folderItem)
                }
            }
            courseFolderLatch!!.countDown()
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
                courseFolderLatch!!.await()
                courseFolderCache!![folderTypeIdx].forEach { emitter.onNext(it) }
            } catch (e: InterruptedException) {
                if (!emitter.isDisposed) emitter.onError(e)
            } finally {
                emitter.onComplete()
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun getFiles(folderItem: FolderItem): Observable<FileItem> {
        return post(
            hashMapOf(
                "courseid" to folderItem.courseId,
                "options[0][name]" to "cmid",
                "options[0][value]" to folderItem.folderId,
                "wsfunction" to "core_course_get_contents"
            )
        ).flatMap { response ->
            Observable.create<FileItem> { emitter ->
                val resJson = JSONArray(response)
                (0 until resJson.length()).map { resJson.get(it) as JSONObject }
                    .filter { it.getJSONArray("modules").length() > 0 }
                    .forEach {
                        val data = it.getJSONArray("modules")
                            .getJSONObject(0)
                            .getJSONArray("contents")
                        (0 until data.length()).map { data.get(it) as JSONObject }.forEach {
                            emitter.onNext(
                                FileItem(
                                    it.getString("filename"),
                                    it.getString("fileurl") + "&token=$token"
                                )
                            )
                        }
                    }
                emitter.onComplete()
            }
        }
    }

    override fun getScore(courseItem: CourseItem): Observable<ScoreItem> {
        return post(
            hashMapOf(
                "courseid" to courseItem.courseId,
                "userid" to userId,
                "wsfunction" to "gradereport_user_get_grades_table"
            )
        ).flatMap { response ->
            Observable.create<ScoreItem> { emitter ->
                val resJson = JSONObject(response)
                    .getJSONArray("tables")
                    .getJSONObject(0)
                    .getJSONArray("tabledata")
                for (i in 1 until resJson.length()) {
                    val tmp =
                        try {
                            resJson.getJSONObject(i)
                        } catch (e: JSONException) {
                            continue
                        }
                    emitter.onNext(
                        ScoreItem(
                            Regex("/<([^>]*)").find(tmp.getJSONObject("itemname").getString("content").reversed())
                            !!.groupValues.last().reversed(),
                            tmp.getJSONObject("grade").getString("content")
                        )
                    )
                }
                emitter.onComplete()
            }
        }
    }

    override fun getMembers(courseItem: CourseItem): Observable<MemberItem> {
        return post(
            hashMapOf(
                "courseid" to courseItem.courseId,
                "wsfunction" to "core_enrol_get_enrolled_users"
            )
        ).flatMap { response ->
            Observable.create<MemberItem> { emitter ->
                val respJson = JSONArray(response)
                (0 until respJson.length()).map { respJson.get(it) as JSONObject }
                    .forEach {
                        val roles = it.getJSONArray("roles")
                        val type =
                        //They don't provide role information sometimes, let's just assume it's student
                            if (roles.length() > 0) {
                                when (it.getJSONArray("roles").getJSONObject(0).getInt("roleid")) {
                                    5 -> MemberItem.Type.Student
                                    4, 9 -> MemberItem.Type.TA
                                    3 -> MemberItem.Type.Teacher
                                    else -> MemberItem.Type.Student
                                }
                            } else MemberItem.Type.Student
                        emitter.onNext(
                            MemberItem(
                                it.getString("fullname").split(" ").first(),
                                it.getString("fullname").split(" ").last(),
                                if (it.has("email")) {
                                    it.getString("email")
                                } else {
                                    ""
                                },
                                type
                            )
                        )
                    }
                emitter.onComplete()
            }
        }
    }


    override fun getCourseHwk(courseItem: CourseItem): Observable<HwkItem> {
        return post(
            hashMapOf(
                "courseids[0]" to courseItem.courseId,
                "wsfunction" to "mod_assign_get_assignments"
            )
        ).flatMap { response ->
            Observable.create<HwkItem> { emitter ->
                val resJson = JSONObject(response)
                    .getJSONArray("courses")
                    .getJSONObject(0)
                    .getJSONArray("assignments")
                (0 until resJson.length()).map { resJson.get(it) as JSONObject }.forEach {
                    val attachItems = mutableListOf<FileItem>()
                    val attachItemJson = it.getJSONArray("introattachments")
                    (0 until attachItemJson.length()).map { attachItemJson.get(it) as JSONObject }
                        .forEach {
                            attachItems.add(
                                FileItem(
                                    it.getString("filename"),
                                    it.getString("fileurl") + "?token=$token"
                                )
                            )
                        }
                    emitter.onNext(
                        HwkItem(
                            E3Type.NEW,
                            it.getString("name"),
                            it.getString("id"),
                            Date(it.getLong("allowsubmissionsfromdate") * 1000),
                            Date(it.getLong("duedate") * 1000),
                            it.getString("intro"),
                            attachItems
                        )
                    )
                }
                emitter.onComplete()
            }
        }
    }

    override fun getHwkSubmitFiles(hwkItem: HwkItem): Observable<FileItem> {
        return post(
            hashMapOf(
                "assignid" to hwkItem.hwkId,
                "userid" to userId,
                "wsfunction" to "mod_assign_get_submission_status"
            )
        ).flatMap { response ->
            Observable.create<FileItem> { emitter ->
                val resJson = JSONObject(response)
                    .getJSONObject("lastattempt")
                    .getJSONObject("submission")
                    .getJSONArray("plugins")
                    .getJSONObject(0)
                    .getJSONArray("fileareas")
                    .getJSONObject(0)
                    .getJSONArray("files")
                (0 until resJson.length()).map { resJson.get(it) as JSONObject }
                    .forEach {
                        emitter.onNext(
                            FileItem(
                                it.getString("filename"),
                                it.getString("fileurl") + "?token=$token"
                            )
                        )
                    }
                emitter.onComplete()
            }

        }
    }

    override fun getHwkDetail(hwkItem: HwkItem, courseItem: CourseItem?): Observable<HwkItem> {
        return Observable.just(hwkItem)
    }

    override fun getFrontPageAnns(): Observable<AnnItem> {
        throw NotImplementedError()
    }

    override fun getAnn(annItem: AnnItem): Observable<AnnItem> {
        return Observable.just(annItem)
    }

    override fun getCookie(): MutableList<Cookie>? {
        return null
    }

    override fun getBaseUrl(): String? {
        return "https://e3new.nctu.edu.tw"
    }

}
