package com.team214.nycue4.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.team214.nycue4.client.E3Type

class CourseDBHelper(context: Context) : SQLiteOpenHelper(context, "courses.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(
            "CREATE TABLE if not exists courses ( id integer PRIMARY KEY autoincrement, " +
                    "e3_type text, course_name text, course_id text, time text," +
                    "additional_info text, sort_key integer, bookmarked integer, bookmark_idx integer)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 1) {
            db!!.execSQL("DROP TABLE IF EXISTS courses")
            onCreate(db)
        }
        if (oldVersion == 2) {
            val data = mutableListOf<CourseItem>()
            val cursor = db!!.rawQuery("SELECT * FROM courses", null)
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    data.add(cursorToItemV2(cursor))
                    cursor.moveToNext()
                }
            }
            cursor.close()
            db.execSQL("DROP TABLE IF EXISTS courses")
            onCreate(db)
            data.forEach {
                val values = ContentValues()
                values.put("e3_type", it.e3Type.name)
                values.put("course_name", it.courseName)
                values.put("course_id", it.courseId)
                values.put("additional_info", it.additionalInfo)
                values.put("sort_key", it.sortKey)
                values.put("bookmarked", it.bookmarked)
                values.put("bookmark_idx", it.bookmarkIdx)
                db.insert("courses", null, values)
            }
        }
    }

    fun readBookmarkedCourse(limit: Int?): MutableList<CourseItem> {
        val data = mutableListOf<CourseItem>()
        val db = readableDatabase
        val cursor = db.query(
            "courses", null, "bookmarked = ?",
            arrayOf(1.toString()), null, null, "bookmark_idx", limit?.toString()
        )
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                data.add(cursorToItem(cursor))
                cursor.moveToNext()
            }
        }
        cursor.close()
        return data
    }

    fun getCourseFromName(courseName: String, e3Type: E3Type): CourseItem? {
        val cursor = readableDatabase.query(
            "courses", null, "course_name = ? and e3_type = ?",
            arrayOf(courseName, e3Type.name), null, null, null
        )
        cursor.moveToFirst()
        return if (cursor.isAfterLast) null else return cursorToItem(cursor)
    }

    fun bookmarkCourse(courseId: String, bookmarked: Int) {
        val values = ContentValues()
        values.put("bookmarked", bookmarked)
        writableDatabase.update("courses", values, "course_id = ?", arrayOf(courseId))
    }

    fun updateBookmarkIdx(data: MutableList<CourseItem>) {
        (0 until data.size).forEach {
            val values = ContentValues()
            values.put("bookmark_idx", it)
            writableDatabase.update("courses", values, "id =?", arrayOf(data[it].id.toString()))
        }
    }

    fun updateCourseTime(courseId: String, time: String) {
        val values = ContentValues()
        values.put("time", time)
        writableDatabase.update("courses", values, "course_id = ?", arrayOf(courseId))
    }

    fun delTable() {
        writableDatabase.delete("courses", null, null)
    }

    fun refreshCourses(data: MutableList<CourseItem>, e3Type: E3Type) {
        val hashSet = HashSet<String>()
        val cursor = readableDatabase.query(
            "courses", arrayOf("course_id"),
            "e3_type = ? and bookmarked = ?",
            arrayOf(e3Type.name, 1.toString()), null, null, null
        )
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                hashSet.add(cursor.getString(0))
                cursor.moveToNext()
            }
        }
        cursor.close()
        writableDatabase.delete("courses", "e3_type=?", arrayOf(e3Type.name))
        data.forEach {
            val values = ContentValues()
            values.put("e3_type", it.e3Type.name)
            values.put("course_name", it.courseName)
            values.put("course_id", it.courseId)
            values.put("additional_info", it.additionalInfo)
            values.put("sort_key", it.sortKey)
            values.put("bookmarked", if (hashSet.contains(it.courseId)) 1 else 0)
            values.put("bookmark_idx", it.bookmarkIdx)
            values.put("time", it.time)
            writableDatabase.insert("courses", null, values)
        }
    }

    fun addCourses(data: MutableList<CourseItem>) {
        data.forEach {
            val values = ContentValues()
            values.put("e3_type", it.e3Type.name)
            values.put("course_name", it.courseName)
            values.put("course_id", it.courseId)
            values.put("additional_info", it.additionalInfo)
            values.put("sort_key", it.sortKey)
            values.put("bookmarked", 0)
            values.put("bookmark_idx", it.bookmarkIdx)
            writableDatabase.insert("courses", null, values)
        }
    }

    fun isCoursesTableEmpty(): Boolean {
        var empty = true
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM courses", null)
        if (cursor != null && cursor.moveToFirst()) {
            empty = (cursor.getInt(0) == 0)
        }
        cursor.close()
        return empty
    }

    fun readCourses(e3Type: E3Type): MutableList<CourseItem> {
        val data = mutableListOf<CourseItem>()
        val cursor =
            readableDatabase.query(
                "courses",
                null,
                "e3_type = ?",
                arrayOf(e3Type.name),
                null,
                null,
                "sort_key DESC"
            )
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                data.add(cursorToItem(cursor))
                cursor.moveToNext()
            }
        }
        cursor.close()
        return data
    }

    private fun cursorToItem(cursor: Cursor): CourseItem {
        return CourseItem(
            E3Type.valueOf(cursor.getString(cursor.getColumnIndex("e3_type"))),
            cursor.getString(cursor.getColumnIndex("course_name")),
            cursor.getString(cursor.getColumnIndex("course_id")),
            cursor.getString(cursor.getColumnIndex("additional_info")),
            cursor.getLong(cursor.getColumnIndex("sort_key")),
            cursor.getInt(cursor.getColumnIndex("bookmarked")),
            cursor.getInt(cursor.getColumnIndex("bookmark_idx")),
            cursor.getString(cursor.getColumnIndex("time")),
            cursor.getInt(cursor.getColumnIndex("id"))
        )
    }


    // old version (for migration)
    private fun cursorToItemV2(cursor: Cursor): CourseItem {
        return CourseItem(
            E3Type.valueOf(cursor.getString(cursor.getColumnIndex("e3_type"))),
            cursor.getString(cursor.getColumnIndex("course_name")),
            cursor.getString(cursor.getColumnIndex("course_id")),
            cursor.getString(cursor.getColumnIndex("additional_info")),
            -1,
            cursor.getInt(cursor.getColumnIndex("bookmarked")),
            cursor.getInt(cursor.getColumnIndex("bookmark_idx")),
            cursor.getString(cursor.getColumnIndex("time")),
            cursor.getInt(cursor.getColumnIndex("id"))
        )
    }
}