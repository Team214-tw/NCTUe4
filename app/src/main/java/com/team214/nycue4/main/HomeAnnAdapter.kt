package com.team214.nycue4.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.team214.nycue4.R
import com.team214.nycue4.client.E3Type
import com.team214.nycue4.model.AnnItem
import kotlinx.android.synthetic.main.item_home_announcement.view.*
import java.text.SimpleDateFormat
import java.util.*

class HomeAnnAdapter internal constructor(
    private val context: Context,
    private val itemClickListener: (AnnItem) -> Unit
) : RecyclerView.Adapter<HomeAnnAdapter.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var dataSet = emptyList<AnnItem>()

    inner class ViewHolder(
        private val view: View, private val context: Context,
        private val itemClickListener: (AnnItem) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        fun bind(ann: AnnItem) {
            view.announcement_name.text = ann.courseName
            view.announcement_title.text = ann.title
            val sdf = SimpleDateFormat("MM/dd", Locale.TAIWAN)
            view.announcement_beginDate.text = sdf.format(ann.date)
            view.setOnClickListener {
                itemClickListener(ann)
            }
            val newE3Color = ContextCompat.getColor(context, R.color.new_e3)
            val oldE3Color = ContextCompat.getColor(context, R.color.old_e3)
            view.e3_image.setImageResource(if (ann.e3Type == E3Type.NEW) R.drawable.ic_new_e3 else R.drawable.ic_old_e3)
            view.e3_image.setColorFilter(if (ann.e3Type == E3Type.NEW) newE3Color else oldE3Color)
            view.ann_identifier_bar.setBackgroundColor(if (ann.e3Type == E3Type.NEW) newE3Color else oldE3Color)
            view.announcement_beginDate.setTextColor(if (ann.e3Type == E3Type.NEW) newE3Color else oldE3Color)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = inflater.inflate(R.layout.item_home_announcement, parent, false)
        return ViewHolder(itemView, context, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    internal fun setAnnItems(annItems: List<AnnItem>) {
        this.dataSet = annItems
        notifyDataSetChanged()
    }


    override fun getItemCount() = dataSet.size
}