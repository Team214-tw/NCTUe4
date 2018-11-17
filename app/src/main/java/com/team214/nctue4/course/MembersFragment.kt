package com.team214.nctue4.course

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.model.CourseItem
import com.team214.nctue4.model.MemberItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_members.*
import kotlinx.android.synthetic.main.item_member.view.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*

class MembersFragment : Fragment() {
    lateinit var client: E3Client
    lateinit var courseItem: CourseItem
    private var disposable: Disposable? = null
    private var memberItems = mutableListOf<MemberItem>()

    override fun onDestroy() {
        activity!!.findViewById<FloatingActionButton>(R.id.course_fab).visibility = View.GONE
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseItem = (activity as CourseActivity).courseItem
        client = (activity as CourseActivity).client
        getData()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.select_all, menu)
    }

    private var selectAll = false
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.action_select_all -> {
                if (course_member_recycler_view.adapter != null) {
                    if (!selectAll) {
                        multiSelect = true
                        selectCnt = memberItems.size
                        memberItems.forEach { it.selected = true }
                    } else {
                        multiSelect = false
                        selectCnt = 0
                        memberItems.forEach { it.selected = false }
                    }
                    selectAll = !selectAll
                    course_member_recycler_view.adapter!!.notifyDataSetChanged()
                    setupFab()
                } else {
                    Toast.makeText(context!!, R.string.wait, Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun getData() {
        disposable?.dispose()
        error_request.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
        memberItems.clear()
        disposable = client.getMembers(courseItem)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(memberItems) { memberItems, memberItem -> memberItems.add(memberItem) }
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onSuccess = { displayData() },
                onError = {
                    error_request.visibility = View.VISIBLE
                    error_request_retry.setOnClickListener { getData() }
                }
            )
    }

    private var selectCnt = 0
    private var multiSelect = false
    private var lstIndex: Int = -1
    private fun displayData() {
        if (memberItems.isEmpty()) {
            empty_request.visibility = View.VISIBLE
            return
        }
        memberItems.sortBy { it.type }
        course_member_recycler_view?.layoutManager = LinearLayoutManager(context)
        course_member_recycler_view?.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        course_member_recycler_view?.adapter = MembersAdapter(context!!,
            memberItems, fun(view: View, member: MemberItem, position: Int) {
                if (member.selected) selectCnt-- else selectCnt++
                if (!multiSelect && lstIndex != -1 && lstIndex != position) {
                    memberItems[lstIndex].selected = false
                    selectCnt--
                    course_member_recycler_view.adapter?.notifyItemChanged(lstIndex)
                }
                lstIndex = position
                if (selectCnt == 0) {
                    multiSelect = false
                    lstIndex = -1
                } else if (selectCnt > 1) multiSelect = true
                member.selected = !member.selected
                view.member_item.setBackgroundColor(
                    if (member.selected) {
                        ContextCompat.getColor(context!!, R.color.md_grey_300)
                    } else {
                        Color.parseColor("#ffffff")
                    }
                )
                setupFab()

            }, fun(view: View, member: MemberItem) {
                multiSelect = true
                if (member.selected) selectCnt-- else selectCnt++
                member.selected = !member.selected
                view.member_item.setBackgroundColor(
                    if (member.selected)
                        ContextCompat.getColor(context!!, R.color.md_grey_300)
                    else Color.parseColor("#ffffff")
                )
                multiSelect = selectCnt != 0
                setupFab()
            })
    }

    private fun setupFab() {
        val fab = activity!!.findViewById<FloatingActionButton>(R.id.course_fab)
        if (selectCnt > 0) {
            if (fab.visibility != View.VISIBLE) {
                fab.show()
                fab.setImageDrawable(
                    ContextCompat.getDrawable(context!!, R.drawable.ic_email_black_24dp)
                )
                fab.setOnClickListener {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.type = "text/plain"
                    var emailUri = "mailto: "
                    memberItems.filter { it.selected && it.email != "" }.forEach {
                        emailUri += it.email + ","
                    }
                    emailUri += "?subject=${courseItem.courseName}"
                    intent.data = Uri.parse(emailUri)
                    startActivity(intent)
                }
            }
        } else fab.hide()

    }

}