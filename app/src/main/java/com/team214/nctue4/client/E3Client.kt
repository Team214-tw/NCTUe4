package com.team214.nctue4.client

import com.team214.nctue4.model.*
import io.reactivex.Observable
import okhttp3.Cookie


abstract class E3Client {
    class ServiceErrorException : Exception()
    class WrongCredentialsException : Exception()

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
}