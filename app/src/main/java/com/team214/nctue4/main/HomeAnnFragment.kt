package com.team214.nctue4.main


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nctue4.R
import com.team214.nctue4.ann.AnnActivity
import kotlinx.android.synthetic.main.fragment_ann.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_empty_compact.*

class HomeAnnFragment : Fragment() {
    private lateinit var annViewModel: HomeAnnViewModel
    private var fromHome: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fromHome = arguments?.getBoolean("home") != null
        if (!fromHome) activity!!.setTitle(R.string.title_ann)
        annViewModel = ViewModelProviders.of(this).get(HomeAnnViewModel::class.java)
        return inflater.inflate(R.layout.fragment_ann, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = HomeAnnAdapter(context!!) {
            val intent = Intent()
            intent.setClass(context!!, AnnActivity::class.java)
            intent.putExtra("fromHome", true)
            intent.putExtra("annItem", it)
            startActivity(intent)
        }
        val recyclerView = if (fromHome) ann_recycler_view_out_swipe else ann_recycler_view_in_swipe
        recyclerView.visibility = View.VISIBLE
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        progress_bar.visibility = View.VISIBLE
        if (!fromHome) {
            ann_swipe_refresh_layout.visibility = View.VISIBLE
            ann_swipe_refresh_layout.setOnRefreshListener {
                ann_swipe_refresh_layout.isRefreshing = true
                refresh()
            }
        }

        annViewModel.annItems.observe(this, Observer {
            val emptyRequestView = if (arguments?.getBoolean("home") != null) empty_request_compact else empty_request
            if (it.isEmpty()) emptyRequestView.visibility = View.VISIBLE
            else emptyRequestView.visibility = View.GONE
            adapter.setAnnItems(
                if (fromHome) it.subList(0, minOf(arguments!!.getInt("home_ann_cnt", 5), it.size))
                else it
            )
        })

        annViewModel.loading.observe(this, Observer { loading ->
            if (loading) {
                progress_bar.visibility = View.VISIBLE
            } else {
                ann_swipe_refresh_layout.isRefreshing = false
                progress_bar.visibility = View.GONE
            }
        })

        annViewModel.error.observe(this, Observer {
            it.getContentIfNotHandled()?.let { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        })

    }

    fun refresh() {
        annViewModel.getData()
    }

}
