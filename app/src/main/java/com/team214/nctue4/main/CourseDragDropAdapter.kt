package com.team214.nctue4.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Type
import com.team214.nctue4.model.CourseDBHelper
import com.team214.nctue4.model.CourseItem
import kotlinx.android.synthetic.main.item_course.view.*

class CourseDragDropAdapter(
    private val dataSet: MutableList<CourseItem>,
    private val context: Context?,
    private val starClickListener: ((view: View, course: CourseItem) -> Unit),
    private val itemClickListener: (CourseItem) -> Unit,
    private val dbHelper: CourseDBHelper? = null
) :
    RecyclerView.Adapter<CourseDragDropAdapter.ViewHolder>(), DraggableItemAdapter<CourseDragDropAdapter.ViewHolder> {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id!!.toLong()
    }

    class ViewHolder(
        val view: View,
        private val context: Context?,
        private val starClickListener: ((view: View, course: CourseItem) -> Unit),
        private val itemClickListener: (CourseItem) -> Unit,
        private val dbHelper: CourseDBHelper? = null
    ) : AbstractDraggableItemViewHolder(view) {
        fun bind(course: CourseItem) {
            view.course_name.text = course.courseName
            view.course_additional_info.text = course.additionalInfo
            view.course_item?.setOnClickListener {
                itemClickListener(course)
            }

            if (course.bookmarked == 1) {
                if (course.e3Type == E3Type.OLD)
                    view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.old_e3))
                else
                    view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.new_e3))
            } else view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.blueGrey))

            if (course.e3Type == E3Type.OLD)
                view.course_item_e3_bar.setBackgroundColor(ContextCompat.getColor(context, R.color.old_e3))
            else
                view.course_item_e3_bar.setBackgroundColor(ContextCompat.getColor(context, R.color.new_e3))
            view.course_star?.setOnClickListener {
                starClickListener(view, course)
            }

            view.course_star?.setOnClickListener {
                starClickListener(view, course)
            }

        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view, context, starClickListener, itemClickListener, dbHelper)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])

    }

    override fun getItemCount() = dataSet.size

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

    override fun onGetItemDraggableRange(holder: ViewHolder?, position: Int): ItemDraggableRange? = null

    override fun onCheckCanStartDrag(holder: ViewHolder?, position: Int, x: Int, y: Int): Boolean = true

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) = notifyDataSetChanged()

    override fun onItemDragStarted(position: Int) = notifyDataSetChanged()

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        dataSet.add(toPosition, dataSet.removeAt(fromPosition))
        dbHelper?.updateBookmarkIdx(dataSet)
    }
}