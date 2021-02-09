package com.team214.nctue4.client

import android.content.Context


abstract class E3Clients {

    companion object {
        @Volatile
        private var NEW_E3_API_INSTANCE: NewE3ApiClient? = null

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
    }
}