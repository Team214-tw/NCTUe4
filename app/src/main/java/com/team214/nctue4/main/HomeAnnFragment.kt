package com.team214.nctue4.main


import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.team214.nctue4.R
import com.team214.nctue4.ann.AnnActivity
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.client.NewE3WebClient
import com.team214.nctue4.client.OldE3Client
import com.team214.nctue4.login.LoginActivity
import com.team214.nctue4.model.AnnItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.mergeDelayError
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_ann.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_empty_compact.*
import kotlinx.android.synthetic.main.status_error.*
import kotlinx.android.synthetic.main.status_wrong_credential.*

class HomeAnnFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var oldE3Client: OldE3Client
    private lateinit var newE3WebClient: NewE3WebClient
    private var disposable: Disposable? = null
    private var fromHome: Boolean = false
    private var oldE3Failed = false
    private var newE3Failed = false
    private val annItems = mutableListOf<AnnItem>()

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fromHome = arguments?.getBoolean("home") != null
        if (!fromHome) activity!!.setTitle(R.string.title_ann)
        return inflater.inflate(R.layout.fragment_ann, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = RecyclerView(context!!)
        recyclerView.layoutParams =
            RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        ann_swipe_refresh_layout.isEnabled = false
        if (!fromHome) {
            ann_swipe_refresh_layout.visibility = View.VISIBLE
            ann_swipe_refresh_layout.addView(recyclerView)
            ann_swipe_refresh_layout.setOnRefreshListener {
                ann_swipe_refresh_layout.isRefreshing = true
                getData()
            }
        } else {
            ann_root.addView(recyclerView)
        }
        newE3WebClient = (activity as MainActivity).newE3WebClient
        oldE3Client = (activity as MainActivity).oldE3Client
        getData()
    }

    private fun getData() {
        error_request.visibility = View.GONE
        error_wrong_credential.visibility = View.GONE
        if (!ann_swipe_refresh_layout.isRefreshing) {
            progress_bar.visibility = View.VISIBLE
        }
        disposable?.dispose()
        annItems.clear()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val observables = mutableListOf(
            newE3WebClient.getFrontPageAnns()
                .doOnError { newE3Failed = true }
        )
        val enableOldE3 = prefs.getBoolean("ann_enable_old_e3", false)
        if (enableOldE3) {
            observables.add(oldE3Client.getFrontPageAnns()
                .doOnError { oldE3Failed = true })
        }
        disposable = observables.mergeDelayError()
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(annItems) { annItems, annItem -> annItems.add(annItem) }
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onSuccess = {
                    annItems.sortByDescending { annItem -> annItem.date }
                    displayErrorToast()
                    displayData()
                },
                onError = { it ->
                    if (it is E3Client.WrongCredentialsException) {
                        displayWrongCredentialsError()
                    } else {
                        if (newE3Failed && oldE3Failed) {
                            displayError()
                        } else {
                            annItems.sortByDescending { it.date }
                            displayErrorToast()
                            displayData()
                        }
                    }
                }
            )

    }

    private fun displayWrongCredentialsError() {
        error_wrong_credential.visibility = View.VISIBLE
        login_again_button?.setOnClickListener {
            val intent = Intent()
            intent.setClass(context!!, LoginActivity::class.java)
            startActivity(intent)
            activity!!.finish()
        }
    }

    private fun displayErrorToast() {
        if (newE3Failed && !oldE3Failed) {
            Toast.makeText(context, getString(R.string.new_e3_ann_error), Toast.LENGTH_LONG).show()
        }
        if (!newE3Failed && oldE3Failed) {
            Toast.makeText(context, getString(R.string.old_e3_ann_error), Toast.LENGTH_LONG).show()
        }
    }

    private fun displayError() {
        error_request.visibility = View.VISIBLE
        ann_swipe_refresh_layout.visibility = View.GONE
        error_request_retry.setOnClickListener {
            getData()
        }
    }

    private fun displayData() {
        val emptyRequestView = if (arguments?.getBoolean("home") != null) empty_request_compact else empty_request
        ann_swipe_refresh_layout?.visibility = View.VISIBLE
        ann_swipe_refresh_layout.isEnabled = !fromHome
        if (annItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyRequestView.visibility = View.VISIBLE
            return
        }
        emptyRequestView.visibility = View.GONE
        if (recyclerView.adapter != null) {
            recyclerView.adapter?.notifyDataSetChanged()
            ann_swipe_refresh_layout?.isRefreshing = false
            return
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        val fromHome = arguments?.getBoolean("home") != null
        if (fromHome) recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = HomeAnnAdapter(
            if (fromHome) annItems.subList(0, minOf(5, annItems.size - 1))
            else annItems, context!!
        ) {
            val intent = Intent()
            intent.setClass(context!!, AnnActivity::class.java)
            intent.putExtra("fromHome", true)
            intent.putExtra("annItem", it)
            startActivity(intent)
        }
        recyclerView.visibility = View.VISIBLE
    }
}
