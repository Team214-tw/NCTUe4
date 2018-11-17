package com.team214.nctue4.course

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.team214.nctue4.R
import com.team214.nctue4.model.MemberItem
import kotlinx.android.synthetic.main.item_member.view.*

class MembersAdapter(
    val context: Context, private val dataSet: MutableList<MemberItem>,
    private val itemClickListener: (View, MemberItem, Int) -> Unit,
    private val longClickListener: (View, MemberItem) -> Unit
) :
    RecyclerView.Adapter<MembersAdapter.ViewHolder>() {

    class ViewHolder(
        val context: Context, val view: View,
        private val itemClickListener: (View, MemberItem, Int) -> Unit,
        private val longClickListener: (View, MemberItem) -> Unit
    ) :
        RecyclerView.ViewHolder(view) {
        fun bind(member: MemberItem, position: Int) {
            view.member_name.text = member.name
            view.member_department.text = member.department
            view.member_email.text = if (member.email == "") context.getString(R.string.no_email) else member.email
            view.course_member_thumb.setColorFilter(
                when (member.type) {
                    MemberItem.Type.Teacher -> ContextCompat.getColor(context, R.color.md_orange_700)
                    MemberItem.Type.TA -> ContextCompat.getColor(context, R.color.md_green_700)
                    MemberItem.Type.Audit -> ContextCompat.getColor(context, R.color.md_indigo_700)
                    MemberItem.Type.Student -> ContextCompat.getColor(context, R.color.md_blue_700)
                }
            )
            view.member_item.setBackgroundColor(
                if (member.selected) ContextCompat.getColor(context, R.color.md_grey_300)
                else Color.parseColor("#ffffff")
            )
            view.member_item.setOnClickListener { itemClickListener(view, member, position) }
            view.member_item.setOnLongClickListener {
                longClickListener(view, member)
                true
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MembersAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return ViewHolder(context, view, itemClickListener, longClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position], position)

    }

    override fun getItemCount() = dataSet.size
}