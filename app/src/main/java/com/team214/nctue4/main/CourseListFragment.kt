package com.team214.nctue4.main

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Type
import com.team214.nctue4.model.CourseDBHelper
import com.team214.nctue4.model.CourseItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_couse_list.*
import kotlinx.android.synthetic.main.item_course.view.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*

class CourseListFragment : Fragment() {
    private lateinit var e3Type: E3Type
    private lateinit var courseDBHelper: CourseDBHelper
    private var courseItems = mutableListOf<CourseItem>()
    private var disposable: Disposable? = null

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.refresh, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.action_refresh -> {
                disposable?.dispose()
                getData()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        e3Type = arguments!!.getSerializable("e3Type") as E3Type
        activity!!.setTitle(
            when (e3Type) {
                E3Type.NEW -> R.string.new_e3
                E3Type.OLD -> R.string.old_e3
            }
        )
        setHasOptionsMenu(true)
        courseDBHelper = CourseDBHelper(context!!)
        return inflater.inflate(R.layout.fragment_couse_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseItems = courseDBHelper.readCourses(e3Type)
        if (courseItems.isEmpty()) {
            empty_request.visibility = View.VISIBLE
            getData()
        } else {
            displayData()
        }

    }

    private fun getData() {
        courseItems.clear()
        progress_bar.visibility = View.VISIBLE
        error_request.visibility = View.GONE
        val client = when (e3Type) {
            E3Type.OLD -> (activity as MainActivity).oldE3Client
            E3Type.NEW -> (activity as MainActivity).newE3ApiClient
        }
        disposable = client.getCourseList()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    courseItems.addAll(it)
                    courseDBHelper.refreshCourses(courseItems, e3Type)
                    displayData()
                    Snackbar.make(course_list_root, getString(R.string.refresh_success), Snackbar.LENGTH_SHORT).show()
                },
                onError = {
                    Snackbar.make(course_list_root, getString(R.string.generic_error), Snackbar.LENGTH_SHORT).show()
                    progress_bar.visibility = View.INVISIBLE
                }
            )
    }

    private fun displayData() {
        progress_bar.visibility = View.INVISIBLE
        if (course_list_recycler_view.adapter != null) {
            course_list_recycler_view.adapter?.notifyDataSetChanged()
            return
        }
        if (courseItems.isEmpty()) empty_request.visibility = View.VISIBLE
        else {
            empty_request.visibility = View.GONE
            course_list_recycler_view?.layoutManager = LinearLayoutManager(context)
            course_list_recycler_view?.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
            course_list_recycler_view?.adapter = CourseAdapter(courseItems,
                context, fun(view: View, course: CourseItem) {
                    if (course.bookmarked == 1) {
                        courseDBHelper.bookmarkCourse(course.courseId, 0)
                        course.toggleBookmark()
                        view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.md_grey_500))
                    } else {
                        courseDBHelper.bookmarkCourse(course.courseId, 1)
                        course.toggleBookmark()
                        view.course_star.setColorFilter(ContextCompat.getColor(context!!, R.color.new_e3))
                    }
                }, {
                    //TODO Course Activity
                })

        }

    }

}