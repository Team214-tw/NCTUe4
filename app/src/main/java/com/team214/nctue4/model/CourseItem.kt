package com.team214.nctue4.model

class CourseItem(
    val courseNo: String,
    val courseName: String,
    val teacherName: String,
    val courseId: String,
    val e3Type: Int,
    var bookmarked: Int = 0,
    val bookmarkIdx: Int = 10000,
    val id: Int? = null
) {
    fun toggleBookmark() {
        bookmarked = if (bookmarked == 1) 0 else 1
    }
}