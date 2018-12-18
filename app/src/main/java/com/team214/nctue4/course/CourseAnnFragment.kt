package com.team214.nctue4.course

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nctue4.R
import com.team214.nctue4.ann.AnnActivity
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.model.CourseItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_course_ann.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*

class CourseAnnFragment : Fragment() {
    lateinit var client: E3Client
    lateinit var courseItem: CourseItem
    private var disposable: Disposable? = null

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_ann, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseItem = (activity as CourseActivity).courseItem
        client = (activity as CourseActivity).client
        getData()
    }

    private fun getData() {
        disposable?.dispose()
        error_request.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
        disposable = client.getCourseAnns(courseItem)
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(mutableListOf<AnnItem>()) { annItems, annItem -> annItems.add(annItem) }
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onSuccess = {
                    it.sortByDescending { annItem -> annItem.date }
                    displayData(it)
                },
                onError = {
                    error_request.visibility = View.VISIBLE
                    error_request_retry.setOnClickListener { getData() }
                }
            )
    }

    private fun displayData(annItems: MutableList<AnnItem>) {
        if (annItems.isEmpty()) {
            empty_request.visibility = View.VISIBLE
            return
        }
        announcement_course_recycler_view.layoutManager = LinearLayoutManager(context)
        announcement_course_recycler_view.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        announcement_course_recycler_view.adapter = CourseAnnAdapter(annItems) {
            val intent = Intent()
            intent.setClass(activity!!, AnnActivity::class.java)
            intent.putExtra("annItem", it)
            startActivity(intent)
        }
        announcement_course_recycler_view.visibility = View.VISIBLE
    }
}