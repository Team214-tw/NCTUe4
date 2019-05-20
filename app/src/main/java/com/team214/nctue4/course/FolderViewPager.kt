package com.team214.nctue4.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.team214.nctue4.R
import com.team214.nctue4.model.FolderItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_course.*
import kotlinx.android.synthetic.main.fragment_course_folder_viewpager.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_error.*

class FolderViewPager : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_folder_viewpager, container, false)
    }

    private var disposable: Disposable? = null
    private val fragments = listOf(FolderFragment(), FolderFragment())

    override fun onStart() {
        activity!!.toolbar_tab_layout?.visibility = View.VISIBLE
        super.onStart()
    }

    override fun onStop() {
        activity!!.toolbar_tab_layout?.visibility = View.GONE
        disposable?.dispose()
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = FolderViewPagerAdapter(
            activity!!.supportFragmentManager,
            fragments,
            listOf(
                getString(R.string.course_folder_type_handout),
                getString(R.string.course_folder_type_reference)
            )
        )
        course_folder_view_pager.adapter = adapter
        activity!!.toolbar_tab_layout.setupWithViewPager(course_folder_view_pager)
        getData()
    }

    fun getData() {
        val client = (activity as CourseActivity).client
        val courseItem = (activity as CourseActivity).courseItem
        disposable?.dispose()
        error_request.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
        disposable =
            client.getCourseFolders(courseItem)
                .collectInto(mutableListOf<FolderItem>()) { collector, folder -> collector.add(folder) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { progress_bar.visibility = View.GONE }
                .subscribeBy(
                    onSuccess = {
                        it.sortByDescending { folderItem -> folderItem.timeModified }
                        fragments[0].displayData(it.filter { folder -> folder.folderType == FolderItem.Type.Handout })
                        fragments[1].displayData(it.filter { folder -> folder.folderType == FolderItem.Type.Reference })

                    },
                    onError = {
                        error_request.visibility = View.VISIBLE
                        error_request_retry.setOnClickListener { getData() }
                    }
                )
    }
}