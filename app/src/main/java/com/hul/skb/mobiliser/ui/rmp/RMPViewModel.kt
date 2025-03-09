package com.hul.skb.mobiliser.ui.rmp

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.hul.data.GetVisitDataResponseData
import com.hul.data.ProjectInfo
import com.hul.data.SchoolCode
import com.hul.data.Visit1
import com.hul.user.UserInfo
import javax.inject.Inject

class RMPViewModel @Inject constructor(
    private val userInfo: UserInfo,
)  : ViewModel() {

    var selectedSchoolCode = MutableLiveData<SchoolCode>()

    var projectInfo = MutableLiveData<ProjectInfo>()

    var position = MutableLiveData<Int>(0)

    var longitude = MutableLiveData<String>()
    var lattitude = MutableLiveData<String>()

    var imageUrl1 = MutableLiveData<String>("")
    var imageUrl2 = MutableLiveData<String>("")
    
    var nameOfRMP = MutableLiveData<String>("")
    var contactNumberOfRMP = MutableLiveData<String>("")

    val loginEnabled = MediatorLiveData<Boolean>(true)

    init {

//        form4Text.addSource(form4) { value ->
//            form4Text.value = if (value) "Yes" else "No"
//        }

        loginEnabled.addSource(imageUrl1) {
            loginEnabled.value = it.length > 0 && imageUrl2.value!!.length > 0 && nameOfRMP.value!!.length > 0 && contactNumberOfRMP.value!!.length > 0 
        }

        loginEnabled.addSource(imageUrl2) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && nameOfRMP.value!!.length > 0 && contactNumberOfRMP.value!!.length > 0 
        }
        

        loginEnabled.addSource(nameOfRMP) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && contactNumberOfRMP.value!!.length > 0 
        }

        loginEnabled.addSource(contactNumberOfRMP) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && nameOfRMP.value!!.length > 0 
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

    val capture2Visibility: LiveData<Int> = imageUrl2.map {
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

    val captured2Visibility: LiveData<Int> = capture2Visibility.map {
        if(it == View.GONE)
        {
            View.VISIBLE
        }
        else{
            View.GONE
        }
    }

    val nameOfRMPError: LiveData<String> = nameOfRMP.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter name"
        }
    }

    val contactNumberOfRMPError: LiveData<String> = contactNumberOfRMP.map {

        if(it.length > 0 && it.length == 10)
        {
            ""
        }
        else if(it.length==0){
            "Enter mobile number"
        }
        else{
            "Enter correct mobilr number"
        }
    }


    var visitData = MutableLiveData<GetVisitDataResponseData>(null)
    var visitDataToView = MutableLiveData<Visit1>(null)

    val booksHandedOver = MutableLiveData<Int>()

    val booksDistributed = MutableLiveData<Int>()

    val videoShown = MutableLiveData<Int>()

    val booksDistributedFlag = MutableLiveData<Boolean>(null)

    val videoShownFlag = MutableLiveData<Boolean>(null)

    val revisitApplicable = MutableLiveData<Int>()

    val revisitApplicableFlag = MutableLiveData<Boolean>(false)

}