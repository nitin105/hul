package com.hul.skb.mobiliser.ui.awc

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

class AWCSessionViewModel @Inject constructor(
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
    var imageUrl6 = MutableLiveData<String>("")
    var imageUrl7 = MutableLiveData<String>("")

    var buildYourBalancedPlate = MutableLiveData<Boolean>(false)
    var ekKatoriToolKit = MutableLiveData<Boolean>(false)
    var ifaTool = MutableLiveData<Boolean>(false)
    var dBooklet = MutableLiveData<Boolean>(false)

    var noOfTGPresent = MutableLiveData<String>("")
    var noOfPregnantPresent = MutableLiveData<String>("")
    var noOfIFAtoolsDistributed = MutableLiveData<String>("")
    var nameOfAnganwadiSevika = MutableLiveData<String>("")
    var mobileNumberOfAnganwadiSevika = MutableLiveData<String>("")

    val loginEnabled = MediatorLiveData<Boolean>(true)

    init {

//        form4Text.addSource(form4) { value ->
//            form4Text.value = if (value) "Yes" else "No"
//        }

        loginEnabled.addSource(imageUrl1) {
            loginEnabled.value = it.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(imageUrl2) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(imageUrl3) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(imageUrl4) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl5.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(imageUrl5) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(imageUrl6) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl5.value!!.length > 0  && imageUrl7.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(imageUrl7) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(noOfTGPresent) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(noOfIFAtoolsDistributed) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(noOfPregnantPresent) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0  && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(nameOfAnganwadiSevika) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(mobileNumberOfAnganwadiSevika) {
            loginEnabled.value = it.length > 0 && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0 && nameOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(buildYourBalancedPlate) {
            loginEnabled.value = it && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0 && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && ekKatoriToolKit.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(ekKatoriToolKit) {
            loginEnabled.value = it && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0 && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ifaTool.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(ifaTool) {
            loginEnabled.value = it && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0 && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && dBooklet.value!!
        }

        loginEnabled.addSource(dBooklet) {
            loginEnabled.value = it && imageUrl1.value!!.length > 0 && imageUrl2.value!!.length > 0 && imageUrl3.value!!.length > 0 && imageUrl4.value!!.length > 0 && imageUrl6.value!!.length > 0  && imageUrl7.value!!.length > 0 && imageUrl5.value!!.length > 0  && noOfTGPresent.value!!.length > 0 && noOfIFAtoolsDistributed.value!!.length > 0 && noOfPregnantPresent.value!!.length > 0 && nameOfAnganwadiSevika.value!!.length > 0 && mobileNumberOfAnganwadiSevika.value!!.length > 0 && buildYourBalancedPlate.value!! && ekKatoriToolKit.value!! && ifaTool.value!!
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

    val capture6Visibility: LiveData<Int> = imageUrl6.map {
        if(it.length>0)
        {
            View.GONE
        }
        else{
            View.VISIBLE
        }
    }

    val capture7Visibility: LiveData<Int> = imageUrl7.map {
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

    val captured6Visibility: LiveData<Int> = capture6Visibility.map {
        if(it == View.GONE)
        {
            View.VISIBLE
        }
        else{
            View.GONE
        }
    }

    val captured7Visibility: LiveData<Int> = capture7Visibility.map {
        if(it == View.GONE)
        {
            View.VISIBLE
        }
        else{
            View.GONE
        }
    }
    

    val noOfTGPresentError: LiveData<String> = noOfTGPresent.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter value"
        }
    }

    val noOfIFAtoolsDistributedError: LiveData<String> = noOfIFAtoolsDistributed.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter name"
        }
    }

    val noOfPregnantPresentError: LiveData<String> = noOfPregnantPresent.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter name"
        }
    }

    val nameOfAnganwadiSevikaError: LiveData<String> = nameOfAnganwadiSevika.map {

        if(it.length > 0)
        {
            ""
        }
        else{
            "Enter name"
        }
    }

    val mobileNumberOfAnganwadiSevikaError: LiveData<String> = mobileNumberOfAnganwadiSevika.map {

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