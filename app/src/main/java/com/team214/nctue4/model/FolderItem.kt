package com.team214.nctue4.model


class FolderItem(
    val name: String,
    val folderId: String,
    val courseId: String,
    val folderType: Type
) {
    enum class Type { Handout, Reference }
}