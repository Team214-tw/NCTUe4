package com.team214.nycue4.model

import android.os.Parcelable
import com.team214.nycue4.client.E3Type
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CourseItem(
    val e3Type: E3Type,
    val courseName: String,
    val courseId: String,
    val additionalInfo: String,
    val sortKey: Long,
    var bookmarked: Int = 0,
    val bookmarkIdx: Int = 10000,
    var time: String? = null,
    val id: Int? = null
) : Parcelable {
    fun toggleBookmark() {
        bookmarked = if (bookmarked == 1) 0 else 1
    }
}