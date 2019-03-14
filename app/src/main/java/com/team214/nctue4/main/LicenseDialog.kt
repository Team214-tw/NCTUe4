package com.team214.nctue4.main


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team214.nctue4.R
import com.team214.nctue4.utility.ThemedDialogFragment
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
            goToUrl("https://www.flaticon.com/authors/freepik")
        }
        font_awesome_license?.setOnClickListener {
            goToUrl("https://fontawesome.com/license")
        }
    }

    private fun goToUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

}
