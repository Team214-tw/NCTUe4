package com.team214.nctue4.main

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nctue4.R
import com.team214.nctue4.utility.openFile
import kotlinx.android.synthetic.main.fragment_download.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_empty_compact.*
import java.io.File

class DownloadFragment : Fragment() {
    private var fromHome: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fromHome = arguments?.getBoolean("home") != null
        if (!fromHome) activity!!.setTitle(R.string.download_history)
        return inflater.inflate(R.layout.fragment_download, container, false)
    }


    private var files = ArrayList<File>()
    private lateinit var emptyRequest: View
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyRequest = if (fromHome) empty_request_compact else empty_request
        updateList()

    }

    fun updateList(homeActivity: FragmentActivity? = null) {
        val path = if (homeActivity != null) homeActivity.getExternalFilesDir(null) else
            activity!!.getExternalFilesDir(null)
        val dir = File(path, "Download")
        val fileList = dir.listFiles()
        files.clear()
        if (dir.exists() && !fileList.isEmpty()) {
            download_recycler.visibility = View.VISIBLE
            emptyRequest.visibility = View.GONE
            fileList.sortByDescending { it.lastModified() }
            if (fromHome) {
                files.addAll(fileList.filter { it != null }.take(5))
            } else files.addAll(fileList.filter { it != null })
            if (download_recycler.adapter == null) {
                download_recycler?.layoutManager = LinearLayoutManager(context)
                if (fromHome)
                    download_recycler?.isNestedScrollingEnabled = false
                download_recycler?.addItemDecoration(
                    DividerItemDecoration(
                        context,
                        LinearLayoutManager.VERTICAL
                    )
                )
                download_recycler?.adapter = DownloadAdapter(context!!, files,
                    fun(it) {
                        openFile(it.name, it, context!!, activity!!)
                    },
                    fun(it) {
                        val dialog = DownloadDialog()
                        dialog.setOnDismissListener(DialogInterface.OnDismissListener { updateList() })
                        val bundle = Bundle()
                        bundle.putSerializable("file", it)
                        dialog.arguments = bundle
                        dialog.show(fragmentManager, "TAG")

                    })
            } else {
                download_recycler?.adapter?.notifyDataSetChanged()
            }

        } else {
            download_recycler?.visibility = View.GONE
            emptyRequest.visibility = View.VISIBLE
        }
    }

}