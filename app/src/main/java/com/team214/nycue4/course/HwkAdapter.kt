package com.team214.nycue4.course


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.team214.nycue4.R
import com.team214.nycue4.model.HwkItem
import kotlinx.android.synthetic.main.item_assign.view.*
import java.text.SimpleDateFormat
import java.util.*

class HwkAdapter(
    private val dataSet: MutableList<HwkItem>,
    private val itemClickListener: (HwkItem) -> Unit
) :
    RecyclerView.Adapter<HwkAdapter.ViewHolder>() {

    class ViewHolder(val view: View, private val itemClickListener: (HwkItem) -> Unit) :
        RecyclerView.ViewHolder(view) {
        private val df = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
        fun bind(assign: HwkItem) {
            view.assign_name.text = assign.name
            view.assign_start.text = df.format(assign.startDate)
            view.assign_end.text = df.format(assign.endDate)
            view.assign_item.setOnClickListener { itemClickListener(assign) }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HwkAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assign, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size
}