package com.hul.skb.mobiliser.ui.scp

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

class SCPViewModel  @Inject constructor(
    private val userInfo: UserInfo,
)  : ViewModel() {

    var selectedSchoolCode = MutableLiveData<SchoolCode>()

    var projectInfo = MutableLiveData<ProjectInfo>()

    var position = MutableLiveData<Int>(0)

    var longitude = MutableLiveData<String>()
    var lattitude = MutableLiveData<String>()

    var imageUrl1 = MutableLiveData<String>("")
    var imageUrl2 = MutableLiveData<String>("")
    var imageUrl3 = MutableLiveData<String>("")
    var imageUrl4 = MutableLiveData<String>("")
    var imageUrl5 = MutableLiveData<String>("")

    var pyramidActivity = MutableLiveData<Boolean>(false)
    var ekKatoriToolKit = MutableLiveData<Boolean>(false)
    var giveAway = MutableLiveData<Boolean>(false)

    var totalNumberOfStudents = MutableLiveData<String>("")
    var totalNoOfGivawayDistributed = MutableLiveData<String>("")
    var nameOfPrincipal = MutableLiveData<String>("")
    var contactNumberOfPrincipal = MutableLiveData<String>("")

    val loginEnabled = MediatorLiveData<Boolean>(true)

    init {

//        form4Text.addSource(form4) { value ->
//            form4Text.value = if (value) "Yes" else "No"
//        }

        loginEnabled.addSource(imageUrl1) {
            loginEnabled.value = it.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0 && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(imageUrl2) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0 && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(imageUrl3) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0 && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(imageUrl4) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0 && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!!
        }

        loginEnabled.addSource(imageUrl5) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0 && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!!
        }

        loginEnabled.addSource(totalNumberOfStudents) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0 && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(totalNoOfGivawayDistributed) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(nameOfPrincipal) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0   && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(contactNumberOfPrincipal) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0   && nameOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(pyramidActivity) {
            loginEnabled.value = it && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0  && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0   && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && giveAway.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(giveAway) {
            loginEnabled.value = it && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0  && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0   && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && ekKatoriToolKit.value!! 
        }

        loginEnabled.addSource(ekKatoriToolKit) {
            loginEnabled.value = it && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0  && totalNumberOfStudents.value!!.length > 0 && totalNoOfGivawayDistributed.value!!.length > 0   && nameOfPrincipal.value!!.length > 0 && contactNumberOfPrincipal.value!!.length > 0 && pyramidActivity.value!! && giveAway.value!! 
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

    val capture3Visibility: LiveData<Int> = imageUrl3.map {
        if(it.length>0)
        {
            View.GONE
        }
        else{
            View.VISIBLE
        }
    }

    val capture4Visibility: LiveData<Int> = imageUrl4.map {
        if(it.length>0)
        {
            View.GONE
        }
        else{
            View.VISIBLE
        }
    }

    val capture5Visibility: LiveData<Int> = imageUrl5.map {
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

    val captured3Visibility: LiveData<Int> = capture3Visibility.map {
        if(it == View.GONE)
        {
            View.VISIBLE
        }
        else{
            View.GONE
        }
    }

    val captured4Visibility: LiveData<Int> = capture4Visibility.map {
        if(it == View.GONE)
        {
            View.VISIBLE
        }
        else{
            View.GONE
        }
    }

    val captured5Visibility: LiveData<Int> = capture5Visibility.map {
        if(it == View.GONE)
        {
            View.VISIBLE
        }
        else{
            View.GONE
        }
    }

    val totalNumberOfStudentsError: LiveData<String> = totalNumberOfStudents.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter value"
        }
    }

    val totalNoOfGivawayDistributedError: LiveData<String> = totalNoOfGivawayDistributed.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter value"
        }
    }

    val nameOfPrincipalError: LiveData<String> = nameOfPrincipal.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter name"
        }
    }

    val contactNumberOfPrincipalError: LiveData<String> = contactNumberOfPrincipal.map {

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