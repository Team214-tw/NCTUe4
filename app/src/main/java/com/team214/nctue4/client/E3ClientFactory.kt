package com.team214.nctue4.client

import android.content.Context
import com.team214.nctue4.model.AnnItem

interface E3ClientFactory {
    companion object {
        fun createFromAnn(context: Context, annItem: AnnItem): E3Client {
            return when (annItem.e3Type) {
                E3Type.NEW -> {
                    if (annItem.content != null) NewE3ApiClient(context) else NewE3WebClient(context)
                }
                E3Type.OLD -> OldE3Client(context)
            }
        }
    }
}