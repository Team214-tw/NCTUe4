package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.model.CourseItem
import com.team214.nctue4.model.FolderItem
import io.reactivex.Observable
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class NewE3ApiClient(context: Context) : E3Client() {
    class TokenInvalidException : Exception()

    companion object {
        const val API_URL = "https://e3new.nctu.edu.tw/webservice/rest/server.php?moodlewsrestformat=json"
    }

    private val client = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
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
        return Observable.fromCallable {
            Log.d("NewE3Post", data.toString())
            client.newCall(request).execute().run {
                this.body()!!.string().apply {
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
        return Observable.fromCallable {
            val formBody = FormBody.Builder()
                .add("username", (studentId ?: prefs.getString("studentId", "")!!))
                .add("password", (password ?: prefs.getString("studentPortalPassword", "")!!))
                .add("service", "moodle_mobile_app")
                .build()
            val request = Request.Builder()
                .url("https://e3new.nctu.edu.tw/login/token.php")
                .post(formBody)
                .build()
            val response = client.newCall(request).execute()
            val resJson = JSONObject(response.body()!!.string())
            if (resJson.has("token")) {
                token = resJson.getString("token")
                prefs.edit().putString("newE3Token", token).apply()
            } else {
                throw WrongCredentialsException()
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

    override fun getCourseList(): Observable<MutableList<CourseItem>> {
        return post(
            hashMapOf(
                "wsfunction" to "core_enrol_get_users_courses",
                "userid" to userId
            )
        ).flatMap { response ->
            Observable.fromCallable {
                val resJson = JSONArray(response)
                val courseItems = mutableListOf<CourseItem>()
                (0 until resJson.length()).map { resJson.get(it) as JSONObject }
                    .forEach {
                        if (it.getLong("enddate") > System.currentTimeMillis() / 1000) {
                            val courseName = it.getString("fullname").split(".").run {
                                if (this.size >= 3) this[2].split(" ").first() else this[0]
                            }
                            val courseId = it.getString("id")
                            val additionalInfo = it.getString("shortname")
                            courseItems.add(CourseItem(E3Type.NEW, courseName, courseId, additionalInfo))
                        }
                    }
                courseItems
            }
        }
    }

    override fun getCourseAnns(courseItem: CourseItem): Observable<MutableList<AnnItem>> {
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
            Observable.fromCallable {
                val resJson = JSONObject(response).getJSONArray("discussions")
                val courseAnnItems = mutableListOf<AnnItem>()
                (0 until resJson.length()).map { resJson.get(it) as JSONObject }.forEach { ann ->
                    val title = ann.getString("name")
                    val content = ann.getString("message")
                    val date = Date(ann.getLong("timemodified") * 1000)
                    courseAnnItems.add(
                        AnnItem(
                            E3Type.NEW,
                            title,
                            date,
                            courseItem.courseName,
                            null,
                            content
                        )
                    )
                }
                courseAnnItems
            }
        }
    }

    override fun getCourseFolders(courseItem: CourseItem): Observable<FolderItem> {
        return post(
            hashMapOf(
                "wsfunction" to "mod_folder_get_folders_by_courses",
                "courseids[0]" to courseItem.courseId
            )
        ).flatMap { response ->
            Observable.create<FolderItem> { emitter ->
                val resJson = JSONObject(response).getJSONArray("folders")
                (0 until resJson.length()).map { resJson.get(it) as JSONObject }.forEach {
                    var name = it.getString("name")
                    val folderType =
                        if (name.startsWith("[參考資料]")) {
                            name = name.removePrefix("[參考資料]")
                            FolderItem.Type.Reference
                        } else if (name.startsWith("[Reference]")) {
                            name = name.removePrefix("[Reference]")
                            FolderItem.Type.Reference
                        } else {
                            FolderItem.Type.Handout
                        }
                    emitter.onNext(
                        FolderItem(
                            name,
                            it.getString("coursemodule"),
                            courseItem.courseId,
                            folderType
                        )
                    )
                }
                emitter.onComplete()
            }
        }
    }

    override fun getFrontPageAnns(): Observable<MutableList<AnnItem>> {
        throw NotImplementedError()
    }

    override fun getAnn(annItem: AnnItem): Observable<AnnItem> {
        return Observable.just(annItem)
    }

    override fun getCookie(): MutableList<Cookie>? {
        return null
    }
}