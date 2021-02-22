package com.team214.nycue4.main


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team214.nycue4.R
import com.team214.nycue4.utility.ThemedDialogFragment
import com.team214.nycue4.utility.openWebURL
import kotlinx.android.synthetic.main.dialog_license.*


class LicenseDialog : ThemedDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_license, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flat_icon_license?.setOnClickListener {
            openWebURL(context!!, "https://www.flaticon.com/authors/freepik")
        }
        font_awesome_license?.setOnClickListener {
            openWebURL(context!!, "https://fontawesome.com/license")
        }
    }

}
