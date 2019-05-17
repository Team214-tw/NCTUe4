package com.team214.nctue4.client

import android.content.Context


abstract class E3Clients {

    companion object {
        @Volatile
        private var NEW_E3_WEB_INSTANCE: NewE3WebClient? = null
        @Volatile
        private var NEW_E3_API_INSTANCE: NewE3ApiClient? = null
        @Volatile
        private var OLD_E3_INSTANCE: OldE3Client? = null

        fun getNewE3WebClient(context: Context): NewE3WebClient {
            val tempInstance = NEW_E3_WEB_INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = NewE3WebClient(context.applicationContext)
                NEW_E3_WEB_INSTANCE = instance
                return instance
            }
        }

        fun getNewE3ApiClient(context: Context): NewE3ApiClient {
            val tempInstance = NEW_E3_API_INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = NewE3ApiClient(context.applicationContext)
                NEW_E3_API_INSTANCE = instance
                return instance
            }
        }

        fun getOldE3Client(context: Context): OldE3Client {
            val tempInstance = OLD_E3_INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = OldE3Client(context.applicationContext)
                OLD_E3_INSTANCE = instance
                return instance
            }
        }
    }
}