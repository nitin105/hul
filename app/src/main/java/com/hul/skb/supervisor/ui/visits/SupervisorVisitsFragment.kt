package com.hul.skb.supervisor.ui.visits

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.hul.data.Attendencemodel
import com.hul.data.MappedUser
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.databinding.FragmentSupervisorVisitsBinding
import com.hul.sb.SBSupervisorDashboardComponent
import com.hul.user.UserInfo
import com.hul.utils.ConnectionDetector
import com.hul.utils.RetryInterface
import com.hul.utils.SUBMITTED
import com.hul.utils.UserTypes.MOBILISER
import com.hul.utils.cancelProgressDialog
import com.hul.utils.noInternetDialogue
import com.hul.utils.redirectionAlertDialogue
import org.json.JSONObject
import java.lang.reflect.Type
import javax.inject.Inject

class SupervisorVisitsFragment : Fragment(), ApiHandler, RetryInterface,
    SupervisorVisitInterface {

    private var _binding: FragmentSupervisorVisitsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var dashboardComponent: SBSupervisorDashboardComponent

    @Inject
    lateinit var uploadFileController: UploadFileController

    @Inject
    lateinit var mobiliserVisitsViewModel: SupervisorVisitsViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var apiController: APIController

    lateinit var myVisitsAdapter: SupervisorVisitsAdapter;

    var visits: ArrayList<ProjectInfo> = ArrayList()

    var previousVisits: ArrayList<ProjectInfo> = ArrayList()

    var allVisits: ArrayList<ProjectInfo> = ArrayList()

    var projectInfoForRedirect: ProjectInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSupervisorVisitsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        dashboardComponent =
            (activity?.application as HULApplication).appComponent.sbSupervisorDashboardComponent()
                .create()
        dashboardComponent.inject(this)
        binding.viewModel = mobiliserVisitsViewModel

        binding.recyclerViewVisits.layoutManager = LinearLayoutManager(context)
        myVisitsAdapter = SupervisorVisitsAdapter(ArrayList(), this, requireContext())
        binding.recyclerViewVisits.adapter = myVisitsAdapter

        binding.stats.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.viewBluePending.visibility = View.VISIBLE
        binding.viewBlueCompleted.visibility = View.GONE
        binding.viewGrayPending.visibility = View.GONE
        binding.viewGrayCompleted.visibility = View.VISIBLE

        binding.txtPending.setTextColor(Color.parseColor("#2C41CA"))
        binding.txtCompleted.setTextColor(Color.parseColor("#2F2B3DE5"))

        binding.llPending.setOnClickListener {
            mobiliserVisitsViewModel.pendingSelected.value = true

            myVisitsAdapter.updateVisits(visits)

            binding.viewBluePending.visibility = View.VISIBLE
            binding.viewBlueCompleted.visibility = View.GONE
            binding.viewGrayPending.visibility = View.GONE
            binding.viewGrayCompleted.visibility = View.VISIBLE

            binding.txtPending.setTextColor(Color.parseColor("#2C41CA"))
            binding.txtCompleted.setTextColor(Color.parseColor("#2F2B3DE5"))
        }
        binding.llCompleted.setOnClickListener {
            mobiliserVisitsViewModel.pendingSelected.value = false


            myVisitsAdapter.updateVisits(previousVisits)

            binding.viewBluePending.visibility = View.GONE
            binding.viewBlueCompleted.visibility = View.VISIBLE
            binding.viewGrayPending.visibility = View.VISIBLE
            binding.viewGrayCompleted.visibility = View.GONE

            binding.txtCompleted.setTextColor(Color.parseColor("#2C41CA"))
            binding.txtPending.setTextColor(Color.parseColor("#2F2B3DE5"))
        }

        if (allPermissionsGranted()) {
            checkLocationSettings()
        } else {
            requestPermission()
        }

        getVisits()

        return root
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
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
            locationRequest, locationCallback, Looper.getMainLooper()
        )

        fetchLastKnownLocation()

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

    private fun fetchLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val location: Location? = task.result
                    currentLocation = location

                } else {
                    // Handle the case where no location is available
                }
            }
    }


    override fun onResume() {
        super.onResume()
    }

    private fun getVisitListModel(userType: String, mobiliserId: Int): RequestModel {
        return RequestModel(
            userType = MOBILISER, mobiliserId = mobiliserId
        )
    }

    private fun getVisits() {

        mobiliserVisitsViewModel.mobiliserUser.value =
            Gson().fromJson(requireArguments().getString("mobiliserData"), MappedUser::class.java)

        binding.txtMobiliserName.text = mobiliserVisitsViewModel.mobiliserUser.value?.user_fullname

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {

            mobiliserVisitsViewModel.mobiliserUser.value?.let {
                apiController.getApiResponse(
                    this, mobiliserVisitsViewModel.mobiliserUser.value?.let {
                        getVisitListModel(
                            userInfo.userType, it.user_id
                        )
                    }, ApiExtentions.ApiDef.VISIT_LIST_FIELD_AUDITOR.ordinal
                )
            }
        } else {
            noInternetDialogue(
                requireContext(), ApiExtentions.ApiDef.VISIT_LIST_FIELD_AUDITOR.ordinal, this
            )
        }

    }

    private fun getPreviousVisits() {

        mobiliserVisitsViewModel.mobiliserUser.value =
            Gson().fromJson(requireArguments().getString("mobiliserData"), MappedUser::class.java)

        binding.txtMobiliserName.text = mobiliserVisitsViewModel.mobiliserUser.value?.user_fullname

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {

            mobiliserVisitsViewModel.mobiliserUser.value?.let {
                apiController.getApiResponse(
                    this, mobiliserVisitsViewModel.mobiliserUser.value?.let {
                        getVisitListModel(
                            userInfo.userType, it.user_id
                        )
                    }, ApiExtentions.ApiDef.VISIT_LIST_PREVIOUS.ordinal
                )
            }
        } else {
            noInternetDialogue(
                requireContext(), ApiExtentions.ApiDef.VISIT_LIST_PREVIOUS.ordinal, this
            )
        }

    }

    private fun getVisitsBySchoolCode(id: Int): RequestModel {
        return RequestModel(
            schoolId = id,
        )
    }


    fun getSchoolVisits(schoolId: Int) {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                getVisitsBySchoolCode(schoolId),
                ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE.ordinal
            )
        } else {
            noInternetDialogue(
                requireContext(),
                ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE.ordinal,
                this
            )
        }

    }

    override fun onApiSuccess(o: String?, objectType: Int) {
        cancelProgressDialog()
        when (ApiExtentions.ApiDef.entries[objectType]) {

            ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE -> {
                val model = JSONObject(o.toString())

                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type

                    val visitList: ArrayList<ProjectInfo> =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);

                    projectInfoForRedirect?.let { redirectToSchoolActivity(it, "") }
                }
            }

            ApiExtentions.ApiDef.VISIT_LIST_FIELD_AUDITOR -> {
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
                    allVisits = Gson().fromJson(model.getJSONArray("data").toString(), listType);

//                    val listAfterIgnoreStatus = allVisits.filter { projectInfo ->
//                        projectInfo.visit_status != INITIATED
//                                && projectInfo.visit_status != ASSIGNED
//                                && projectInfo.visit_status != PARTIALLY_SUBMITTED
//                                && projectInfo.visit_status != SUB_AGENCY_REJECTED
//                    }

                    visits = allVisits as ArrayList<ProjectInfo>;

//                    val pendingVisits = allVisits.filter { projectInfo ->
//                        projectInfo.visit_status == SUBMITTED || projectInfo.visit_status == SUB_AGENCY_APPROVED
//                    }

                    myVisitsAdapter.updateVisits(visits)
                    getPreviousVisits()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }


            }

            ApiExtentions.ApiDef.VISIT_LIST_PREVIOUS -> {
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
                    allVisits = Gson().fromJson(model.getJSONArray("data").toString(), listType);

//                    val listAfterIgnoreStatus = allVisits.filter { projectInfo ->
//                        projectInfo.visit_status != INITIATED
//                                && projectInfo.visit_status != ASSIGNED
//                                && projectInfo.visit_status != PARTIALLY_SUBMITTED
//                                && projectInfo.visit_status != SUB_AGENCY_REJECTED
//                    }
                    allVisits.forEachIndexed{index, element ->
                        allVisits.get(index).visit_status = SUBMITTED
                    }
                    previousVisits = allVisits as ArrayList<ProjectInfo>;

//                    val pendingVisits = allVisits.filter { projectInfo ->
//                        projectInfo.visit_status == SUBMITTED || projectInfo.visit_status == SUB_AGENCY_APPROVED
//                    }

                    //myVisitsAdapter.updateVisits(previousVisits)
                    getAttendance()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }


            }

            ApiExtentions.ApiDef.GET_ATTENDENCE -> {
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<Attendencemodel?>?>() {}.type
                    val items: ArrayList<Attendencemodel> =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    val currentObject = items.get(items.size - 1)
                    mobiliserVisitsViewModel.attendenceToday.value = currentObject
                    items.removeAt(items.size - 1)

                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }
    }

    override fun onApiError(message: String?) {

    }

    override fun retry(type: Int) {

    }

    private fun getAttendance() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            apiController.getApiResponse(
                this,
                getAttendanceModel(),
                ApiExtentions.ApiDef.GET_ATTENDENCE.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.GET_ATTENDENCE.ordinal, this)
        }

    }

    private fun getAttendanceModel(): RequestModel {
        return RequestModel(
            projectId = userInfo.projectId
        )
    }

    fun redirectToAttendence(projectInfo: ProjectInfo) {
        val bundle = Bundle()
        bundle.putString("projectInfo", Gson().toJson(projectInfo))
        findNavController().navigate(
            R.id.action_supervisorVisitsFragment_to_SBSupervisorAttendenceFragment,
            bundle
        )

    }

    override fun redirectToSchoolActivity(
        projectInfo: ProjectInfo,
        heading : String
    ) {
        if (!mobiliserVisitsViewModel.attendenceToday.value!!.present!!) {
            redirectToAttendence(projectInfo)
        } else run {
            redirectToCamera(
                1,
                "Back",
                heading,
                projectInfo
            )
        }
    }

    private fun redirectToCamera(position: Int, imageType: String, heading: String,projectInfo: ProjectInfo) {
        val intent = Intent(activity, CameraActivity::class.java)
        intent.putExtra("position", position)
        intent.putExtra("imageType", imageType)
        intent.putExtra("heading", heading)
        intent.putExtra("project", "SKBSupervisor")
        intent.putExtra("projectInfo", Gson().toJson(projectInfo))
        intent.putExtra("mobilisername", mobiliserVisitsViewModel.mobiliserUser.value?.user_fullname)
        startImageCapture.launch(intent)
    }
    val startImageCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                val position = data!!.getIntExtra("position", 0)
                val imageUrl = result.data!!.getStringExtra("imageUrl")

                getVisits()
            }
        }



    override fun goToMap(projectInfo: ProjectInfo) {
        if (currentLocation != null) {
            projectInfo.longitude?.let { it1 ->
                projectInfo.lattitude?.let { it2 ->
                    openGoogleMapsForDirections(
                        currentLocation!!.latitude, currentLocation!!.longitude, it2, it1
                    )
                }
            }
        }
    }

    private fun openGoogleMapsForDirections(
        lat: Double, lng: Double, destinationLat: String, destinationLng: String
    ) {

        val destLat = destinationLat
        val destLng = destinationLng

        // Build the URI for the directions request
        val uri = Uri.parse("http://maps.google.com/maps?saddr=$lat,$lng&daddr=$destLat,$destLng")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    companion object {
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
    }

}