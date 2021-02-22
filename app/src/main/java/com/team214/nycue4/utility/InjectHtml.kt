package com.team214.nycue4.utility

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue

fun injectHtml(html: String?, context: Context): String {
    if (html == null) return ""
    val autoLinkScript = """
        <script type="text/javascript" src=" https://cdnjs.cloudflare.com/ajax/libs/autolinker/3.0.5/Autolinker.min.js"></script>
        <script type="text/javascript">
            var autolinker = new Autolinker( { stripPrefix: false } );
            document.addEventListener("DOMContentLoaded", function () {
                document.documentElement.innerHTML = autolinker.link( document.documentElement.innerHTML );
            });
        </script>
    """.trimIndent()
    val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
        return """
        <style>
            * {
                word-wrap: break-word;
            }
        </style>
        """.trimIndent() + html + autoLinkScript
    }
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
            * {
                word-wrap: break-word;
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
                    var flag = false;
                    var bgColor = tinycolor("$bgColor");
                    if (currentNode.style.backgroundColor !== "") {
                        bgColor = tinycolor(currentNode.style.backgroundColor);
                        flag = true;
                    }
                    var color = tinycolor("#FAFAFA");
                    if (currentNode.style.color !== "") {
                        var color = tinycolor(currentNode.style.color);
                        flag = true;
                    }
                    if (flag) {
                        var modifyCnt = 0;  // fail safe
                        while (tinycolor.readability(color, bgColor) < 5 && modifyCnt < 10) {
                            color = bgColor.isDark() ? color.brighten() :  color.darken() ;
                            modifyCnt += 1;
                        }
                        currentNode.style.color = color;
                    }
                }
            })
        </script>
    """.trimIndent() + autoLinkScript
}