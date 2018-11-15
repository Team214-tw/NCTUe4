package com.team214.nctue4.client

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import io.reactivex.Observable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class NewE3ApiClient(context: Context) : E3Client() {
    class TokenExpiredException : Exception()

    companion object {
        const val API_URL = "https://e3new.nctu.edu.tw/webservice/rest/server.php?moodlewsrestformat=json"
    }

    private val client = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var token = prefs.getString("newE3Token", "")
    private var userId = prefs.getString("newE3UserId", "")

    private fun post(
        data: HashMap<String, String>
    ): Observable<String> {
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
                            throw TokenExpiredException()
                        }
                    } catch (e: JSONException) {
                        // Response is a JsonArray, Pass
                    }
                }
            }
        }.retryWhen {
            it.filter { error -> error is TokenExpiredException }
                .flatMap { _ -> login() }
                .take(1)
                .concatMap { _ -> Observable.error<ServiceErrorException>(ServiceErrorException()) }
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
}