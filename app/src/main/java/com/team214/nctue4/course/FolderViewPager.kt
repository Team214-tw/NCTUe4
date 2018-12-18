package com.team214.nctue4.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.team214.nctue4.R
import com.team214.nctue4.model.FolderItem
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_course.*
import kotlinx.android.synthetic.main.fragment_course_folder_viewpager.*

class FolderViewPager : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_folder_viewpager, container, false)
    }

    private var disposable: Disposable? = null

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
        val handoutFragmentBundle = Bundle()
        val referenceFragmentBundle = Bundle()
        handoutFragmentBundle.putSerializable("folderType", FolderItem.Type.Handout)
        referenceFragmentBundle.putSerializable("folderType", FolderItem.Type.Reference)
        val fragments = listOf(FolderFragment(), FolderFragment())
        fragments[0].arguments = handoutFragmentBundle
        fragments[1].arguments = referenceFragmentBundle
        val adapter = FolderViewPagerAdapter(
            activity!!.supportFragmentManager,
            fragments,
            listOf(
                getString(R.string.course_folder_type_handout),
                getString(R.string.course_folder_type_reference)
            )
        )
        val client = (activity as CourseActivity).client
        val courseItem = (activity as CourseActivity).courseItem
        disposable = client.prepareCourseFolders(courseItem).subscribe()
        course_folder_view_pager.adapter = adapter
        activity!!.toolbar_tab_layout.setupWithViewPager(course_folder_view_pager)
    }
}