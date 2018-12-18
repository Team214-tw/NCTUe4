package com.team214.nctue4.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.team214.nctue4.R
import com.team214.nctue4.utility.FileNameToColor
import com.team214.nctue4.utility.FileNameToIcon
import kotlinx.android.synthetic.main.item_course_doc.view.*
import java.io.File

class DownloadAdapter(
    val context: Context,
    private val dataSet: ArrayList<File>,
    private val itemClickListener: (File) -> Unit,
    private val longClickListener: (File) -> Unit
) :
    RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {
    class ViewHolder(
        val context: Context, val view: View,
        private val itemClickListener: (File) -> Unit,
        private val longClickListener: (File) -> Unit
    ) :
        RecyclerView.ViewHolder(view) {
        fun bind(attach: File) {
            view.doc_display_name.text = attach.name
            view.doc_file_icon.setImageResource(FileNameToIcon().getId(attach.name))
            view.doc_file_icon.setColorFilter(ContextCompat.getColor(context, R.color.md_white_1000))
            view.doc_file_circle.setColorFilter(ContextCompat.getColor(context, FileNameToColor().getId(attach.name)))
            view.course_doc_card_layout?.setOnClickListener {
                itemClickListener(attach)
            }
            view.course_doc_card_layout?.setOnLongClickListener {
                longClickListener(attach)
                true
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_doc, parent, false)
        return ViewHolder(context, view, itemClickListener, longClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])

    }

    override fun getItemCount() = dataSet.size
}