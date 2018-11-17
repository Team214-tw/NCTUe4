package com.team214.nctue4.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class FolderItem(
    val name: String,
    val folderId: String,
    val courseId: String,
    val folderType: Type
) : Parcelable {
    enum class Type { Handout, Reference }
}