package com.team214.nycue4.course

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nycue4.R
import com.team214.nycue4.ann.AnnActivity
import com.team214.nycue4.client.E3Client
import com.team214.nycue4.client.E3Type
import com.team214.nycue4.model.AnnItem
import com.team214.nycue4.model.CourseItem
import com.team214.nycue4.utility.openWebURL
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
    private var syllabusURL: String? = null

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_course_ann, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (courseItem.e3Type != E3Type.NEW) return
        Regex("(\\d{3})(\\d)\\.(\\d+)").find(courseItem.additionalInfo)?.apply {
            val acy = groupValues[1]
            val sem = groupValues[2]
            val crsNo = groupValues[3]
            syllabusURL = "http://timetable.nctu.edu.tw/?r=main/crsoutline&Acy=$acy&Sem=$sem&CrsNo=$crsNo&lang=zh-tw"
            inflater.inflate(R.menu.to_syllabus, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_to_syllabus -> {
                syllabusURL?.apply { openWebURL(context!!, this) }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
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