package com.team214.nctue4.client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.team214.nctue4.R
import com.team214.nctue4.main.MainActivity
import com.team214.nctue4.model.CourseDBHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class CourseListFragment : Fragment() {
    private lateinit var e3Type: E3Type
    private lateinit var courseDBHelper: CourseDBHelper

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
        val client = when (e3Type) {
            E3Type.OLD -> (activity as MainActivity).oldE3Client
            E3Type.NEW -> (activity as MainActivity).newE3ApiClient
        }
        client.getCourseList()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                it.forEach {
                    Log.d("E3Course", it.courseName)
                }
            }
    }
}