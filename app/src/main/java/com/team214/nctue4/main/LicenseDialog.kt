package com.team214.nctue4.main


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.team214.nctue4.R
import kotlinx.android.synthetic.main.dialog_license.*


class LicenseDialog : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_license, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
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
