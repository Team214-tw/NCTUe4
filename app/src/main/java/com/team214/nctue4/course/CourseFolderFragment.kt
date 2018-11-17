package com.team214.nctue4.course

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.model.CourseItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class CourseFolderFragment : Fragment() {
    lateinit var client: E3Client
    lateinit var courseItem: CourseItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseItem = (activity as CourseActivity).courseItem
        client = (activity as CourseActivity).client
        client.getCourseFolders(courseItem)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { Log.d("E3Folder", it.name) },
                onError = { Log.d("E3error", "QQ") }

            )
    }
}