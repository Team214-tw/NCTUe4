package com.team214.nctue4.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.team214.nctue4.AppDatabase
import com.team214.nctue4.MainApplication
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.client.E3Clients
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.utility.Event
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.mergeDelayError
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class HomeAnnViewModel(application: Application) : AndroidViewModel(application) {

    private var disposable: Disposable? = null
    private val annDao = AppDatabase.getDatabase(application).annDao()
    val annItems = MutableLiveData<List<AnnItem>>()
    private val newE3Client = E3Clients.getNewE3WebClient(application)
    private val oldE3Client = E3Clients.getOldE3Client(application)
    val loading = MutableLiveData<Boolean>()
    val error = MutableLiveData<Event<String>>()

    private var oldE3Failed = false
    private var newE3Failed = false

    init {
        Observable.fromCallable {
            annItems.postValue(annDao.getAll())
        }.subscribeOn(Schedulers.io()).subscribe()
        getData()
    }

    fun getData() {
        loading.value = true
        disposable?.dispose()
        val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())

        val observables = mutableListOf(
            newE3Client.getFrontPageAnns()
                .doOnError { newE3Failed = true }
        )
        val enableOldE3 = prefs.getBoolean("ann_enable_old_e3", false)
        if (enableOldE3) {
            observables.add(oldE3Client.getFrontPageAnns()
                .doOnError { oldE3Failed = true })
        }

        val collector = mutableListOf<AnnItem>()
        disposable = observables.mergeDelayError()
            .collectInto(collector) { c, annItem -> c.add(annItem) }
            .doFinally { loading.postValue(false) }
            .subscribeBy(
                onSuccess = {
                    annDao.deleteAll()
                    annDao.insertAll(*it.toTypedArray())
                    annItems.postValue(annDao.getAll())
                },
                onError = {
                    if (it is E3Client.WrongCredentialsException) {
                        error.postValue(Event(getApplication<MainApplication>().resources.getString(R.string.wrong_credential)))
                    } else {
                        if (newE3Failed && oldE3Failed) {
                            error.postValue(Event(getApplication<MainApplication>().resources.getString(R.string.ann_error)))
                        } else if (newE3Failed) {
                            annDao.deleteAllOldE3()
                            annDao.insertAll(*collector.toTypedArray())
                            annItems.postValue(annDao.getAll())
                            error.postValue(Event(getApplication<MainApplication>().resources.getString(R.string.new_e3_ann_error)))
                        } else if (oldE3Failed) {
                            annDao.deleteAllNewE3()
                            annDao.insertAll(*collector.toTypedArray())
                            annItems.postValue(annDao.getAll())
                            error.postValue(Event(getApplication<MainApplication>().resources.getString(R.string.old_e3_ann_error)))
                        }
                    }
                }
            )
    }

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }


}