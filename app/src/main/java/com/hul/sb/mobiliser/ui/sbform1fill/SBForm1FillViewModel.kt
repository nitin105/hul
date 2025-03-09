package com.hul.sb.mobiliser.ui.sbform1fill

import android.view.View
import androidx.lifecycle.*
import com.hul.data.*
import com.hul.user.UserInfo
import javax.inject.Inject
import android.widget.CheckBox
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

class SBForm1FillViewModel @Inject constructor(
    private val userInfo: UserInfo
) : ViewModel() {

    // LiveData to hold form data
    var selectedSchoolCode = MutableLiveData<SchoolCode>()
    var projectInfo = MutableLiveData<ProjectInfo>()
    var position = MutableLiveData(0)
    var numberOfStudentEditable = MutableLiveData(false)

    // Coordinates
    var longitude = MutableLiveData<String>()
    var latitude = MutableLiveData<String>()

    // Image URLs
    var imageUrl1 = MutableLiveData("")
    var imageUrl2 = MutableLiveData("")
    var imageUrl3 = MutableLiveData("")
    var imageUrl4 = MutableLiveData("")
    //var imageUrl5 = MutableLiveData("")

    // API Image URLs
    var imageApiUrl1 = MutableLiveData("")
    var imageApiUrl2 = MutableLiveData("")
    var imageApiUrl3 = MutableLiveData("")
    var imageApiUrl4 = MutableLiveData("")

    // Form fields
    var houseCode = MutableLiveData("")
    var customerName = MutableLiveData("")
    var noOfMembers = MutableLiveData("")
    var isThereToilet = MutableLiveData("")
    var isThereWaching = MutableLiveData("")
    var visitedSuvidha = MutableLiveData("")
    var address = MutableLiveData("")
    var mobile = MutableLiveData("")
    var leadQuality = MutableLiveData("")
    //val isChecked = MutableLiveData<Boolean>(false) // Initial value is false
    val isCheckBoxChecked = MutableLiveData(false)



    // State to enable login
    val loginEnabled = MediatorLiveData<Boolean>(true)

    // Timer
    val timerFinished = MediatorLiveData<Boolean>(false)

    // Initialize login conditions
    init {
        val sources = listOf(
            imageUrl1, imageUrl2, imageUrl3, imageUrl4,
            houseCode, customerName, noOfMembers, isThereToilet, isThereWaching,
            visitedSuvidha, address, mobile, leadQuality
        )

        sources.forEach { source ->
            loginEnabled.addSource(source) { validateLogin() }
        }
    }

    // Helper method to validate login state
    private fun validateLogin() {
        loginEnabled.value = listOf(
            imageUrl1, imageUrl2, imageUrl3, imageUrl4,
            houseCode, customerName, noOfMembers, isThereToilet, isThereWaching,
            visitedSuvidha, address, mobile, leadQuality
        ).all { it.value?.isNotEmpty() == true } &&
                mobile.value?.length == 10 && mobile.value?.first()?.isDigit() == true &&
                mobile.value?.first()?.toString()?.toInt() in 6..9
    }

    // LiveData for handling visibility of image capturing views
    val capture1Visibility: LiveData<Int> = imageUrl1.map { if (it.isNotEmpty()) View.GONE else View.VISIBLE }
    val capture2Visibility: LiveData<Int> = imageUrl2.map { if (it.isNotEmpty()) View.GONE else View.VISIBLE }
    val capture3Visibility: LiveData<Int> = imageUrl3.map { if (it.isNotEmpty()) View.GONE else View.VISIBLE }
    val capture4Visibility: LiveData<Int> = imageUrl4.map { if (it.isNotEmpty()) View.GONE else View.VISIBLE }
    //val capture5Visibility: LiveData<Int> = imageUrl5.map { if (it.isNotEmpty()) View.GONE else View.VISIBLE }

    // LiveData for handling visibility of captured image views
    val captured1Visibility: LiveData<Int> = capture1Visibility.map { if (it == View.GONE) View.VISIBLE else View.GONE }
    val captured2Visibility: LiveData<Int> = capture2Visibility.map { if (it == View.GONE) View.VISIBLE else View.GONE }
    val captured3Visibility: LiveData<Int> = capture3Visibility.map { if (it == View.GONE) View.VISIBLE else View.GONE }
    val captured4Visibility: LiveData<Int> = capture4Visibility.map { if (it == View.GONE) View.VISIBLE else View.GONE }
    //val captured5Visibility: LiveData<Int> = capture5Visibility.map { if (it == View.GONE) View.VISIBLE else View.GONE }

    // Error messages for form fields
    var houseCodeError: LiveData<String> = houseCode.map { if (it.isNotEmpty()) "" else "Enter value" }
    var customerNameError: LiveData<String> = customerName.map { if (it.isNotEmpty()) "" else "Enter value" }
    var noOfMembersError: LiveData<String> = noOfMembers.map { if (it.isNotEmpty()) "" else "Enter value" }
    var isThereToiletError: LiveData<String> = isThereToilet.map { if (it.isNotEmpty()) "" else "Enter value" }
    var isThereWachingError: LiveData<String> = isThereWaching.map { if (it.isNotEmpty()) "" else "Enter value" }
    var visitedSuvidhaError: LiveData<String> = visitedSuvidha.map { if (it.isNotEmpty()) "" else "Enter value" }

    /*
    var addressError: LiveData<String> = address.map {
        when {
            it.isEmpty() -> "Enter value"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() -> "Enter valid email"
            else -> ""
        }
    }

     */
    var addressError: LiveData<String> = address.map { if (it.isNotEmpty()) "" else "Enter value" }

    var mobileError: LiveData<String> = mobile.map {
        when {
            it.isEmpty() -> "Enter value"
            it.length != 10 -> "Enter a 10-digit number"
            !it.all(Char::isDigit) -> "Enter only digits"
            it[0].toString().toInt() !in 6..9 -> "Number should start with 6 to 9"
            else -> ""
        }
    }

    var leadQualityError: LiveData<String> = leadQuality.map { if (it.isNotEmpty()) "" else "Enter value" }

    // Visit data
    var visitData = MutableLiveData<GetVisitDataResponseData>(null)
    var visitDataToView = MutableLiveData<Visit1>(null)
}
