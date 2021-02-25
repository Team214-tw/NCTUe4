package com.team214.nycue4.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nycue4.R
import com.team214.nycue4.client.E3Type
import com.team214.nycue4.model.CourseDBHelper
import com.team214.nycue4.model.CourseItem
import kotlinx.android.synthetic.main.fragment_timetable_edit.*
import kotlinx.android.synthetic.main.status_empty.*

class TimetableEditFragment : Fragment() {
    private lateinit var courseDBHelper: CourseDBHelper
    private var courseItems = mutableListOf<CourseItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        courseDBHelper = CourseDBHelper(requireContext())
        val cur = System.currentTimeMillis() / 1000
        courseItems = courseDBHelper.readCourses(E3Type.NEW)
        courseItems.retainAll { cur <= it.sortKey }
        Log.d("Courses", courseItems.toString())
        return inflater.inflate(R.layout.fragment_timetable_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayData()
    }

    private fun displayData() {
        if (timetable_edit_recycler_view.adapter != null) {
            timetable_edit_recycler_view.adapter?.notifyDataSetChanged()
            return
        }
        if (courseItems.isEmpty()) {
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
        } else {
            empty_request.visibility = View.GONE
            empty_request_new_e3.visibility = View.GONE
            timetable_edit_recycler_view?.layoutManager = LinearLayoutManager(context)
            timetable_edit_recycler_view?.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
            timetable_edit_recycler_view?.adapter = TimetableEditAdapter(courseItems, requireContext())
        }
    }
}