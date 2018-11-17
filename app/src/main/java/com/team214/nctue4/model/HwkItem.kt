package com.team214.nctue4.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.util.*

@Parcelize
class HwkItem(
    var name: String,
    var assignId: String,
    val submitId: String,
    var startDate: Date,
    var endDate: Date,
    var content: String,
    var attachItem: MutableList<File> = ArrayList(),
    var sentItem: MutableList<File> = ArrayList()
) : Parcelable