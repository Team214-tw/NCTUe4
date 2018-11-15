package com.team214.nctue4.model

import java.text.NumberFormat
import java.util.*


class FileItem(
    var name: String,
    private val _fileSize: String,
    var url: String
) {
    val fileSize: String = _fileSize
        get() =
            try {
                "${NumberFormat.getNumberInstance(Locale.US).format(field.toInt())} B"
            } catch (e: NumberFormatException) {
                "$field B"
            }
}
