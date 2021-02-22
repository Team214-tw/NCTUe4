package com.team214.nycue4.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.team214.nycue4.R
import com.team214.nycue4.model.CourseItem
import kotlinx.android.synthetic.main.item_course.view.*

class CourseAdapter(
    private val dataSet: MutableList<CourseItem>,
    private val context: Context?,
    private val starClickListener: ((view: View, course: CourseItem) -> Unit),
    private val itemClickListener: (CourseItem) -> Unit
) :
    RecyclerView.Adapter<CourseAdapter.ViewHolder>() {
    class ViewHolder(
        val view: View,
        private val context: Context?,
        private val starClickListener: ((view: View, course: CourseItem) -> Unit),
        private val itemClickListener: (CourseItem) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        fun bind(course: CourseItem) {
            view.course_name.text = course.courseName
            view.course_additional_info.text = course.additionalInfo
            view.course_item?.setOnClickListener {
                itemClickListener(course)
            }

            if (course.bookmarked == 1) {
                view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.new_e3))
            } else view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.md_grey_500))

            view.course_item_e3_bar.setBackgroundColor(ContextCompat.getColor(context, R.color.new_e3))
            view.course_star?.setOnClickListener {
                starClickListener(view, course)
            }

        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CourseAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view, context, starClickListener, itemClickListener)
    }

    override fun onBindViewHolder(holder: CourseAdapter.ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size
}