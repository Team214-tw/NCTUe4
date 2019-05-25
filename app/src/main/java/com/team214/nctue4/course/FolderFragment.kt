package com.team214.nctue4.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nctue4.R
import com.team214.nctue4.model.FolderItem
import com.team214.nctue4.utility.DataStatus
import kotlinx.android.synthetic.main.fragment_course_folder.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*

class FolderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = activity!!.run {
            ViewModelProviders.of(this).get(FolderViewModel::class.java)
        }

        val adapter = FolderAdapter {
            val dialog = FileDialog()
            dialog.arguments = Bundle().apply {
                this.putParcelable("folderItem", it)
            }
            dialog.show(fragmentManager!!, "FileDialog")
        }

        course_folder_list_recycler_view.layoutManager = LinearLayoutManager(context)
        course_folder_list_recycler_view.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )
        course_folder_list_recycler_view.adapter = adapter

        error_request_retry.setOnClickListener {
            model.getData(context!!, (activity as CourseActivity).courseItem)
        }

        model.status.observe(this, Observer { status ->
            if (status == DataStatus.LOADING) {
                progress_bar.visibility = View.VISIBLE
            } else {
                progress_bar.visibility = View.GONE
            }

            if (status == DataStatus.SUCCESS) {
                course_folder_list_recycler_view.visibility = View.VISIBLE
            } else {
                course_folder_list_recycler_view.visibility = View.INVISIBLE
            }

            if (status == DataStatus.ERROR) {
                error_request.visibility = View.VISIBLE
            } else {
                error_request.visibility = View.GONE
            }

            if (adapter.itemCount == 0 && model.status.value == DataStatus.SUCCESS) {
                empty_request.visibility = View.VISIBLE
            } else {
                empty_request.visibility = View.GONE
            }
        })


        val dataSource = when (arguments!!.getSerializable("folderType") as FolderItem.Type) {
            FolderItem.Type.Handout -> model.handOutItems
            FolderItem.Type.Reference -> model.referenceItems
        }

        dataSource.observe(this, Observer { folderItems ->
            if (folderItems.isEmpty() && model.status.value == DataStatus.SUCCESS) empty_request.visibility = View.VISIBLE
            else empty_request.visibility = View.GONE
            adapter.setFolderItems(folderItems)
        })
    }

}