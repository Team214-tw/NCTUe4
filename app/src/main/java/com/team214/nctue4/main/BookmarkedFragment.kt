package com.team214.nctue4.main

import android.content.Intent
import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Type
import com.team214.nctue4.course.CourseActivity
import com.team214.nctue4.model.CourseDBHelper
import com.team214.nctue4.model.CourseItem
import kotlinx.android.synthetic.main.fragment_couse_list.*
import kotlinx.android.synthetic.main.item_course.view.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_empty_compact.*


class BookmarkedFragment : Fragment() {
    private lateinit var courseDBHelper: CourseDBHelper
    private var courseItems = mutableListOf<CourseItem>()
    private var snackBar: Snackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        courseDBHelper = CourseDBHelper(context!!)
        if (arguments?.getBoolean("home") == null)
            activity!!.setTitle(R.string.bookmarked_courses)
        return inflater.inflate(R.layout.fragment_bookmarked, container, false)
    }

    override fun onDestroy() {
        snackBar?.dismiss()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (arguments?.getBoolean("home") != null)
            course_list_recycler_view.isNestedScrollingEnabled = false
        courseItems = courseDBHelper.readBookmarkedCourse(
            if (arguments?.getBoolean("home") != null) arguments!!.getInt("home_bookmarked_cnt", 5) else null
        )
        updateList()
        super.onViewCreated(view, savedInstanceState)
    }


    private fun updateList() {
        if (courseItems.isEmpty())
            (if (arguments?.getBoolean("home") != null) empty_request_compact else empty_request)?.visibility =
                View.VISIBLE
        else {
            val dragDropManager = RecyclerViewDragDropManager()
            val courseAdapter = CourseDragDropAdapter(courseItems,
                context, fun(view: View, course: CourseItem) {
                    when {
                        course.bookmarked == 1 -> {
                            courseDBHelper.bookmarkCourse(course.courseId, 0)
                            course.toggleBookmark()
                            view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.md_grey_500))
                        }
                        else -> {
                            course.toggleBookmark()
                            view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.new_e3))
                        }
                    }
                }, {
                    val intent = Intent()
                    intent.setClass(activity!!, CourseActivity::class.java)
                    intent.putExtra("courseItem", it)
                    startActivity(intent)
                }, courseDBHelper
            )
            val wrappedAdapter = dragDropManager.createWrappedAdapter(courseAdapter)
            course_list_recycler_view?.adapter = wrappedAdapter
            course_list_recycler_view?.layoutManager = LinearLayoutManager(context)
            course_list_recycler_view?.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
            dragDropManager.setDraggingItemShadowDrawable(
                ContextCompat.getDrawable(
                    context!!,
                    R.drawable.ms9_composite_shadow_z6
                ) as NinePatchDrawable
            )
            dragDropManager.setInitiateOnLongPress(true)
            dragDropManager.setInitiateOnMove(false)
            course_list_recycler_view.itemAnimator = DraggableItemAnimator()
            dragDropManager.attachRecyclerView(course_list_recycler_view)
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            if (arguments?.getBoolean("home") == null && courseItems.size >= 2 &&
                !pref.getBoolean("dragDropTipped", false)
            ) {
                snackBar =
                    Snackbar.make(course_list_root, getString(R.string.drag_drop_tip), Snackbar.LENGTH_INDEFINITE)
                snackBar!!.show()
                pref.edit().putBoolean("dragDropTipped", true).apply()
            }
        }
    }
}