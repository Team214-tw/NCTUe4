package com.team214.nycue4.course

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.team214.nycue4.R
import com.team214.nycue4.model.ScoreItem
import kotlinx.android.synthetic.main.item_score.view.*

class ScoreAdapter(private val dataSet: MutableList<ScoreItem>) :
    RecyclerView.Adapter<ScoreAdapter.ViewHolder>() {

    class ViewHolder(val view: View) :
        RecyclerView.ViewHolder(view) {
        fun bind(score: ScoreItem) {
            view.score_name.text = score.name
            view.score_score.text = score.score
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ScoreAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_score, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size
}