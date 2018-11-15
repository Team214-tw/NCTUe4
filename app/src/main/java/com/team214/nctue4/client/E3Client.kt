package com.team214.nctue4.client

import io.reactivex.Observable


abstract class E3Client {
    class ServiceErrorException : Exception()
    class WrongCredentialsException : Exception()

    abstract fun login(studentId: String? = null, password: String? = null): Observable<Unit>
}