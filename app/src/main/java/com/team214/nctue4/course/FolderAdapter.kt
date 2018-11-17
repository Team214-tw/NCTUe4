package com.team214.nctue4.course

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.team214.nctue4.R
import com.team214.nctue4.model.FolderItem
import kotlinx.android.synthetic.main.item_course_folder.view.*

class FolderAdapter(
    private val context: Context,
    private val dataSet: MutableList<FolderItem>,
    private val itemClickListener: (FolderItem) -> Unit
) :
    RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    class ViewHolder(
        private val context: Context,
        private val view: View,
        private val itemClickListener: (FolderItem) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        fun bind(doc: FolderItem) {
            view.doc_group_display_name.text = doc.name
            view.course_doc_type.text = when (doc.folderType) {
                FolderItem.Type.Handout -> context.getString(R.string.course_doc_type_handout)
                FolderItem.Type.Reference -> context.getString(R.string.course_doc_type_reference)
            }
            view.course_doc_group_list_item?.setOnClickListener {
                itemClickListener(doc)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FolderAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_folder, parent, false)
        return ViewHolder(context, view, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])

    }

    override fun getItemCount() = dataSet.size
}