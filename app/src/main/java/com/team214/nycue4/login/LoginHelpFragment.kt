package com.team214.nycue4.login


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team214.nycue4.R
import com.team214.nycue4.utility.ThemedDialogFragment


class LoginHelpFragment : ThemedDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_help, container, false)
    }


}
