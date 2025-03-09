package com.hul.sb.supervisor.ui.form

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.hul.data.ProjectInfo
import com.hul.user.UserInfo
import javax.inject.Inject

class SupervisorFormViewModel @Inject constructor(
    private val userInfo: UserInfo,
)  : ViewModel() {

    var projectInfo = MutableLiveData<ProjectInfo>()

    var longitude = MutableLiveData<String>()
    var lattitude = MutableLiveData<String>()

    var imageUrl1 = MutableLiveData<String>("")

    var imageApiUrl1 = MutableLiveData<String>("")

    var imageUrl2 = MutableLiveData<String>("")

    var imageUrl3 = MutableLiveData<String>("")

    var imageApiUrl2 = MutableLiveData<String>("")


    val loginEnabled = MediatorLiveData<Boolean>(true)

    val captured2Visibility = MutableLiveData<Int>(View.GONE)

    init {

        loginEnabled.addSource(imageUrl1) {
            loginEnabled.value = it.length > 0 &&
                    imageUrl2.value!!.length > 0
        }

        loginEnabled.addSource(imageUrl2) {
            loginEnabled.value = it.length > 0 &&
                    imageUrl1.value!!.length > 0
        }

    }

    val capture1Visibility: LiveData<Int> = imageUrl1.map {
        if(it.length>0)
        {
            View.GONE
        }
        else{
            View.VISIBLE
        }
    }

    val captured1Visibility: LiveData<Int> = capture1Visibility.map {
        if(it == View.GONE)
        {
            View.VISIBLE
        }
        else{
            View.GONE
        }
    }

    val capture2Visibility = MutableLiveData<Int>(View.VISIBLE)
    val capturing2Visibility = MutableLiveData<Int>(View.GONE)

}