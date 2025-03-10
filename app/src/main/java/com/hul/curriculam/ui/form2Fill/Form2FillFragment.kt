package com.hul.curriculam.ui.form2Fill

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.provider.Settings
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hul.HULApplication
import com.hul.R
import com.hul.api.ApiExtentions
import com.hul.api.ApiHandler
import com.hul.api.controller.APIController
import com.hul.api.controller.UploadFileController
import com.hul.camera.CameraActivity
import com.hul.curriculam.CurriculamComponent
import com.hul.dashboard.Dashboard
import com.hul.data.GetVisitDataResponseData
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.data.SchoolCode
import com.hul.data.UploadImageData
import com.hul.data.VisitData
import com.hul.data.VisitDetails
import com.hul.databinding.FragmentForm2FillBinding
import com.hul.screens.field_auditor_dashboard.ui.image_preview.ImagePreviewDialogFragment
import com.hul.sync.VisitDataTable
import com.hul.sync.VisitDataViewModel
import com.hul.user.UserInfo
import com.hul.utils.ConnectionDetector
import com.hul.utils.RetryInterface
import com.hul.utils.cancelProgressDialog
import com.hul.utils.noInternetDialogue
import com.hul.utils.redirectionAlertDialogue
import org.json.JSONObject
import java.lang.reflect.Type
import javax.inject.Inject

class Form2FillFragment : Fragment(), ApiHandler, RetryInterface {

    private var _binding: FragmentForm2FillBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var curriculamComponent: CurriculamComponent

//    private lateinit var disceCodeEditText: String

    @Inject
    lateinit var form2FillViewModel: Form2FillViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var apiController: APIController

    @Inject
    lateinit var uploadFileController: UploadFileController

    @Inject
    lateinit var visitDataViewModel: VisitDataViewModel

    var imageIndex: Int = 0

    private lateinit var countDownTimer: CountDownTimer

    var isTimerStarted = false;

    var projectLocalList: ArrayList<ProjectInfo> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentForm2FillBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        curriculamComponent =
            (activity?.application as HULApplication).appComponent.curriculamComponent()
                .create()
        curriculamComponent.inject(this)

        val schoolCode = Gson().fromJson(
            requireArguments().getString(ARG_CONTENT1),
            SchoolCode::class.java
        )

