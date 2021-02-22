package com.team214.nycue4.utility

fun unescapeHtml(html: String): String {
    return html
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&amp;", "&")
}