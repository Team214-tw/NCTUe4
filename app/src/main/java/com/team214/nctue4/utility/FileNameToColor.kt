package com.team214.nctue4.utility

import com.team214.nctue4.R

class FileNameToColor {

    fun getId(fileName: String): Int {
        val key = fileName.split(".").last()
        return if (key in iconMap) iconMap[key]!! else R.color.md_grey_600
    }

    companion object {
        val iconMap = hashMapOf(
            "png" to R.color.md_amber_800,
            "jpg" to R.color.md_amber_800,
            "jpeg" to R.color.md_amber_800,
            "gif" to R.color.md_amber_800,
            "bmp" to R.color.md_amber_800,
            "tif" to R.color.md_amber_800,
            "tiff" to R.color.md_amber_800,
            "svg" to R.color.md_amber_800,
            "7z" to R.color.md_brown_500,
            "rar" to R.color.md_brown_500,
            "zip" to R.color.md_brown_500,
            "gz" to R.color.md_brown_500,
            "deb" to R.color.md_brown_500,
            "rpm" to R.color.md_brown_500,
            "pkg" to R.color.md_brown_500,
            "mp3" to R.color.md_blue_300,
            "wav" to R.color.md_blue_300,
            "wma" to R.color.md_blue_300,
            "wpa" to R.color.md_blue_300,
            "aif" to R.color.md_blue_300,
            "cda" to R.color.md_blue_300,
            "mid" to R.color.md_blue_300,
            "midi" to R.color.md_blue_300,
            "ogg" to R.color.md_blue_300,
            "cpp" to R.color.md_purple_500,
            "c" to R.color.md_purple_500,
            "py" to R.color.md_purple_500,
            "java" to R.color.md_purple_500,
            "kt" to R.color.md_purple_500,
            "js" to R.color.md_purple_500,
            "ts" to R.color.md_purple_500,
            "json" to R.color.md_purple_500,
            "xml" to R.color.md_purple_500,
            "htm" to R.color.md_purple_500,
            "html" to R.color.md_purple_500,
            "f90" to R.color.md_purple_500,
            "h" to R.color.md_purple_500,
            "sh" to R.color.md_purple_500,
            "xls" to R.color.msExcel,
            "xlsx" to R.color.msExcel,
            "ods" to R.color.msExcel,
            "xls" to R.color.msExcel,
            "pdf" to R.color.adobePdf,
            "ppt" to R.color.msPowerPoint,
            "pptx" to R.color.msPowerPoint,
            "pps" to R.color.msPowerPoint,
            "ppsx" to R.color.msPowerPoint,
            "odp" to R.color.msPowerPoint,
            "doc" to R.color.msWord,
            "docx" to R.color.msWord,
            "odt" to R.color.msWord,
            "webm" to R.color.md_teal_500,
            "mkv" to R.color.md_teal_500,
            "flv" to R.color.md_teal_500,
            "avi" to R.color.md_teal_500,
            "mov" to R.color.md_teal_500,
            "wmv" to R.color.md_teal_500,
            "rmvb" to R.color.md_teal_500,
            "mp4" to R.color.md_teal_500,
            "m4v" to R.color.md_teal_500,
            "mpg" to R.color.md_teal_500,
            "mpeg" to R.color.md_teal_500,
            "mp2" to R.color.md_teal_500,
            "mpe" to R.color.md_teal_500,
            "m2v" to R.color.md_teal_500,
            "m4v" to R.color.md_teal_500,
            "3gp" to R.color.md_teal_500
        )
    }
}