        binding.nestedScrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                    Log.d("TouchListener", "User scrolled manually")
                    val currentFocus = activity?.currentFocus
                    if (currentFocus is TextInputEditText) {
                        currentFocus.clearFocus()
                    }
                }
            }
            false
        }

        //Client asked to remove it, so hidden in both results
        binding.llGetDirection.visibility =
            if (schoolCode.lattitude == null) View.GONE else View.GONE

        form2FillViewModel.selectedSchoolCode.value = schoolCode

        form2FillViewModel.projectInfo.value = Gson().fromJson(
            requireArguments().getString(ARG_CONTENT2),
            ProjectInfo::class.java
        )

        binding.viewModel = form2FillViewModel

        binding.capture1.setOnClickListener {
            redirectToCamera(0, "Back", requireContext().getString(R.string.school_pic1))
        }
        binding.capture2.setOnClickListener {
            redirectToCamera(
                1,
                "Image Capture Front",
                requireContext().getString(R.string.school_pic2)
            )
        }
        binding.capture3.setOnClickListener {
            redirectToCamera(2, "Back", requireContext().getString(R.string.school_pic3))
        }
        binding.capture4.setOnClickListener {
            redirectToCamera(3, "Back", requireContext().getString(R.string.school_pic4))
        }

        binding.proceed.setOnClickListener {
            if (checkValidation()) {
                if (imageIndex == 0) {
                    val visitDataTable = VisitDataTable(
                        jsonData = Gson().toJson(submitModel()),
                        visitNumber = form2FillViewModel.projectInfo.value!!.visit_number!!.toInt(),
                        locationName = form2FillViewModel.projectInfo.value!!.location_name!!,
                        uDiceCode = binding.disceCode.text.toString(),
                        locationId = form2FillViewModel.projectInfo.value!!.location_id!!
                    )
                    Log.d("visitDataTable", "onCreateView: ${visitDataTable}")
                    visitDataViewModel.insert(visitDataTable)

                    Toast.makeText(requireContext(), "Visit Data saved successfully", Toast.LENGTH_LONG).show()

                    for(item in 0..projectLocalList.size-1)
                    {
                        if(projectLocalList.get(item).visit_id == form2FillViewModel.projectInfo.value!!.visit_id)
                        {
                            projectLocalList.removeAt(item)
                            userInfo.localProjectList = Gson().toJson(projectLocalList)
                            break
                        }
                    }

                    val intent = Intent(activity, Dashboard::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }



        binding.view1.setOnClickListener {
            form2FillViewModel.imageUrl1.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }
        binding.view2.setOnClickListener {
            form2FillViewModel.imageUrl2.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }
        binding.view3.setOnClickListener {
            form2FillViewModel.imageUrl3.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }

        binding.view4.setOnClickListener {
            form2FillViewModel.imageUrl4.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }

        form2FillViewModel.uDiceCode.value = requireArguments().getString(U_DICE_CODE)

        if (allPermissionsGranted()) {
            checkLocationSettings()
        } else {
            requestPermission()
        }

        val allowOnlyLettersAndSpacesFilter =
            InputFilter { source, start, end, dest, dstart, dend ->
                for (i in start until end) {
                    if (!source[i].isLetter() && !source[i].isWhitespace()) {
                        return@InputFilter ""
                    }
                }
                null
            }

        binding.form1.filters = arrayOf(allowOnlyLettersAndSpacesFilter)

        binding.txtDirections.setOnClickListener {
            if (currentLocation != null) {
                form2FillViewModel.selectedSchoolCode.value?.longitude?.let { it1 ->
                    form2FillViewModel.selectedSchoolCode.value?.lattitude?.let { it2 ->
                        openGoogleMapsForDirections(
                            currentLocation!!.latitude,
                            currentLocation!!.longitude,
                            it2,
                            it1
                        )
                    }
                }
            }
        }



        binding.radioButton1.setOnCheckedChangeListener { group, checkedId ->

            // on below line we are displaying a toast message.
            form2FillViewModel.curriculamOnTrackFlag.value = true
        }

        binding.radioButton2.setOnCheckedChangeListener { group, checkedId ->

            // on below line we are displaying a toast message.
            form2FillViewModel.curriculamOnTrackFlag.value = false
        }

        loadLocalData()

        return root
    }

    fun loadLocalData() {
        if (userInfo.localProjectList != null && userInfo.localProjectList.length > 0) {
            val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
            projectLocalList = Gson().fromJson(userInfo.localProjectList, listType)
        }
    }

    private fun openGoogleMapsForDirections(
        lat: Double,
        lng: Double,
        destinationLat: String,
        destinationLng: String
    ) {

        val destLat = destinationLat
        val destLng = destinationLng

        // Build the URI for the directions request
        val uri =
            Uri.parse("http://maps.google.com/maps?saddr=$lat,$lng&daddr=$destLat,$destLng")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun showImagePreview(imagePath: String) {
        val imageUri = Uri.parse(imagePath)
        val newFragment = ImagePreviewDialogFragment.newInstance(imageUri)
        newFragment.show(childFragmentManager, "image_preview")
    }


    private fun redirectToCamera(position: Int, imageType: String, heading: String) {
        val intent = Intent(activity, CameraActivity::class.java)
        intent.putExtra("position", position)
        intent.putExtra("imageType", imageType)
        intent.putExtra("heading", heading)
        startImageCapture.launch(intent)
    }

    private fun uploadImageModel(): RequestModel {
        var fileName: String = ""
        val visitPrefix = "project_" + userInfo.projectName;
        when (imageIndex) {
            0 -> {
                fileName = visitPrefix + "_picture_of_school_name.jpeg";
            }

            1 -> {
                fileName = visitPrefix + "_selfie_with_school_name.jpeg";
            }

            2 -> {
                fileName = visitPrefix + "_students_showing_filled_tracker.jpeg";
            }

            3 -> {
                fileName = visitPrefix + "_teacher_handling_trackers.jpeg";
            }

            4 -> {
                fileName = visitPrefix + "_acknowledgement_letter.jpeg";
            }
        }

        return RequestModel(
            project = userInfo.projectName,
            uploadFor = "field_audit",
            filename = fileName,
            visit_id = ""
        )
    }

    private fun uploadImage(imageUri: Uri) {
        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            uploadFileController.getApiResponse(
                this,
                imageUri,
                uploadImageModel(),
                ApiExtentions.ApiDef.UPLOAD_IMAGE.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.UPLOAD_IMAGE.ordinal, this)
        }
    }

    val startImageCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                val position = data!!.getIntExtra("position", 0)
                val imageUrl = result.data!!.getStringExtra("imageUrl")
                startTimer()
                Log.d("form2FillViewModel.imageUrl1.value", "${form2FillViewModel.imageUrl1.value}")
                // Update the view model's imageUrl at the corresponding position
                when (position) {
                    0 -> form2FillViewModel.imageUrl1.value = imageUrl
                    1 -> form2FillViewModel.imageUrl2.value = imageUrl
                    2 -> form2FillViewModel.imageUrl3.value = imageUrl
                    3 -> form2FillViewModel.imageUrl4.value = imageUrl
                }
            }
        }


    companion object {
        private const val ARG_CONTENT1 = "content1"
        private const val ARG_CONTENT2 = "content2"
        private const val U_DICE_CODE = "uDiceCode"

        private val REQUIRED_PERMISSIONS =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R){
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                )
            }
            else{
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )

            }

        fun newInstance(content1: String, content2: String, uDiceCode: String?) =
            Form2FillFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT1, content1)
                    putString(ARG_CONTENT2, content2)
                    putString(U_DICE_CODE, uDiceCode)
                }
            }
    }

    private fun visitsDataModel(): RequestModel {
        return form2FillViewModel.projectInfo.value?.visit_id?.let {
            RequestModel(
                project = userInfo.projectName,
                visitId = it,
                loadImages = false
            )
        }!!
    }

    private fun getVisitData() {
        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Visit data")
            apiController.getApiResponse(
                this,
                visitsDataModel(),
                ApiExtentions.ApiDef.GET_VISIT_DATA.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.GET_VISIT_DATA.ordinal, this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getVisitData()
    }

    // TODO: Step 1.1, Review variables (no changes).
// FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you
// should receive updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    // Used only for local storage of the last known location. Usually, this would be saved to your
// database, but because this is a simplified sample without a full database, we only need the
// last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }
            if (granted) {
                checkLocationSettings()
            } else {
                showInformationMessage()
            }
        }

    private fun showInformationMessage() {
        AlertDialog.Builder(requireActivity())
            .setTitle("Permissions Needed")
            .setMessage("You have denied the permissions. Please go to settings and allow the permissions manually.")
            .setPositiveButton("Settings") { dialog, _ ->
                requestPermissionSettings()
                dialog.dismiss() // This dismisses the dialog
            }
            .setCancelable(false)
            .show()
    }

    private fun requestPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireActivity().packageName, null)
        }
        requestPermissionSetting.launch(intent)
    }


    private val requestPermissionSetting =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { permissions ->
            if(!allPermissionsGranted()) {
                showInformationMessage()
            }
        }

    private val requestLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { permissions ->
            checkLocationSettings()
        }

    private fun checkLocationSettings() {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            //Toast.makeText(this, "GPS is not enabled", Toast.LENGTH_SHORT).show()
            requestLocation()
        } else {
            requestLocationUpdates()
            // Location services are enabled
            //Toast.makeText(this, "Location services are enabled", Toast.LENGTH_SHORT).show()
            // Proceed with location-related operations
        }
    }

    fun requestPermission() {
        requestPermission.launch(REQUIRED_PERMISSIONS)
    }

    fun requestLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        requestLocation.launch(intent)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())


        locationRequest = LocationRequest.create().apply {
            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.
            //
            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            interval = 60

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            fastestInterval = 30

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = 10

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                // Normally, you want to save a new location to a database. We are simplifying
                // things a bit and just saving it as a local variable, as we only need it again
                // if a Notification is created (when the user navigates away from app).
                currentLocation = locationResult.lastLocation

                //attendenceViewModel.longitude.value = currentLocation!!.longitude.toString()
                //attendenceViewModel.lattitude.value = currentLocation!!.latitude.toString()
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)

            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

