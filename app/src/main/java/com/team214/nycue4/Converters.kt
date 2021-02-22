package com.team214.nycue4

import androidx.room.TypeConverter
import com.team214.nycue4.client.E3Type
import com.team214.nycue4.model.FileItem
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.util.*

class Converters {
    @TypeConverter
    fun fromE3Type(type: E3Type): Int {
        return type.ordinal
    }

    @TypeConverter
    fun toE3Type(x: Int): E3Type {
        return E3Type.values()[x]
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(x: Long): Date {
        return Date(x)
    }

    @TypeConverter
    fun toAttachItems(x: String): MutableList<FileItem> {
        return Json.parse(FileItem.serializer().list, x).toMutableList()

    }

    @TypeConverter
    fun fromAttachItems(x: MutableList<FileItem>): String {
        return Json.stringify(FileItem.serializer().list, x)
    }
}