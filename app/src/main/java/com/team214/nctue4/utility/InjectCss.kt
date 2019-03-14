package com.team214.nctue4.utility

import android.content.Context
import android.content.res.Configuration

fun injectCss(html: String?, context: Context): String {
    if (html == null) return ""
    val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK;
    if (currentNightMode != Configuration.UI_MODE_NIGHT_YES) return html
    return """
        <style>
            html {
                color: #FAFAFA;
            }
            a {
                color: rgb(141, 178, 215);
            }
        </style>
    """.trimIndent() + html
}