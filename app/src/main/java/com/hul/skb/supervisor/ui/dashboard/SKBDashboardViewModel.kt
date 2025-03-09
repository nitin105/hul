package com.hul.skb.supervisor.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hul.data.Attendencemodel
import com.hul.user.UserInfo
import javax.inject.Inject

class SKBDashboardViewModel @Inject constructor(
    private val userInfo: UserInfo,
)  : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    val attendenceToday = MutableLiveData<Attendencemodel>()

    val userName: LiveData<String> = MutableLiveData(userInfo.projectName)
    val userType: LiveData<String> = MutableLiveData(userInfo.userType)
}