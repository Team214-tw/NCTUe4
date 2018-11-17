package com.team214.nctue4.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.text.NumberFormat
import java.util.*

@Parcelize
data class FileItem(
    val name: String,
    val url: String
) : Parcelable
