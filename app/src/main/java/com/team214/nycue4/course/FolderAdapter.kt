package com.team214.nycue4.course

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.team214.nycue4.R
import com.team214.nycue4.model.FolderItem
import kotlinx.android.synthetic.main.item_course_folder.view.*
import java.text.SimpleDateFormat
import java.util.*

class FolderAdapter(
    private val itemClickListener: (FolderItem) -> Unit
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    private var dataSet = emptyList<FolderItem>()

    inner class ViewHolder(
        private val view: View,
        private val itemClickListener: (FolderItem) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        fun bind(folder: FolderItem) {
            view.folder_display_name.text = folder.name
            val df = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
            view.folder_modify_date.text = df.format(folder.timeModified)
            view.course_folder_list_item?.setOnClickListener {
                itemClickListener(folder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_folder, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    internal fun setFolderItems(folderItems: List<FolderItem>) {
        this.dataSet = folderItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = dataSet.size
}