package com.team214.nctue4.course

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
import com.team214.nctue4.model.FolderItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_course_folder.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*

class FolderFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_course_folder, container, false)
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
        val folderType = arguments!!.getSerializable("folderType") as FolderItem.Type
        disposable = client.getCourseFolders(courseItem, folderType)
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(mutableListOf<FolderItem>()) { folderItems, folderItem -> folderItems.add(folderItem) }
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onSuccess = {
                    it.sortByDescending { folderItem -> folderItem.timeModified }
                    displayData(it)
                },
                onError = {
                    error_request.visibility = View.VISIBLE
                    error_request_retry.setOnClickListener { getData() }
                }
            )
    }

    private fun displayData(folderItems: MutableList<FolderItem>) {
        if (folderItems.size == 0) {
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
        course_folder_list_recycler_view?.adapter = FolderAdapter(context!!, folderItems) {
            val dialog = FileDialog()
            dialog.arguments = Bundle().apply {
                this.putParcelable("folderItem", it)
            }
            dialog.show(fragmentManager!!, "FileDialog")
        }
        course_folder_list_recycler_view?.visibility = View.VISIBLE
    }
}