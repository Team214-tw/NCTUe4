package com.team214.nctue4.utility

fun unescapeHtml(html: String): String {
    return html
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&amp;", "&")
}