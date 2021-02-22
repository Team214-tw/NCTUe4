package com.team214.nycue4.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.team214.nycue4.R
import com.team214.nycue4.model.FolderItem
import kotlinx.android.synthetic.main.activity_course.*
import kotlinx.android.synthetic.main.fragment_course_folder_viewpager.*

class FolderViewPager : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_folder_viewpager, container, false)
    }

    override fun onStart() {
        activity!!.toolbar_tab_layout?.visibility = View.VISIBLE
        super.onStart()
    }

    override fun onStop() {
        activity!!.toolbar_tab_layout?.visibility = View.GONE
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model = activity!!.run {
            ViewModelProviders.of(this).get(FolderViewModel::class.java)
        }
        model.getData(context!!, (activity as CourseActivity).courseItem)

        val handoutFragmentBundle = Bundle()
        val referenceFragmentBundle = Bundle()
        handoutFragmentBundle.putSerializable("folderType", FolderItem.Type.Handout)
        referenceFragmentBundle.putSerializable("folderType", FolderItem.Type.Reference)
        val fragments = listOf(FolderFragment(), FolderFragment())
        fragments[0].arguments = handoutFragmentBundle
        fragments[1].arguments = referenceFragmentBundle

        course_folder_view_pager.adapter = FolderViewPagerAdapter(
            activity!!.supportFragmentManager,
            fragments,
            listOf(getString(R.string.course_folder_type_handout), getString(R.string.course_folder_type_reference))
        )
        activity!!.toolbar_tab_layout.setupWithViewPager(course_folder_view_pager)
    }

}