//        locationCallback = object : LocationCallback() {
//            //This callback is where we get "streaming" location updates. We can check things like accuracy to determine whether
//            //this latest update should replace our previous estimate.
//            override fun onLocationResult(locationResult: LocationResult) {
//
//                if (locationResult == null) {
//                    Log.d(TAG, "locationResult null")
//                    return
//                }
//                Log.d(TAG, "received " + locationResult.locations.size + " locations")
//                for (loc in locationResult.locations) {
//                    cameraPreviewViewModel.longitude.value = loc.longitude.toString()
//                    cameraPreviewViewModel.lattitude.value = loc.latitude.toString()
//                    if (cameraPreviewViewModel.uri.value != null) {
//                        cancelProgressDialog()
//                        redurectToImagePreview(cameraPreviewViewModel.uri.value!!)
//                    }
//                }
//            }
//
//            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
//                Log.d(TAG, "locationAvailability is " + locationAvailability.isLocationAvailable)
//                super.onLocationAvailability(locationAvailability)
//            }
//        }

//        val locationRequest = LocationRequest.create().apply {
//            interval = 100 // Update interval in milliseconds
//            fastestInterval = 500 // Fastest update interval in milliseconds
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            null /* Looper */
//        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    fun submitForm() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                submitModel(),
                ApiExtentions.ApiDef.VISIT_DATA.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.VISIT_DATA.ordinal, this)
        }

    }

    private fun submitModel(): RequestModel {

        return RequestModel(
            visit_number = "2",
            project = userInfo.projectName,
            visit_id = form2FillViewModel.projectInfo.value!!.visit_id.toString(),
            visitData = VisitData(
                u_dice_code = VisitDetails(value = binding.disceCode.text.toString()),
                school_name = VisitDetails(value = binding.schoolName.text.toString()),
                number_of_books_distributed = VisitDetails(value = binding.noOfBooksHanded.text.toString()),

                visit_image_1 = VisitDetails(value = form2FillViewModel.imageUrl1.value),
                visit_image_2 = VisitDetails(value = form2FillViewModel.imageUrl2.value),
                visit_image_3 = VisitDetails(value = form2FillViewModel.imageUrl3.value),
                visit_image_4 = VisitDetails(value = form2FillViewModel.imageUrl4.value),

                name_of_the_school_representative_who_collected_the_books = VisitDetails(value = binding.form1.text.toString()),
                mobile_number_of_the_school_representative_who_collected_the_books = VisitDetails(
                    value = binding.form2.text.toString()
                ),

                curriculum_on_track = VisitDetails(value = form2FillViewModel.curriculamOnTrackFlag.value),
                remark = VisitDetails(value = binding.form5.text.toString()),
                visit_id = form2FillViewModel.projectInfo.value!!.visit_id.toString(),
                latitude = VisitDetails(value = currentLocation?.latitude.toString()),
                longitude = VisitDetails(value = currentLocation?.longitude.toString())
            )
        )
    }

    override fun onApiSuccess(o: String?, objectType: Int) {
        when (ApiExtentions.ApiDef.entries[objectType]) {

            ApiExtentions.ApiDef.SUBMIT_SCHOOL_FORM -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    // Set the adapter to the AutoCompleteTextView
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.VISIT_DATA -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    userInfo.didUserSubmitNewVisit = true
                    val intent = Intent(activity, Dashboard::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.UPLOAD_IMAGE -> {
                val model = JSONObject(o.toString())
                val uploadImageData = Gson().fromJson(
                    model.getJSONObject("data").toString(),
                    UploadImageData::class.java
                )
                if (uploadImageData != null && imageIndex == 0) {
                    imageIndex += 1;
                    form2FillViewModel.imageApiUrl1.value = uploadImageData.url
                    uploadImage(form2FillViewModel.imageUrl2.value?.toUri()!!)
                } else if (uploadImageData != null && imageIndex == 1) {
                    imageIndex += 1;
                    form2FillViewModel.imageApiUrl2.value = uploadImageData.url
                    uploadImage(form2FillViewModel.imageUrl3.value?.toUri()!!)
                } else if (uploadImageData != null && imageIndex == 2) {
                    imageIndex += 1;
                    form2FillViewModel.imageApiUrl3.value = uploadImageData.url
                    uploadImage(form2FillViewModel.imageUrl4.value?.toUri()!!)
                } else if (uploadImageData != null && imageIndex == 3) {
                    imageIndex += 1;
                    form2FillViewModel.imageApiUrl4.value = uploadImageData.url
                    submitForm()
                }
            }

            ApiExtentions.ApiDef.GET_VISIT_DATA -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                form2FillViewModel.visitData.value = Gson().fromJson(
                    model.getJSONObject("data").toString(),
                    GetVisitDataResponseData::class.java
                )

                fillData()

                // For render purpose only
                /*if (form2FillViewModel.visitData.value?.visit_1 != null) {
                    form2FillViewModel.visitDataToView.value =
                        form2FillViewModel.visitData.value?.visit_1
                } else if (form2FillViewModel.visitData.value?.visit_2 != null) {
                    form2FillViewModel.visitDataToView.value =
                        form2FillViewModel.visitData.value?.visit_2
                } else if (form2FillViewModel.visitData.value?.visit_3 != null) {
                    form2FillViewModel.visitDataToView.value =
                        form2FillViewModel.visitData.value?.visit_3
                }*/
            }

            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }
    }

