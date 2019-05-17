package com.team214.nctue4.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.team214.nctue4.client.E3Type
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

/*  detailLocationHint is a string that help client find where the detail ann is
    For old e3, it will be course id
    For new e3 web, it will be a http link to the detail page */

@Entity(tableName = "annTable")
@Parcelize

data class AnnItem(

    var e3Type: E3Type = E3Type.NEW,
    var title: String = "",
    var date: Date? = null,
    var courseName: String = "",
    var detailLocationHint: String?,
    @Ignore val content: String?,
    @Ignore val attachItems: MutableList<FileItem>
) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @IgnoredOnParcel
    var id: Int = 0

    constructor(e3Type: E3Type, title: String, date: Date?, courseName: String, detailLocationHint: String?) : this(
        e3Type,
        title,
        date,
        courseName,
        detailLocationHint,
        null,
        mutableListOf()
    )
}