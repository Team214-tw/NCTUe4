package com.team214.nctue4.model

import android.os.Parcelable
import com.team214.nctue4.client.E3Type
import kotlinx.android.parcel.Parcelize
import java.util.*

/*  detailLocationHint is a string that help client find where the detail ann is
    For old e3, it will be course id
    For new e3 web, it will be a http link to the detail page */

@Parcelize
data class AnnItem(
    val e3Type: E3Type,
    val title: String,
    val date: Date?,
    val courseName: String,
    val detailLocationHint: String?,
    val content: String? = null,
    val attachItems: MutableList<FileItem> = mutableListOf()
) : Parcelable