//    override fun onApiSuccess(o: String?, objectType: Int) {
//
//        cancelProgressDialog()
//        when (ApiExtentions.ApiDef.values()[objectType]) {
//
//            ApiExtentions.ApiDef.SUBMIT_SCHOOL_FORM -> {
//                val model = JSONObject(o.toString())
//                if (!model.getBoolean("error")) {
//
//
//                    // Set the adapter to the AutoCompleteTextView
//                } else {
//                    redirectionAlertDialogue(requireContext(), model.getString("message"))
//                }
//
//            }
//
//
//            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
//        }
//    }

    override fun onApiError(message: String?) {
        cancelProgressDialog()
        println(message)
        redirectionAlertDialogue(requireContext(), message!!)
    }

    override fun retry(type: Int) {

        when (ApiExtentions.ApiDef.entries[type]) {
            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }

    }

    private fun fillData() {
        binding.disceCode.setText(form2FillViewModel.uDiceCode.value)
        binding.schoolName.setText(form2FillViewModel.selectedSchoolCode.value?.location_name)
        binding.noOfBooksHanded.setText(form2FillViewModel.projectInfo.value?.number_of_books_distributed)
        binding.form1.setText(form2FillViewModel.visitData.value?.visit_2?.name_of_the_school_representative_who_collected_the_books?.value.toString())
        binding.form2.setText(form2FillViewModel.visitData.value?.visit_2?.mobile_number_of_the_school_representative_who_collected_the_books?.value.toString())
//        binding.switchIsCurriculamOnTrack.isChecked =
//            form2FillViewModel.visitData.value?.visit_2?.curriculum_on_track?.value == 1
        binding.form5.setText(form2FillViewModel.visitData.value?.visit_2?.remark?.value.toString())
    }

    private fun startTimer() {
        if (isTimerStarted) {
            return
        }

        isTimerStarted = true
        //binding.proceed.isEnabled = false

        val totalTime = 20 * 60 * 1000L

        // Set initial time before starting the timer
        updateTimerText(totalTime)
        binding.llTimer.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerText(millisUntilFinished)
            }

            override fun onFinish() {
                binding.llTimer.visibility = View.GONE
                form2FillViewModel.timerFinished.value = true
            }
        }

        countDownTimer.start()
    }

    private fun updateTimerText(millisUntilFinished: Long) {
        val minutesLeft = millisUntilFinished / 1000 / 60
        val secondsLeft = (millisUntilFinished / 1000) % 60
        binding.txtClock.text = String.format("Submit in %d:%02d minutes", minutesLeft, secondsLeft)
    }

    private fun checkValidation(): Boolean {
        if (binding.disceCode.text.toString().isBlank()) {
            Toast.makeText(requireContext(), "Please enter U Dise Code", Toast.LENGTH_LONG).show()
            return false;
        } else if (binding.schoolName.text.toString().isBlank()) {
            Toast.makeText(requireContext(), "Please enter school name", Toast.LENGTH_LONG).show()
            return false;
        } else if (form2FillViewModel.projectInfo.value?.number_of_books_distributed?.isEmpty() == true) {
            Toast.makeText(
                requireContext(),
                "Please enter no. of books handed over",
                Toast.LENGTH_LONG
            ).show()
            binding.noOfBooksHanded.isEnabled = true
            return false;
        } else if (form2FillViewModel.form1.value.toString().isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please enter name of school representative",
                Toast.LENGTH_LONG
            ).show()
            return false;
        } else if (form2FillViewModel.form2.value.toString().isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please enter mobile number of school representative",
                Toast.LENGTH_LONG
            ).show()
            return false;
        } else if (currentLocation == null) {
            Toast.makeText(requireContext(), "Location details not found", Toast.LENGTH_LONG).show()
            return false;
        } else if (form2FillViewModel.imageUrl1.value?.isEmpty() == true || form2FillViewModel.imageUrl2.value?.isEmpty() == true
            || form2FillViewModel.imageUrl3.value?.isEmpty() == true || form2FillViewModel.imageUrl4.value?.isEmpty() == true
        ) {
            Toast.makeText(requireContext(), "Please add required images", Toast.LENGTH_LONG).show()
            return false;
        } else {
            return true;
        }
    }
}