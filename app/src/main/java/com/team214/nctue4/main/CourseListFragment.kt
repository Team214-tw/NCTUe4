package com.team214.nctue4.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.client.E3Clients
import com.team214.nctue4.client.E3Type
import com.team214.nctue4.course.CourseActivity
import com.team214.nctue4.model.CourseDBHelper
import com.team214.nctue4.model.CourseItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.refresh, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
            }
        )
        setHasOptionsMenu(true)
        courseDBHelper = CourseDBHelper(context!!)
        return inflater.inflate(R.layout.fragment_couse_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseItems = courseDBHelper.readCourses(e3Type)
        getData()
        if (courseItems.isEmpty()) {
            empty_request.visibility = View.VISIBLE
        } else {
            displayData()
        }

    }

    private fun getData() {
        progress_bar.visibility = View.VISIBLE
        error_request.visibility = View.GONE
        val client = when (e3Type) {
            E3Type.NEW -> E3Clients.getNewE3ApiClient(context!!)
        }
        disposable = client.getCourseList()
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(mutableListOf<CourseItem>()) { courseItems, courseItem -> courseItems.add(courseItem) }
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onSuccess = {
                    val oldSet = setOf(*courseItems.map { x -> x.courseId }.toTypedArray())
                    val newSet = setOf(*it.map { x -> x.courseId }.toTypedArray())
                    if (oldSet != newSet) {
                        courseDBHelper.refreshCourses(it, e3Type)
                        courseItems.clear()
                        courseItems.addAll(courseDBHelper.readCourses(e3Type))
                        displayData()
                        Snackbar.make(course_list_root, getString(R.string.refresh_success), Snackbar.LENGTH_SHORT)
                            .show()
                    }
                },
                onError = { error ->
                    when (error) {
                        is E3Client.WrongCredentialsException -> Snackbar.make(
                            course_list_root,
                            getString(R.string.wrong_credential),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        else -> Snackbar.make(
                            course_list_root,
                            getString(R.string.generic_error),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }

                }
            )
    }

    private fun displayData() {
        if (course_list_recycler_view.adapter != null) {
            course_list_recycler_view.adapter?.notifyDataSetChanged()
            return
        }
        if (courseItems.isEmpty()) {
            when (e3Type) {
                E3Type.NEW -> {
                    empty_request.visibility = View.GONE
                    empty_request_new_e3.visibility = View.VISIBLE
                    click_to_agree_site_policy.setOnClickListener {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://e3new.nctu.edu.tw/user/policy.php")
                            )
                        )
                    }
                }
            }

        } else {
            empty_request.visibility = View.GONE
            empty_request_new_e3.visibility = View.GONE
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
                        view.course_star.setColorFilter(
                            ContextCompat.getColor(
                                context!!,
                                if (course.e3Type == E3Type.NEW) R.color.new_e3 else R.color.old_e3
                            )
                        )
                    }
                }, {
                    val intent = Intent()
                    intent.setClass(activity!!, CourseActivity::class.java)
                    intent.putExtra("courseItem", it)
                    startActivity(intent)
                })

        }

    }

}