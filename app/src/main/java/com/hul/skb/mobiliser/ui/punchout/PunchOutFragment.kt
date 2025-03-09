package com.hul.skb.mobiliser.ui.punchout

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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hul.HULApplication
import com.hul.R
import com.hul.api.ApiExtentions
import com.hul.api.ApiHandler
import com.hul.api.controller.APIController
import com.hul.api.controller.UploadFileController
import com.hul.camera.CameraActivity
import com.hul.curriculam.Curriculam
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.data.Society
import com.hul.data.UploadImageData
import com.hul.databinding.FragmentPunchOutBinding
import com.hul.databinding.FragmentSKBMobiliserAttendenceBinding
import com.hul.screens.field_auditor_dashboard.ui.image_preview.ImagePreviewDialogFragment
import com.hul.skb.SKBDashboardComponent
import com.hul.skb.mobiliser.SKBMobiliserDashboard
import com.hul.skb.mobiliser.ui.attendence.SKBMobiliserAttendenceViewModel
import com.hul.user.UserInfo
import com.hul.utils.ConnectionDetector
import com.hul.utils.RetryInterface
import com.hul.utils.UserTypes
import com.hul.utils.cancelProgressDialog
import com.hul.utils.noInternetDialogue
import com.hul.utils.nonredirectionAlertDialogue
import com.hul.utils.setProgressDialog
import org.json.JSONObject
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PunchOutFragment : Fragment(), ApiHandler, RetryInterface {

    private var _binding: FragmentPunchOutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var skbDashboardComponent: SKBDashboardComponent

    @Inject
    lateinit var uploadFileController: UploadFileController

    @Inject
    lateinit var attendenceViewModel: PunchOutViewModel

    @Inject
    lateinit var userInfo: UserInfo

    lateinit var allVillageList: ArrayList<ProjectInfo>

    @Inject
    lateinit var apiController: APIController

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

    var imageIndex: Int = 0

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

                attendenceViewModel.longitude.value = currentLocation!!.longitude.toString()
                attendenceViewModel.lattitude.value = currentLocation!!.latitude.toString()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPunchOutBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        skbDashboardComponent =
            (activity?.application as HULApplication).appComponent.skbDashboardComponent()
                .create()
        skbDashboardComponent.inject(this)
        binding.viewModel = attendenceViewModel


        binding.selfieCapture3.setOnClickListener {
            redirectToCamera(0, "Front", "Selfie at village")
        }

        binding.stats.setOnClickListener {
            requireActivity().onBackPressed()
        }

//        binding.retake1.setOnClickListener {
//            redirectToCamera(0,attendenceViewModel.imageType1.value!!)
//        }
//
//        binding.retake2.setOnClickListener {
//            redirectToCamera(1,attendenceViewModel.imageType2.value!!)
//        }

        binding.markAttendence2.setOnClickListener {
            if (attendenceViewModel.imageUrl1.value != null && attendenceViewModel.imageUrl1.value!!.length > 0) {
                if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
                    attendenceViewModel.position.value = 1
                    uploadImage(attendenceViewModel.imageUrl1.value?.toUri()!!)
                } else {
                    var locationId = "1"
                    for (data in allVillageList) {
                        if (data.location_name.equals(attendenceViewModel.village.value)) {
                            locationId = data.location_id!!
                        }
                    }
                    userInfo.localPunchOut = Gson().toJson(
                        RequestModel(
                            project = userInfo.projectName,
                            location_id = locationId,
                            village_name = attendenceViewModel.village.value,
                            lattitude = attendenceViewModel.lattitude.value,
                            longitude = attendenceViewModel.longitude.value,
                            photo_url1 = attendenceViewModel.imageUrl1.value,
                            photo_url1_description = "selfie_at_village",
                            attendance_type =  "PUNCH-OUT"
                        )
                    )
                    userInfo.didUserPunchedOut = true
                    binding.llVisitSuccessToast.visibility = View.VISIBLE
                    val mDelay: Long = 2000
                    Handler(Looper.getMainLooper()).postDelayed({
                        redirectToDashboard()
                    }, mDelay)


                }
            }
        }

        if (allPermissionsGranted()) {
            checkLocationSettings()
        } else {
            requestPermission()
        }

        attendenceViewModel.projectInfo.value =
            Gson().fromJson(requireArguments().getString("projectInfo"), Society::class.java)

        getTodaysVisit()
        return root
    }

    private fun getTodaysVisit() {
        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            apiController.getApiResponse(
                this,
                getTodaysVisitModel(),
                ApiExtentions.ApiDef.VILLAGE_LIST.ordinal
            )
        } else if(!userInfo.localProjectList.isNullOrEmpty()){
            val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
            val visitListFrom: ArrayList<ProjectInfo> =
                Gson().fromJson(userInfo.localProjectList, listType)
            allVillageList = visitListFrom
            val visitListFromBE = ArrayList<String>()
            for (data in visitListFrom) {
                visitListFromBE.add(data.location_name!!+" "+data.external_id1)
            }
            val responseArrayAdapter =
                ArrayAdapter(requireActivity(), R.layout.list_popup_window_item, visitListFromBE)
            binding.response.setAdapter(responseArrayAdapter)
        }
    }

    private fun getTodaysVisitModel(): RequestModel {
        return RequestModel(
            projectId = userInfo.projectId,
            userType = UserTypes.MOBILISER
        )
    }

    private fun showImagePreview(imagePath: String) {
        val imageUri = Uri.parse(imagePath)
        val newFragment = ImagePreviewDialogFragment.newInstance(imageUri)
        newFragment.show(childFragmentManager, "image_preview")
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    fun markAttendence2() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                markAttendenceModel(),
                ApiExtentions.ApiDef.PUNCH_OUT.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.PUNCH_OUT.ordinal, this)
        }

    }

    private fun markAttendenceModel2(): RequestModel {
        return RequestModel(
            project = userInfo.projectName,
            location_id = attendenceViewModel.projectInfo.value!!.id.toString(),
            lattitude = attendenceViewModel.lattitude.value,
            longitude = attendenceViewModel.longitude.value,
            photo_url1 = attendenceViewModel.imageUrl1API.value,
            photo_url1_description = binding.image3Description.text.toString(),
            attendance_type =  "PUNCH-OUT"
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()


    }

    fun markAttendence() {
        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                markAttendenceModel(),
                ApiExtentions.ApiDef.PUNCH_OUT.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.PUNCH_OUT.ordinal, this)
        }

    }

    private fun markAttendenceModel(): RequestModel {
        var locationId = "1"
        for (data in allVillageList) {
            if ((data.location_name!!+" "+data.external_id1).equals(attendenceViewModel.village.value)) {
                locationId = data.location_id!!
            }
        }
        return RequestModel(
            project = userInfo.projectName,
            location_id = locationId,
            village_name = attendenceViewModel.village.value,
            lattitude = attendenceViewModel.lattitude.value,
            longitude = attendenceViewModel.longitude.value,
            photo_url1 = attendenceViewModel.imageUrl1API.value,
            photo_url1_description = "selfie_at_village",
            attendance_type =  "PUNCH-OUT"
        )
    }

    fun getAttendenceForm() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                getAttendenceFormModel(),
                ApiExtentions.ApiDef.ATTENDENCE_FORM.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.ATTENDENCE_FORM.ordinal, this)
        }

    }

    private fun getAttendenceFormModel(): RequestModel {
        return RequestModel(
            projectId = userInfo.projectId,
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

    private fun uploadImageModel(): RequestModel {
        var fileName: String = ""
        val visitPrefix = "project_" + userInfo.projectName;
        if (imageIndex == 0) {
            fileName = visitPrefix + "_team_selfie_at_the_location.jpeg";
        }
        return RequestModel(
            project = userInfo.projectName,
            uploadFor = "attendance",
            filename = fileName,
        )
    }

    private fun getTag(string: String): String {
        return string.replace(" ", "_").lowercase()
    }

    private fun deleteImage(uri: Uri) {
        try {
            val resolver = requireActivity().contentResolver
            val rowsDeleted = resolver.delete(uri, null, null)
            if (rowsDeleted > 0) {
                // File successfully deleted
            } else {
                // File not found or couldn't be deleted
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the error
        }
    }

    override fun onApiSuccess(o: String?, objectType: Int) {

        cancelProgressDialog()
        when (ApiExtentions.ApiDef.entries[objectType]) {

            ApiExtentions.ApiDef.VILLAGE_LIST -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
                    val visitListFrom: ArrayList<ProjectInfo> =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    allVillageList = visitListFrom
                    val visitListFromBE = ArrayList<String>()
                    for (data in visitListFrom) {
                        visitListFromBE.add(data.location_name!!+" "+data.external_id1)
                    }
                    val responseArrayAdapter =
                        ArrayAdapter(
                            requireActivity(),
                            R.layout.list_popup_window_item,
                            visitListFromBE
                        )
                    binding.response.setAdapter(responseArrayAdapter)


                } else {
                    nonredirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            ApiExtentions.ApiDef.ATTENDENCE_FORM -> {
                val model = JSONObject(o.toString())
//                binding.image1Description.text =
//                    model.getJSONObject("data").getJSONArray("form_fields").getJSONObject(0)
//                        .getString("form_field_title")
//                binding.image2Description.text =
//                    model.getJSONObject("data").getJSONArray("form_fields").getJSONObject(1)
//                        .getString("form_field_title")
//
//                attendenceViewModel.imageType1.value =
//                    model.getJSONObject("data").getJSONArray("form_fields").getJSONObject(0)
//                        .getString("input_type")
//                attendenceViewModel.imageType2.value =
//                    model.getJSONObject("data").getJSONArray("form_fields").getJSONObject(1)
//                        .getString("input_type")

            }

            ApiExtentions.ApiDef.PUNCH_OUT -> {
                val model = JSONObject(o.toString())
                /*if (attendenceViewModel.projectInfo.value!!.project_name != null) {
                    redirectToCurriculam(attendenceViewModel.projectInfo.value!!)
                } else {
                    requireActivity().onBackPressed()
                }*/
                binding.llVisitSuccessToast.visibility = View.VISIBLE
                userInfo.localPunchOut = ""
                userInfo.didUserPunchedOut = true
                val mDelay: Long = 2000
                Handler(Looper.getMainLooper()).postDelayed({
                    redirectToDashboard()
                }, mDelay)

            }

            ApiExtentions.ApiDef.UPLOAD_IMAGE -> {
                val model = JSONObject(o.toString())
                val uploadImageData = Gson().fromJson(
                    model.getJSONObject("data").toString(),
                    UploadImageData::class.java
                )
                    attendenceViewModel.imageUrl1API.value = uploadImageData.url

                    deleteImage(attendenceViewModel.imageUrl1.value?.toUri()!!)

                    markAttendence()
            }

            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }
    }

    private fun redirectToDashboard() {
        val intent = Intent(activity, SKBMobiliserDashboard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun onApiError(message: String?) {
        nonredirectionAlertDialogue(requireContext(), message!!)
    }

    override fun retry(type: Int) {

        when (ApiExtentions.ApiDef.entries[type]) {
            ApiExtentions.ApiDef.PUNCH_OUT -> markAttendence()
            ApiExtentions.ApiDef.ATTENDENCE_FORM -> getAttendenceForm()
            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }

    }

    private fun redirectToCamera(position: Int, imageType: String, heading: String) {
        val intent = Intent(activity, CameraActivity::class.java)
        intent.putExtra("position", position)
        intent.putExtra("imageType", imageType)
        intent.putExtra("heading", heading)
        startImageCapture.launch(intent)
    }

    private fun redirectToCurriculam(projectInfo: ProjectInfo) {
        val intent = Intent(activity, Curriculam::class.java)
        intent.putExtra("projectInfo", Gson().toJson(projectInfo))
        startActivity(intent)
    }

    //private val startImageCapture = registerForActivityResult(CameraActivity::class.java)

    var startImageCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data

                    attendenceViewModel.imageUrl1.value = result.data!!.getStringExtra("imageUrl")
            }
        }

    companion object {
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
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
    }

}