package com.hul.skb.mobiliser.ui.ipc1

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
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputFilter
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.hul.curriculam.ui.form1Fill.Form1FillFragment
import com.hul.curriculam.ui.form1Fill.Form1FillViewModel
import com.hul.dashboard.Dashboard
import com.hul.data.GetVisitDataResponseData
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.data.SchoolCode
import com.hul.data.UploadImageData
import com.hul.data.VillageLocalModel
import com.hul.data.VisitData
import com.hul.data.VisitDetails
import com.hul.databinding.FragmentForm1FillBinding
import com.hul.databinding.FragmentIPC1Binding
import com.hul.screens.field_auditor_dashboard.ui.image_preview.ImagePreviewDialogFragment
import com.hul.skb.SKBDashboardComponent
import com.hul.skb.mobiliser.SKBMobiliserDashboard
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
import java.time.LocalDateTime
import javax.inject.Inject

class IPC1Fragment : Fragment(), ApiHandler, RetryInterface {

    private var _binding: FragmentIPC1Binding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var skbDashboardComponent: SKBDashboardComponent

    //    private lateinit var disceCodeEditText: String
    @Inject
    lateinit var visitDataViewModel: VisitDataViewModel

    @Inject
    lateinit var ipC1ViewModel: IPC1ViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var apiController: APIController

    @Inject
    lateinit var uploadFileController: UploadFileController

    lateinit var villageLocalModel: VillageLocalModel

    var imageIndex: Int = 0

    private lateinit var countDownTimer: CountDownTimer

    var isTimerStarted = false;

    private val handler = Handler(Looper.getMainLooper())

    var projectLocalList: ArrayList<ProjectInfo> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentIPC1Binding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        skbDashboardComponent =
            (activity?.application as HULApplication).appComponent.skbDashboardComponent()
                .create()
        skbDashboardComponent.inject(this)

        val schoolCode = Gson().fromJson(
            requireArguments().getString(ARG_CONTENT1),
            SchoolCode::class.java
        )

        ipC1ViewModel.selectedSchoolCode.value = schoolCode

        ipC1ViewModel.projectInfo.value = Gson().fromJson(
            requireArguments().getString(ARG_CONTENT2),
            ProjectInfo::class.java
        )

        binding.viewModel = ipC1ViewModel

        binding.capture1.setOnClickListener {
            redirectToCamera(0, "Front", requireContext().getString(R.string.location_selfie))
        }
        binding.capture2.setOnClickListener {
            redirectToCamera(
                1,
                "Back",
                requireContext().getString(R.string.picture_with_IPC_Assets)
            )
        }
        binding.capture3.setOnClickListener {
            redirectToCamera(2, "Back", requireContext().getString(R.string.consent_card_picture))
        }
        binding.capture4.setOnClickListener {
            redirectToCamera(
                3,
                "Back",
                requireContext().getString(R.string.image_while_conducting_build_your_balanced_plate_activity)
            )
        }

        binding.capture5.setOnClickListener {
            redirectToCamera(
                4,
                "Back",
                requireContext().getString(R.string.image_while_conducting_ek_katori_demo)
            )
        }

        binding.capture6.setOnClickListener {
            redirectToCamera(
                5,
                "Back",
                requireContext().getString(R.string.image_of_TG_where_all_faces_are_visible)
            )
        }

        binding.capture7.setOnClickListener {
            redirectToCamera(
                6,
                "Back",
                requireContext().getString(R.string.image_with_TG_carrying_bindi_tool_Recepie_book_in_hand_Long_shot_all_TG_visible)
            )
        }

        binding.capture8.setOnClickListener {
            redirectToCameraPotrait(
                7,
                "Back",
                requireContext().getString(R.string.image_of_session_monitor_declaration_form)
            )
        }

