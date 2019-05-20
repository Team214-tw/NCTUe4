package com.team214.nctue4.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nctue4.R
import com.team214.nctue4.model.FolderItem
import kotlinx.android.synthetic.main.fragment_course_folder.*
import kotlinx.android.synthetic.main.status_empty.*

class FolderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_folder, container, false)
    }

    fun displayData(folderItems: List<FolderItem>) {
        if (folderItems.isEmpty()) {
            empty_request.visibility = View.VISIBLE
            return
        }
        course_folder_list_recycler_view?.layoutManager = LinearLayoutManager(context)
        course_folder_list_recycler_view?.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        course_folder_list_recycler_view?.adapter = FolderAdapter(folderItems) {
            val dialog = FileDialog()
            dialog.arguments = Bundle().apply {
                this.putParcelable("folderItem", it)
            }
            dialog.show(fragmentManager!!, "FileDialog")
        }
        course_folder_list_recycler_view?.visibility = View.VISIBLE
    }
}