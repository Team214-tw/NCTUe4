package com.team214.nctue4.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.team214.nctue4.AppDatabase
import com.team214.nctue4.MainApplication
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.client.E3Clients
import com.team214.nctue4.client.E3Type
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.model.CourseDBHelper
import com.team214.nctue4.utility.Event
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.mergeDelayError
import io.reactivex.rxkotlin.subscribeBy

class HomeAnnViewModel(application: Application) : AndroidViewModel(application) {
    private val remoteConfigInstance = FirebaseRemoteConfig.getInstance()
    private var disposable: Disposable? = null
    private val annDao = AppDatabase.getDatabase(application).annDao()
    val annItems = MutableLiveData<List<AnnItem>>()
    private val newE3Client: E3Client
    val loading = MutableLiveData<Boolean>()
    val error = MutableLiveData<Event<String>>()
    private val courseDBHelper = CourseDBHelper(application)

    private var newE3Failed = false

    private val useAPI = remoteConfigInstance.getBoolean("use_api_for_home_ann")

    init {
        newE3Client = E3Clients.getNewE3ApiClient(application)
        annItems.value = annDao.getAll()
        loading.value = false
        getData(false)
    }

    fun getData(forceRefresh: Boolean = true) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())
        val current = System.currentTimeMillis()
        if (current - prefs.getLong("home_ann_last_refresh", -1) < 60 * 5 * 1000 && !forceRefresh) return

        loading.value = true
        disposable?.dispose()
        val courseList = if (useAPI) courseDBHelper.readCourses(E3Type.NEW) else null

        val observables = mutableListOf(
            newE3Client.getFrontPageAnns(courseList)
                .doOnError { newE3Failed = true }
        )

        val collector = mutableListOf<AnnItem>()
        disposable = observables.mergeDelayError()
            .collectInto(collector) { c, annItem -> c.add(annItem) }
            .doFinally { loading.postValue(false) }
            .subscribeBy(
                onSuccess = {
                    annDao.deleteAll()
                    annDao.insertAll(*it.toTypedArray())
                    annItems.postValue(annDao.getAll())
                    prefs.edit().putLong("home_ann_last_refresh", current).apply()
                },
                onError = {
                    if (it is E3Client.WrongCredentialsException) {
                        error.postValue(Event(getApplication<MainApplication>().resources.getString(R.string.wrong_credential)))
                    } else {
                        if (newE3Failed) {
                            error.postValue(Event(getApplication<MainApplication>().resources.getString(R.string.ann_error)))
                        }
                    }
                    prefs.edit().putLong("home_ann_last_refresh", -1).apply()
                }
            )
    }

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }


}