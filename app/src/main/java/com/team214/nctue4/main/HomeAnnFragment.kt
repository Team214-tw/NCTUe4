package com.team214.nctue4.main


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.team214.nctue4.R

class HomeAnnFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (arguments?.getBoolean("home") == null) {
            activity!!.setTitle(R.string.title_ann)
        }
        return inflater.inflate(R.layout.fragment_ann, container, false)
    }


}
