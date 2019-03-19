package com.team214.nctue4.utility

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue

fun injectHtml(html: String?, context: Context): String {
    if (html == null) return ""
    val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (currentNightMode != Configuration.UI_MODE_NIGHT_YES) return html
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
    val bgColor = "#" + Integer.toHexString(typedValue.data and 0x00ffffff)
    return """
        <style>
            html {
                color: #FAFAFA;
            }
            a {
                color: rgb(141, 178, 215);
            }
        </style>
        """.trimIndent() +
            html + """
        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/tinycolor/1.4.1/tinycolor.min.js"></script>
        <script type="text/javascript">
            document.addEventListener("DOMContentLoaded", function () {
                var currentNode;
                var ni = document.createNodeIterator(document.body, NodeFilter.SHOW_ELEMENT);
                while (currentNode = ni.nextNode()) {
                    if (currentNode.style.color != "") {
                        var color = tinycolor(currentNode.style.color);
                        while (tinycolor.readability(color, "$bgColor") < 5) {
                            color = color.brighten();
                        }
                        currentNode.style.color = color;
                    }
                }
            })
        </script>
    """.trimIndent()
}