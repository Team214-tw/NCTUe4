package com.team214.nycue4.main

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.team214.nycue4.R
import com.team214.nycue4.model.CourseDBHelper
import com.team214.nycue4.model.CourseItem
import kotlinx.android.synthetic.main.item_timetable_edit.view.*


class TimetableEditAdapter(
    private val dataSet: MutableList<CourseItem>,
    private val context: Context?
) : RecyclerView.Adapter<TimetableEditAdapter.ViewHolder>() {

    class ViewHolder(
        val view: View,
        private val context: Context?
    ) : RecyclerView.ViewHolder(view) {
        private lateinit var courseDBHelper: CourseDBHelper
        private val regex = Regex("[M|T|W|R|F|S|U[1-9a|b|c|y|z|n]*]*")
        fun bind(course: CourseItem) {
            courseDBHelper = CourseDBHelper(context!!)
            view.course_name.text = course.courseName
            view.course_additional_info.text = course.additionalInfo
            view.edit_time.text.clear()
            if (course.time != null) {
                view.edit_time.text.append(course.time)
            }
            view.edit_time.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    if (regex.matches(s.toString()) || s.toString() == "") {
                        courseDBHelper.updateCourseTime(course.courseId, s.toString())
                        Log.d("Update", "course.courseId$s")
                    }
                }
            })
            view.course_item_e3_bar.setBackgroundColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.new_e3
                )
            )
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timetable_edit, parent, false)
        return ViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size
}