package com.team214.nctue4.model

import com.team214.nctue4.client.E3Type

class CourseItem(
    val e3Type: E3Type,
    val courseName: String,
    val courseId: String,
    val additionalInfo: String,
    var bookmarked: Int = 0,
    val bookmarkIdx: Int = 10000,
    val id: Int? = null
) {
    fun toggleBookmark() {
        bookmarked = if (bookmarked == 1) 0 else 1
    }
}