package com.team214.nctue4.course

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.model.FileItem
import com.team214.nctue4.model.FolderItem
import com.team214.nctue4.utility.downloadFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_course_folder.*

class FileDialog : DialogFragment() {
    private lateinit var client: E3Client
    private lateinit var folderItem: FolderItem
    private lateinit var url: String
    private lateinit var fileName: String
    private var disposable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        client = (activity as CourseActivity).client
        folderItem = arguments!!.getParcelable("folderItem")!!
        return inflater.inflate(R.layout.dialog_course_folder, container, false)
    }

    override fun onStop() {
        disposable?.dispose()
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        getData()
    }

    private fun getData() {
        disposable = client.getFiles(folderItem)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(mutableListOf<FileItem>()) { fileItems, fileItem -> fileItems.add(fileItem) }
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onSuccess = { displayData(it) },
                onError = {
                    if (!(context as Activity).isFinishing) {
                        Toast.makeText(context, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
                    }
                    dismissAllowingStateLoss()
                }
            )
    }

    private fun displayData(fileItems: MutableList<FileItem>) {
        course_doc_dialog_recycler_view.layoutManager = LinearLayoutManager(context)
        course_doc_dialog_recycler_view.adapter = FileAdapter(context!!, fileItems) {
            url = it.url
            fileName = it.name
            downloadFile(fileName, url, context!!, activity!!, activity!!.findViewById(R.id.container)) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    0
                )
            }
            if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            0 -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    downloadFile(
                        fileName,
                        url,
                        context!!,
                        activity!!,
                        activity!!.findViewById(R.id.container),
                        null,
                        null
                    )
                    dismissAllowingStateLoss()
                }
                return
            }
        }
    }
}