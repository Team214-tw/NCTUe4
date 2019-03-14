package com.team214.nctue4.ann

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.team214.nctue4.R
import com.team214.nctue4.model.FileItem
import com.team214.nctue4.utility.FileNameToColor
import com.team214.nctue4.utility.FileNameToIcon
import kotlinx.android.synthetic.main.item_announcement_attach.view.*


class AnnAttachmentAdapter(
    val context: Context, val dataSet: MutableList<FileItem>,
    private val itemClickListener: (FileItem) -> Unit
) :
    RecyclerView.Adapter<AnnAttachmentAdapter.ViewHolder>() {

    class ViewHolder(val context: Context, val view: View, private val itemClickListener: (FileItem) -> Unit) :
        RecyclerView.ViewHolder(view) {
        fun bind(attach: FileItem) {
            view.announcement_attach_name.text = attach.name
            view.announcement_attach_img.setImageResource(FileNameToIcon().getId(attach.name))
            var color = getColor(context, FileNameToColor().getId(attach.name))
            val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                hsv[2] = 1.0f - 0.1f * (1.0f - hsv[2]);
                color = Color.HSVToColor(hsv)
            }
            view.announcement_attach_img.setColorFilter(color)
            view.announcement_attach_bar.setBackgroundColor(color)
            view.announcement_attach_button?.setOnClickListener {
                itemClickListener(attach)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement_attach, parent, false)
        return ViewHolder(context, view, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size
}