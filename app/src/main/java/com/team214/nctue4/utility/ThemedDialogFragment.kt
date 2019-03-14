package com.team214.nctue4.utility

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.team214.nctue4.R

abstract class ThemedDialogFragment : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogFragmentTheme)
    }

}