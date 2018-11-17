package com.team214.nctue4.model

data class MemberItem(
    val name: String,
    val department: String,
    val email: String,
    val type: Type,
    var selected: Boolean = false
) {
    enum class Type {
        Teacher, TA, Student, Audit
    }
}