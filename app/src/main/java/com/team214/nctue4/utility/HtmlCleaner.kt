package com.team214.nctue4.utility

fun htmlCleaner(body: String): String {
    var result = body.replace("<!--[\\S\\s]*?-->".toRegex(), "")
    result = result.replace("[0-9a-zA-Z.#]+ [{][\\s\\S]*?[}]".toRegex(), "")
    return result
}
