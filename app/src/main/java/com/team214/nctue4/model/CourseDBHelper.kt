package com.team214.nctue4.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CourseDBHelper(context: Context)
    : SQLiteOpenHelper(context, "courses.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL("CREATE TABLE if not exists courses ( id integer PRIMARY KEY autoincrement, " +
                "course_no text,course_name text,teacher_name text, course_id text," +
                " e3_type int, bookmarked int, bookmark_idx int, idx int )")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun readBookmarkedCourse(limit: Int?): ArrayList<CourseItem> {
        val data = ArrayList<CourseItem>()
        val cursor = readableDatabase.query("courses", null, "bookmarked = ?",
            arrayOf(1.toString()), null, null, "bookmark_idx", limit?.toString())
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                data.add(cursorToItem(cursor))
                cursor.moveToNext()
            }
        }
        cursor.close()
        return data
    }

    fun bookmarkCourse(courseId: String, bookmarked: Int) {
        val values = ContentValues()
        values.put("bookmarked", bookmarked)
        writableDatabase.update("courses", values, "course_id = ?", arrayOf(courseId))
    }

    fun updateBookmarkIdx(data: ArrayList<CourseItem>) {
        (0 until data.size).forEach {
            val values = ContentValues()
            values.put("bookmark_idx", it)
            writableDatabase.update("courses", values, "id =?", arrayOf(data[it].id.toString()))
        }

    }

    fun delTable() {
        writableDatabase.delete("courses", null, null)
    }

    fun refreshCourses(data: ArrayList<CourseItem>, e3Type: Int) {
        val hashSet = HashSet<String>()
        val cursor = readableDatabase.query("courses", arrayOf("course_id"),
            "e3_type = ? and bookmarked = ?",
            arrayOf(e3Type.toString(), 1.toString()), null, null, null)
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                hashSet.add(cursor.getString(0))
                cursor.moveToNext()
            }
        }
        cursor.close()
        writableDatabase.delete("courses", "e3_type=?", arrayOf(e3Type.toString()))
        data.forEach {
            val values = ContentValues()
            values.put("course_no", it.courseNo)
            values.put("course_name", it.courseName)
            values.put("teacher_name", it.teacherName)
            values.put("course_id", it.courseId)
            values.put("e3_type", it.e3Type)
            values.put("bookmarked", if (hashSet.contains(it.courseId)) 1 else 0)
            values.put("bookmark_idx", it.bookmarkIdx)
            writableDatabase.insert("courses", null, values)
        }
    }


    fun readCourses(e3Type: Int): ArrayList<CourseItem> {
        val data = ArrayList<CourseItem>()
        val cursor = readableDatabase.query("courses", null, "e3_type = ?", arrayOf(e3Type.toString()), null, null, "idx")
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
            cursor.getString(cursor.getColumnIndex("course_no")),
            cursor.getString(cursor.getColumnIndex("course_name")),
            cursor.getString(cursor.getColumnIndex("teacher_name")),
            cursor.getString(cursor.getColumnIndex("course_id")),
            cursor.getInt(cursor.getColumnIndex("e3_type")),
            cursor.getInt(cursor.getColumnIndex("bookmarked")),
            cursor.getInt(cursor.getColumnIndex("bookmark_idx")),
            cursor.getInt(cursor.getColumnIndex("id"))
        )
    }
}