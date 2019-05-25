package com.team214.nctue4.course

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.team214.nctue4.client.E3ClientFactory
import com.team214.nctue4.model.CourseItem
import com.team214.nctue4.model.FolderItem
import com.team214.nctue4.utility.DataStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy


class FolderViewModel(application: Application) : AndroidViewModel(application) {

    val status = MutableLiveData<DataStatus>()
    val handOutItems = MutableLiveData<List<FolderItem>>()
    val referenceItems = MutableLiveData<List<FolderItem>>()
    private var disposable: Disposable? = null

    fun getData(context: Context, courseItem: CourseItem) {
        val client = E3ClientFactory.createFromCourse(context, courseItem)
        disposable?.dispose()
        status.value = DataStatus.LOADING
        disposable =
            client.getCourseFolders(courseItem)
                .collectInto(mutableListOf<FolderItem>()) { collector, folder -> collector.add(folder) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { folderList ->
                        folderList.sortByDescending { folderItem -> folderItem.timeModified }
                        referenceItems.postValue(folderList.filter { it.folderType == FolderItem.Type.Reference })
                        handOutItems.postValue(folderList.filter { it.folderType == FolderItem.Type.Handout })
                        status.postValue(DataStatus.SUCCESS)
                    },
                    onError = {
                        status.postValue(DataStatus.ERROR)
                    }
                )
    }

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }
}