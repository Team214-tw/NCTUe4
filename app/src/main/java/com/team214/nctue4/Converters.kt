package com.team214.nctue4

import androidx.room.TypeConverter
import com.team214.nctue4.client.E3Type
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
}