        binding.proceed.setOnClickListener {

            val visitDataTable = VisitDataTable(
                jsonData = Gson().toJson(submitModel()),
                visitNumber = 1,
                locationName = ipC1ViewModel.projectInfo.value!!.location_name!!,
                uDiceCode = "IPC 01",
                locationId = ipC1ViewModel.projectInfo.value!!.location_id!!
            )
            Log.d("visitDataTableForm1", "onCreateView: ${visitDataTable}")

            visitDataViewModel.insert(visitDataTable)
            Toast.makeText(requireContext(), "Visit Data saved successfully", Toast.LENGTH_LONG)
                .show()

            userInfo.villageLocalData = ""

            val intent = Intent(activity, SKBMobiliserDashboard::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            requireActivity().finish()
        }


        binding.view1.setOnClickListener {
            ipC1ViewModel.imageUrl1.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }
        binding.view2.setOnClickListener {
            ipC1ViewModel.imageUrl2.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }
        binding.view3.setOnClickListener {
            ipC1ViewModel.imageUrl3.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }

        binding.view4.setOnClickListener {
            ipC1ViewModel.imageUrl4.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }

        binding.view5.setOnClickListener {
            ipC1ViewModel.imageUrl5.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }

        binding.view6.setOnClickListener {
            ipC1ViewModel.imageUrl6.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }

        binding.view7.setOnClickListener {
            ipC1ViewModel.imageUrl7.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }

        binding.view8.setOnClickListener {
            ipC1ViewModel.imageUrl8.value?.let { it1 ->
                showImagePreview(
                    it1
                )
            }
        }


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

        binding.nameOfSessionMonitor.filters = arrayOf(allowOnlyLettersAndSpacesFilter)


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

        loadLocalData()
        getLocalData()

//        ipC1ViewModel.revisitApplicable.observe(viewLifecycleOwner) { index ->
//            // Update UI or perform any necessary actions
//            val indexSelected = binding.radioGroup.indexOfChild(binding.radioGroup.findViewById(index))
//            ipC1ViewModel.revisitApplicableFlag.value = if(indexSelected==0) true else false
//        }

        ipC1ViewModel.imageUrl1.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl1 = ipC1ViewModel.imageUrl1.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.imageUrl2.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl2 = ipC1ViewModel.imageUrl2.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.imageUrl3.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl3 = ipC1ViewModel.imageUrl3.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.imageUrl4.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl4 = ipC1ViewModel.imageUrl4.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.imageUrl5.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl5 = ipC1ViewModel.imageUrl5.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.imageUrl6.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl6 = ipC1ViewModel.imageUrl6.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.imageUrl7.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl7 = ipC1ViewModel.imageUrl7.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.imageUrl8.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.imageUrl8 = ipC1ViewModel.imageUrl8.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.buildYourBalancedPlate.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.build_your_balanced_plate =
                ipC1ViewModel.buildYourBalancedPlate.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.ekKatoriToolKit.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.ek_katori_tool_kit = ipC1ViewModel.ekKatoriToolKit.value!!
            updateData(villageLocalModel)
        }
//        ipC1ViewModel.bindiTool.observe(viewLifecycleOwner) { newValue ->
//            villageLocalModel.bindi_tool = ipC1ViewModel.bindiTool.value!!
//            updateData(villageLocalModel)
//        }
        ipC1ViewModel.recipeBook.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.recipe_book = ipC1ViewModel.recipeBook.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.noOfTGPresent.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.no_of_TG_present = ipC1ViewModel.noOfTGPresent.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.noOfReciepeBookDistributed.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.no_of_recipe_book_distributed =
                ipC1ViewModel.noOfReciepeBookDistributed.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.nameOfSessionMoniter.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.name_of_session_monitor = ipC1ViewModel.nameOfSessionMoniter.value!!
            updateData(villageLocalModel)
        }
        ipC1ViewModel.sessionMonitorContactNumber.observe(viewLifecycleOwner) { newValue ->
            villageLocalModel.session_monitor_contact_number =
                ipC1ViewModel.sessionMonitorContactNumber.value!!
            updateData(villageLocalModel)
        }
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

    private fun redirectToCameraPotrait(position: Int, imageType: String, heading: String) {
        val intent = Intent(activity, CameraActivity::class.java)
        intent.putExtra("position", position)
        intent.putExtra("imageType", imageType)
        intent.putExtra("heading", heading)
        intent.putExtra("viewType", "Portrait")
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
            filename = fileName
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

                // Update the view model's imageUrl at the corresponding position
                when (position) {
                    0 -> ipC1ViewModel.imageUrl1.value = imageUrl
                    1 -> ipC1ViewModel.imageUrl2.value = imageUrl
                    2 -> ipC1ViewModel.imageUrl3.value = imageUrl
                    3 -> ipC1ViewModel.imageUrl4.value = imageUrl
                    4 -> ipC1ViewModel.imageUrl5.value = imageUrl
                    5 -> ipC1ViewModel.imageUrl6.value = imageUrl
                    6 -> ipC1ViewModel.imageUrl7.value = imageUrl
                    7 -> ipC1ViewModel.imageUrl8.value = imageUrl
                }
            }
        }


    companion object {
        private const val ARG_CONTENT1 = "content1"
        private const val ARG_CONTENT2 = "projectInfo"
        private const val U_DICE_CODE = "uDiceCode"

        private val REQUIRED_PERMISSIONS =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                )
            } else {
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
            Form1FillFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT1, content1)
                    putString(ARG_CONTENT2, content2)
                    putString(U_DICE_CODE, uDiceCode)
                }
            }
    }

    private fun visitsDataModel(): RequestModel {
        return ipC1ViewModel.projectInfo.value?.visit_id?.let {
            RequestModel(
                project = userInfo.projectName,
                visitId = it,
                loadImages = false
            )
        }!!
    }

    private fun getVisitData() {
//        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
        //setProgressDialog(requireContext(), "Loading Visit data")
        apiController.getApiResponse(
            this,
            visitsDataModel(),
            ApiExtentions.ApiDef.GET_VISIT_DATA.ordinal
        )
//        } else {
//            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.GET_VISIT_DATA.ordinal, this)
//        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //getVisitData()
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
            if (!allPermissionsGranted()) {
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
            visit_number = "1",
            project = userInfo.projectName,
            visit_id = ipC1ViewModel.projectInfo.value!!.visit_id.toString(),
            identifier = villageLocalModel.identifier,
            visitData = VisitData(
                identifier = VisitDetails(value = villageLocalModel.identifier),
                visit_image_1 = VisitDetails(value = ipC1ViewModel.imageUrl1.value),
                visit_image_2 = VisitDetails(value = ipC1ViewModel.imageUrl2.value),
                visit_image_3 = VisitDetails(value = ipC1ViewModel.imageUrl3.value),
                visit_image_4 = VisitDetails(value = ipC1ViewModel.imageUrl4.value),
                visit_image_5 = VisitDetails(value = ipC1ViewModel.imageUrl5.value),
                visit_image_6 = VisitDetails(value = ipC1ViewModel.imageUrl6.value),
                visit_image_7 = VisitDetails(value = ipC1ViewModel.imageUrl7.value),
                visit_image_8 = VisitDetails(value = ipC1ViewModel.imageUrl8.value),
                build_your_balanced_plate = VisitDetails(value = ipC1ViewModel.buildYourBalancedPlate.value),
                ek_katori_tool_kit = VisitDetails(value = ipC1ViewModel.ekKatoriToolKit.value),
                recipe_book = VisitDetails(value = ipC1ViewModel.recipeBook.value),
                no_of_TG_present = VisitDetails(value = ipC1ViewModel.noOfTGPresent.value),
                no_of_recipe_book_distributed = VisitDetails(value = ipC1ViewModel.noOfReciepeBookDistributed.value),
                name_of_session_monitor = VisitDetails(value = ipC1ViewModel.nameOfSessionMoniter.value),
                session_monitor_contact_number = VisitDetails(value = ipC1ViewModel.sessionMonitorContactNumber.value),

                visit_id = ipC1ViewModel.projectInfo.value!!.visit_id.toString(),
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
                    requireActivity().onBackPressed()
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
            }

            ApiExtentions.ApiDef.GET_VISIT_DATA -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                ipC1ViewModel.visitData.value = Gson().fromJson(
                    model.getJSONObject("data").toString(),
                    GetVisitDataResponseData::class.java
                )

                //fillData()

                // For render purpose only
                /*if (ipC1ViewModel.visitData.value?.visit_1 != null) {
                    ipC1ViewModel.visitDataToView.value =
                        ipC1ViewModel.visitData.value?.visit_1
                } else if (ipC1ViewModel.visitData.value?.visit_2 != null) {
                    ipC1ViewModel.visitDataToView.value =
                        ipC1ViewModel.visitData.value?.visit_2
                } else if (ipC1ViewModel.visitData.value?.visit_3 != null) {
                    ipC1ViewModel.visitDataToView.value =
                        ipC1ViewModel.visitData.value?.visit_3
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

    private fun startTimer() {
        if (isTimerStarted) {
            return
        }

        isTimerStarted = true
        //binding.proceed.isEnabled = false

        val totalTime = 20 * 60 * 1000L
        //val totalTime =10000L

        // Set initial time before starting the timer
        updateTimerText(totalTime)


        countDownTimer.start()
    }

    private fun updateTimerText(millisUntilFinished: Long) {
        val minutesLeft = millisUntilFinished / 1000 / 60
        val secondsLeft = (millisUntilFinished / 1000) % 60
    }

    fun showViewTemporarily(view: View, duration: Long) {
        view.visibility = View.VISIBLE
        handler.postDelayed({
            view.visibility = View.GONE
        }, duration)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    private fun checkValidation(): Boolean {
        Log.d("Nitin", ipC1ViewModel.revisitApplicableFlag.value.toString())
        if (ipC1ViewModel.projectInfo.value == null) {
            Toast.makeText(requireContext(), "Visit details not found", Toast.LENGTH_LONG).show()
            return false;
        } else if (currentLocation == null) {
            Toast.makeText(requireContext(), "Location details not found", Toast.LENGTH_LONG).show()
            return false;
        } else if (ipC1ViewModel.imageUrl1.value?.isEmpty() == true || ipC1ViewModel.imageUrl2.value?.isEmpty() == true
            || (ipC1ViewModel.imageUrl3.value?.isEmpty() == true) || (ipC1ViewModel.imageUrl4.value?.isEmpty() == true)
        ) {
            Toast.makeText(requireContext(), "Please add required images", Toast.LENGTH_LONG).show()
            return false;
        } else {
            return true;
        }
    }

    fun getLocalData() {
        if (userInfo.villageLocalData.isNotEmpty()) {
            villageLocalModel =
                Gson().fromJson(userInfo.villageLocalData, VillageLocalModel::class.java)
            ipC1ViewModel.imageUrl1.value = villageLocalModel.imageUrl1
            ipC1ViewModel.imageUrl2.value = villageLocalModel.imageUrl2
            ipC1ViewModel.imageUrl3.value = villageLocalModel.imageUrl3
            ipC1ViewModel.imageUrl4.value = villageLocalModel.imageUrl4
            ipC1ViewModel.imageUrl5.value = villageLocalModel.imageUrl5
            ipC1ViewModel.imageUrl6.value = villageLocalModel.imageUrl6
            ipC1ViewModel.imageUrl7.value = villageLocalModel.imageUrl7
            ipC1ViewModel.imageUrl8.value = villageLocalModel.imageUrl8
            ipC1ViewModel.buildYourBalancedPlate.value = villageLocalModel.build_your_balanced_plate
            ipC1ViewModel.ekKatoriToolKit.value = villageLocalModel.ek_katori_tool_kit
            ipC1ViewModel.recipeBook.value = villageLocalModel.recipe_book
            ipC1ViewModel.noOfTGPresent.value = villageLocalModel.no_of_TG_present
            ipC1ViewModel.noOfReciepeBookDistributed.value =
                villageLocalModel.no_of_recipe_book_distributed
            ipC1ViewModel.nameOfSessionMoniter.value = villageLocalModel.name_of_session_monitor
            ipC1ViewModel.sessionMonitorContactNumber.value =
                villageLocalModel.session_monitor_contact_number
        } else {
            villageLocalModel = VillageLocalModel()
            villageLocalModel.identifier = System.currentTimeMillis().toString()
            updateData(villageLocalModel)
        }
    }

    fun updateData(villageLocalModel: VillageLocalModel) {
        userInfo.villageLocalData = Gson().toJson(villageLocalModel)
    }
}