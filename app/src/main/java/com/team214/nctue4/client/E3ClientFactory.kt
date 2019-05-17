package com.team214.nctue4.client

import android.content.Context
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.model.CourseItem
import com.team214.nctue4.model.HwkItem

interface E3ClientFactory {
    companion object {
        fun createFromAnn(context: Context, annItem: AnnItem): E3Client {
            return when (annItem.e3Type) {
                E3Type.NEW -> {
                    if (annItem.content != null) E3Clients.getNewE3ApiClient(context)
                    else E3Clients.getNewE3WebClient(context)
                }
                E3Type.OLD -> E3Clients.getOldE3Client(context)
            }
        }

        fun createFromCourse(context: Context, courseItem: CourseItem): E3Client {
            return when (courseItem.e3Type) {
                E3Type.NEW -> E3Clients.getNewE3ApiClient(context)
                E3Type.OLD -> E3Clients.getOldE3Client(context)
            }
        }

        fun createFromHwk(context: Context, hwkItem: HwkItem): E3Client {
            return when (hwkItem.e3Type) {
                E3Type.NEW -> E3Clients.getNewE3ApiClient(context)
                E3Type.OLD -> E3Clients.getOldE3Client(context)
            }
        }
    }
}