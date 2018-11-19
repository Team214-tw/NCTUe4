package com.team214.nctue4.client

import com.team214.nctue4.model.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


abstract class E3Client {
    class ServiceErrorException : Exception()
    class WrongCredentialsException : Exception()

    protected abstract val client: OkHttpClient
    protected fun clientExecute(request: Request): Observable<Pair<Response, String>> {
        return Observable.create { emitter ->
            try {
                val response = client.newCall(request).execute()
                emitter.onNext(Pair(response, response.body()!!.string()))
                emitter.onComplete()
            } catch (e: Exception) {
                if (emitter.isDisposed) {
                    emitter.onComplete()
                } else {
                    emitter.onError(e)
                }
            }
        }
    }

    abstract fun login(studentId: String? = null, password: String? = null): Observable<Unit>

    abstract fun getFrontPageAnns(): Observable<AnnItem>

    abstract fun getAnn(annItem: AnnItem): Observable<AnnItem>

    abstract fun getCookie(): MutableList<Cookie>?

    abstract fun getCourseList(): Observable<CourseItem>

    abstract fun getCourseAnns(courseItem: CourseItem): Observable<AnnItem>

    abstract fun getCourseFolders(courseItem: CourseItem): Observable<FolderItem>

    abstract fun getFiles(folderItem: FolderItem): Observable<FileItem>

    abstract fun getScore(courseItem: CourseItem): Observable<ScoreItem>

    abstract fun getMembers(courseItem: CourseItem): Observable<MemberItem>

    abstract fun getCourseHwk(courseItem: CourseItem): Observable<HwkItem>

    abstract fun getHwkDetail(hwkItem: HwkItem, courseItem: CourseItem?): Observable<HwkItem>

    abstract fun getHwkSubmitFiles(hwkItem: HwkItem): Observable<FileItem>
}