package com.team214.nctue4.main


import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.team214.nctue4.R
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity!!.setTitle(R.string.home)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private var fragment1: HomeAnnFragment? = null
    private var fragment2: DownloadFragment? = null

    override fun onStart() {
        if (fragment2 != null) {
            fragment2?.updateList(activity)
        } else {
            home_swipe_refresh?.setOnRefreshListener {
                fragment1?.refresh()
                val handler = Handler()
                handler.postDelayed({ home_swipe_refresh?.isRefreshing = false }, 1000)
            }
            loadFragments()
        }
        super.onStart()
    }

    private fun loadFragments() {
        val fragmentManager = activity!!.supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val bundle = Bundle()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        bundle.putBoolean("home", true)


        if (prefs.getBoolean("home_enable_ann", true)) {
            ann_layout.visibility = View.VISIBLE
            fragment1 = HomeAnnFragment()
            bundle.putInt("home_ann_cnt", prefs.getString("home_ann_cnt", "5")!!.toInt())
            fragment1!!.arguments = bundle
            transaction.replace(R.id.home_ann, fragment1!!)
            home_more_ann?.setOnClickListener { (activity!! as MainActivity).switchFragment(R.id.nav_ann) }
        }

        if (prefs.getBoolean("home_enable_download", true)) {
            download_layout.visibility = View.VISIBLE
            fragment2 = DownloadFragment()
            bundle.putInt("home_download_cnt", prefs.getString("home_download_cnt", "5")!!.toInt())
            fragment2!!.arguments = bundle
            transaction.replace(R.id.home_download, fragment2!!)
            home_more_download?.setOnClickListener { (activity!! as MainActivity).switchFragment(R.id.nav_download) }
        }


        if (prefs.getBoolean("home_enable_bookmarked", true)) {
            bookmarked_layout.visibility = View.VISIBLE
            val fragment3 = BookmarkedFragment()
            bundle.putInt("home_bookmarked_cnt", prefs.getString("home_bookmarked_cnt", "5")!!.toInt())
            fragment3.arguments = bundle
            transaction.replace(R.id.home_bookmarked, fragment3)
            home_more_course?.setOnClickListener { (activity!! as MainActivity).switchFragment(R.id.nav_bookmarked) }
        }

        transaction.commit()
    }

}