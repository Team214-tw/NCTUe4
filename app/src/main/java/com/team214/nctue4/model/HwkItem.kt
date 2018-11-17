package com.team214.nctue4.model

import android.os.Parcelable
import com.team214.nctue4.client.E3Type
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class HwkItem(
    val e3Type: E3Type,
    val name: String,
    val hwkId: String,
    val startDate: Date,
    val endDate: Date,
    val content: String? = null,
    val attachItems: MutableList<FileItem> = mutableListOf(),
    val submitItems: MutableList<FileItem> = mutableListOf()
) : Parcelable