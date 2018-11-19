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
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.model.CourseItem
import com.team214.nctue4.model.HwkItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_assign.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*

class HwkFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_assign, container, false)
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
        disposable = client.getCourseHwk(courseItem)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { progress_bar?.visibility = View.GONE }
            .collectInto(mutableListOf<HwkItem>()) { hwkItems, hwkItem -> hwkItems.add(hwkItem) }
            .subscribeBy(
                onSuccess = { displayData(it) },
                onError = {
                    error_request.visibility = View.VISIBLE
                    error_request_retry.setOnClickListener { getData() }
                }
            )
    }

    private fun displayData(hwkItems: MutableList<HwkItem>) {
        hwkItems.sortByDescending { it.endDate }
        if (hwkItems.isEmpty()) {
            empty_request.visibility = View.VISIBLE
            return
        }
        assign_recycler_view?.layoutManager = LinearLayoutManager(context)
        assign_recycler_view?.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        assign_recycler_view?.adapter = HwkAdapter(hwkItems) {
            val intent = Intent()
            intent.setClass(activity!!, HwkActivity::class.java)
            intent.putExtra("courseItem", courseItem)
            intent.putExtra("hwkItem", it)
            startActivity(intent)
        }
    }
}