package com.team214.nctue4.client

import com.team214.nctue4.model.AnnItem
import io.reactivex.Observable
import okhttp3.Cookie


abstract class E3Client {
    class ServiceErrorException : Exception()
    class WrongCredentialsException : Exception()

    abstract fun login(studentId: String? = null, password: String? = null): Observable<Unit>

    abstract fun getFrontPageAnn(): Observable<MutableList<AnnItem>>

    abstract fun getAnn(annItem: AnnItem): Observable<AnnItem>

    abstract fun getCookie(): MutableList<Cookie>?
}