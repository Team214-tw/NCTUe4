package com.team214.nctue4.model

import android.os.Parcelable
import com.team214.nctue4.client.E3Type
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CourseItem(
    val e3Type: E3Type,
    val courseName: String,
    val courseId: String,
    val additionalInfo: String,
    var bookmarked: Int = 0,
    val bookmarkIdx: Int = 10000,
    val id: Int? = null
) : Parcelable {
    fun toggleBookmark() {
        bookmarked = if (bookmarked == 1) 0 else 1
    }
}