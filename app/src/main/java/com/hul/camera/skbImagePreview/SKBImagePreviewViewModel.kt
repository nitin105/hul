package com.hul.camera.skbImagePreview

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hul.data.GetVisitDataResponseData
import com.hul.data.ProjectInfo
import com.hul.user.UserInfo
import javax.inject.Inject

class SKBImagePreviewViewModel @Inject constructor(
    private val userInfo: UserInfo,
) : ViewModel() {

    var uri = MutableLiveData<Uri>()

    var longitude = MutableLiveData<String>()
    var lattitude = MutableLiveData<String>()

    var visitData = MutableLiveData<GetVisitDataResponseData>(null)

    var projectInfo = MutableLiveData<ProjectInfo>()

    var imageApiUrl1 = MutableLiveData<String>("")

}