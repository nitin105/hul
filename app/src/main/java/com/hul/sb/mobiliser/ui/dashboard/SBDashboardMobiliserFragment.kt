package com.hul.sb.mobiliser.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.OnFocusChangeListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.hul.curriculam.ui.schoolCode.SchoolCodeAdapter
import com.hul.data.Attendencemodel
import com.hul.data.CodeList
import com.hul.data.District
import com.hul.data.PerformanceData
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.data.State
import com.hul.data.UploadImageData
import com.hul.databinding.FragmentSBDashboardMobiliserBinding
import com.hul.sb.SBDashboardComponent
import com.hul.screens.field_auditor_dashboard.ui.school_activity.SchoolActivityFragment
import com.hul.sync.VisitDataTable
import com.hul.sync.VisitDataViewModel
import com.hul.user.UserInfo
import com.hul.utils.ConnectionDetector
import com.hul.utils.RetryInterface
import com.hul.utils.UserTypes
import com.hul.utils.cancelProgressDialog
import com.hul.utils.noInternetDialogue
import com.hul.utils.redirectToLogin
import com.hul.utils.redirectionAlertDialogue
import com.hul.utils.setProgressDialog
import org.json.JSONObject
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject


class SBDashboardMobiliserFragment : Fragment(), ApiHandler, RetryInterface, DashboardFragmentInterface,
    ListDialogInterface {

    private var binding: FragmentSBDashboardMobiliserBinding? = null

    private lateinit var sbDashboardComponent: SBDashboardComponent

    @Inject
    lateinit var dashboardViewModel: DashboardViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var visitDataViewModel: VisitDataViewModel

    @Inject
    lateinit var apiController: APIController

    private var imageIndex: Int = 0

    private var houseCodAdded: String = ""

    private var isSyncing: Boolean = false

    private var visitDataTableUploading: VisitDataTable? = null

    private var requestModel: RequestModel? = null

    val perforManceList = arrayListOf("Till Date", "Today", "Yesterday", "This Week", "This Month")

    var perfSelectedposition = 0
    @Inject
    lateinit var uploadFileController: UploadFileController

    var adapter: SchoolCodeAdapter? = null

    //var schoolCodes: ArrayList<SchoolCode> = ArrayList()

    private var syncDataList: ArrayList<VisitDataTable> = ArrayList()

    var visitList: ArrayList<ProjectInfo> = ArrayList()

    var projectInfoCompleted = ProjectInfo()

    private var districtList: ArrayList<District> = ArrayList()
    private var stateList: ArrayList<State> = ArrayList()

    var districtCallBack: ListDialogInterface? = null;

    var stateCallBack: ListDialogInterface? = null;

    //private var syncDataList: ArrayList<VisitDataTable> = ArrayList()

    private var currentLocation: Location? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private lateinit var allVisitList: ArrayList<ProjectInfo>

    private val handler = Handler(Looper.getMainLooper())

    private var isAddSchoolFlow = false

    private lateinit var audioFilePath: String

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSBDashboardMobiliserBinding.inflate(inflater, container, false)
        val root: View = binding!!.root

        binding!!.lifecycleOwner = viewLifecycleOwner
        sbDashboardComponent =
            (activity?.application as HULApplication).appComponent.sbDashboardComponent()
                .create()
        sbDashboardComponent.inject(this)
        binding!!.viewModel = dashboardViewModel

        binding!!.locationToVisit.layoutManager = LinearLayoutManager(context)

        binding!!.myArea.text = userInfo.myArea

        binding!!.punchInButton.setOnClickListener {
            redirectToAttendence(ProjectInfo(location_id = "1"))
        }

        binding!!.dayToday.text = dayOfWeek()
        binding!!.date.text = formatDate(Date(), "dd MMM yyyy")
        binding!!.txtLatter.text = userInfo.projectName.trim().split("")[1].uppercase()

        binding!!.rlProfile.setOnClickListener {
            showCustomDialog()
        }
        val pInfo: PackageInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0)
        binding!!.txtVersionName.text = "v"+pInfo.versionName

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                Log.e("Location :: ", locationResult.lastLocation.toString())

                // Normally, you want to save a new location to a database. We are simplifying
                // things a bit and just saving it as a local variable, as we only need it again
                // if a Notification is created (when the user navigates away from app).
                currentLocation = locationResult.lastLocation
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }

        binding!!.llGetDirection.setOnClickListener {
//            if (currentLocation != null && selectedSchoolCode != null) {
//                selectedSchoolCode!!.longitude?.let { it1 ->
//                    selectedSchoolCode!!.lattitude?.let { it2 ->
//                        openGoogleMapsForDirections(
//                            currentLocation!!.latitude,
//                            currentLocation!!.longitude,
//                            it2,
//                            it1
//                        )
//                    }
//                }
//            }
        }

//        binding!!.addHouseHold.setOnClickListener {
//            getStateList()
//        }

        binding!!.schoolCode.onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                binding!!.llGetDirection.visibility = GONE
            } else {
                hideKeyboard(view)
            }
        }

        binding!!.schoolCode.setOnItemClickListener { parent, view, position, id ->
            binding!!.llGetDirection.visibility =
                if (allVisitList[position].lattitude == null) GONE else VISIBLE
            binding!!.schoolCode.setText(allVisitList[position].external_id1)
            binding!!.schoolCode.clearFocus()
        }

        binding!!.schoolCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Code to execute before the text is changed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Code to execute when the text is changed



            }

            override fun afterTextChanged(s: Editable?) {
                // Code to execute after the text is changed

                var visitListFromBE =  ArrayList<ProjectInfo>()
                if(s.toString().length > 0) {
                    for (visitList in allVisitList) {
                        if (visitList.external_id1 != null && visitList.external_id1!!.contains(s.toString())) {
                            visitListFromBE.add(visitList)
                        }

                    }
                }else{
                    visitListFromBE = allVisitList
                }
                val myVisitsAdapter =
                    MyVisitsAdapter(
                        visitListFromBE,
                        this@SBDashboardMobiliserFragment,
                        requireContext()
                    )

                // Setting the Adapter with the recyclerview
                binding!!.locationToVisit.adapter = myVisitsAdapter
            }
        })


        if (allPermissionsGranted()) {
            checkLocationSettings()
        } else {
            requestPermission()
        }


        binding!!.tillDateButton.setOnClickListener { showPerfDialog() }



        return root
    }


    private fun showAddSchoolDialog() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
        val inflater = getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.add_housel_dialog, null)
        builder.setView(dialogView)
        val alertDialog: android.app.AlertDialog = builder.create()

        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose);
        val btnAdd = dialogView.findViewById<AppCompatButton>(R.id.btnAdd);

        val edtSchoolCode = dialogView.findViewById<TextInputEditText>(R.id.edtSchoolCode);




        btnAdd.setOnClickListener {

            val schoolCode = edtSchoolCode.text.toString()

            if (schoolCode.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all inputs", Toast.LENGTH_LONG).show()
            } else if (!isValidSchoolCode(schoolCode)) {
                Toast.makeText(
                    requireContext(),
                    "House code can only be alpha-numeric value",
                    Toast.LENGTH_LONG
                ).show()
            } else {

                alertDialog.dismiss()

                val addSchoolModel = RequestModel(
                    location_name = schoolCode,
                    area_id = userInfo.areaId.toDouble().toInt().toString(),
                    project_id = userInfo.projectId,
                    location_type = "House",
                    lattitude = currentLocation?.latitude.toString(),
                    longitude = currentLocation?.longitude.toString(),
                    external_id1 = schoolCode,
                    external_id1_description = "House Code",
                    external_id2 = null,
                    external_id2_description = "Temp Code",
                    location_ward = "wardBlock",
                    location_district = "",
                    location_state = "",
                    remarks = ""
                )

                apiController.getApiResponse(
                    this,
                    addSchoolModel,
                    ApiExtentions.ApiDef.ADD_NEW_SCHOOL.ordinal
                )
            }
        }

        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding!!.syncNow.setOnClickListener {
            if (syncDataList.isNotEmpty()) {
                binding!!.syncNow.isClickable = false
                binding!!.syncNow.isEnabled = false
                setProgressDialog(requireContext(), "Syncing Data")
                binding!!.syncNow.isEnabled = false
                binding!!.syncNow.isClickable = false
                startSync(syncDataList[syncDataList.size - 1])
            }
        }
        //visitDataViewModel.insert(VisitDataTable(jsonData = "Nitin", visitNumber = 1, project = "Test", uDiceCode = "retest"))

    }

    private fun fetchVisitData() {
        if (isSyncing) {
            if (syncDataList.isNotEmpty()) {
                startSync(syncDataList[syncDataList.size - 1])
            } else {
                binding!!.syncNow.visibility = View.GONE
                isSyncing = false
                binding!!.syncNow.isEnabled = true
                binding!!.syncNow.isClickable = true
                binding!!.syncNow.isClickable = true
                binding!!.syncNow.isEnabled = true
                cancelProgressDialog()
                showViewTemporarily(binding!!.llVisitSuccessToast, 2000)
                getTodaysVisit()
            }
        }
    }

    private fun startSync(visitDataTable: VisitDataTable) {
        imageIndex = 0
        isSyncing = true
        visitDataTableUploading = visitDataTable
        requestModel = Gson().fromJson(visitDataTable.jsonData, RequestModel::class.java)
        Log.d("TAG", "startSync: ${requestModel}")
        Log.d("TAG", "startSync: ${requestModel!!.visitData!!.visit_image_1!!.value.toString().toUri()}")
        uploadImage(requestModel!!.visitData!!.visit_image_1!!.value.toString().toUri(), requestModel!!.visit_number!!)
    }

    private fun uploadImage(imageUri: Uri, visitNumber: String) {
        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            uploadFileController.getApiResponse(
                this,
                imageUri,
                uploadImageModel(visitNumber),
                ApiExtentions.ApiDef.UPLOAD_IMAGE.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.UPLOAD_IMAGE.ordinal, this)
        }
    }
/*
    private fun uploadImageModel(visitNumber: String): RequestModel {
        var fileName: String = ""
        val visitPrefix = "project_" + userInfo.projectName;
        if (visitNumber.toInt() == 1) {
            when (imageIndex) {
                0 -> {
                    fileName = visitPrefix + "_image_of_communicator_while_conducting_activity.jpeg";
                }

                1 -> {
                    fileName = visitPrefix + "_communicator_visit_audio_recording.mp3";
                }

                2 -> {
                    fileName =
                        visitPrefix + "_long_shot_of_mobiliser_handing_over_laundry_discount_coupon.jpeg";
                }

                3 -> {
                    fileName = visitPrefix + "_selfie_image_of_communicator_image_with_the_house_code_sticker_on_door.jpeg";
                }

                4 -> {
                    fileName = visitPrefix + "_consent_form.jpeg";
                }
            }
        } else if (visitNumber.toInt() == 2) {
            when (imageIndex) {
                0 -> {
                    fileName = visitPrefix + "_selfie_image_outside_the_door_with_visible_door_sticker_from_visit_1.jpeg";
                }

                1 -> {
                    fileName = visitPrefix + "_germ_glow_image.jpeg";
                }

                2 -> {
                    fileName =
                        visitPrefix + "_activity_image_while_conducting_the_activity_along_with_the_flipchart.jpeg";
                }

                3 -> {
                    fileName = visitPrefix + "_door_sticker_with_updated_date_of_Visit_2.jpeg";
                }

                4 -> {
                    fileName = visitPrefix + "_acknowledgement_letter.jpeg";
                }
            }
        } else {
            when (imageIndex) {
                0 -> {
                    fileName = visitPrefix + "_selfie_image_outside_the_Door_sticker_image_with_visit_2_date_visible.jpeg";
                }

                1 -> {
                    fileName = visitPrefix + "_water_demo_with_glass_and_mud.jpeg";
                }

                2 -> {
                    fileName =
                        visitPrefix + "_nutrition_sticker_at_kitchen.jpeg";
                }

                3 -> {
                    fileName = visitPrefix + "_image_of_acknowledgement_letter.jpeg";
                }

                4 -> {
                    fileName = visitPrefix + "_acknowledgement_letter.jpeg";
                }
            }
        }

        return RequestModel(
            project = userInfo.projectName,
            uploadFor = "field_audit",
            filename = fileName,
            visit_id = requestModel!!.visit_id
        )
    }

 */

private fun uploadImageModel(visitNumber: String): RequestModel {
    var fileName: String = ""
    val visitPrefix = "project_" + userInfo.projectName;
    if (visitNumber.toInt() == 1) {
        when (imageIndex) {
            0 -> {
                fileName = visitPrefix + "_selfie_image_outside_the_door_before_entering_the_house.jpeg";
            }

            1 -> {
                fileName = visitPrefix + "_long_shot_of_mobiliser_showing_suvidha_photobook_in_the_household.jpeg";
            }

            2 -> {
                fileName =
                    visitPrefix + "_long_shot_of_mobiliser_handing_over_laundry_discount_coupon.jpeg";
            }

            3 -> {
                fileName = visitPrefix + "_Identifier_Sticker_with_activity_date_mentioned.jpeg";
            }
        }
    } else if (visitNumber.toInt() == 2) {
        when (imageIndex) {
            0 -> {
                fileName = visitPrefix + "_selfie_image_outside_the_door_before_entering_the_house_with_visible_sticker_of_1st_and_2nd_visit_with_date.jpeg";
            }

            1 -> {
                fileName = visitPrefix + "_germ_glow_image.jpeg";
            }

            2 -> {
                fileName =
                    visitPrefix + "_activity_image_while_conducting_the_activity_along_with_the_flipchart.jpeg";
            }

            3 -> {
                fileName = visitPrefix + "_identifier_sticker_with_activity_date_mentioned.jpeg";
            }
        }
    } else {
        when (imageIndex) {
            0 -> {
                fileName = visitPrefix + "_selfie_image_outside_the_door_before_entering_the_house_with_visible_sticker_of_visit_1_or_door_marking_with_marker.jpeg";
            }

            1 -> {
                fileName = visitPrefix + "_water_demo_with_glass_and_mud.jpeg";
            }

            2 -> {
                fileName =
                    visitPrefix + "_nutrition_sticker_at_kitchen.jpeg";
            }

            3 -> {
                fileName = visitPrefix + "_identifier_sticker_with_activity_date_mentioned.jpeg";
            }
        }
    }

    return RequestModel(
        project = userInfo.projectName,
        uploadFor = "field_audit",
        filename = fileName,
        visit_id = requestModel!!.visit_id
    )
}
    fun submitForm(requestModel: RequestModel) {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                requestModel,
                ApiExtentions.ApiDef.VISIT_DATA.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.VISIT_DATA.ordinal, this)
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getPerformanceModel(filter :String): RequestModel {
        return RequestModel(
            date_filter = filter
        )
    }

    private fun getPerformance(filter :String) {
        apiController.getApiResponse(
            this,
            getPerformanceModel(filter),
            ApiExtentions.ApiDef.GET_PERFORMANCE.ordinal
        )
    }

    private fun getTodaysVisitModel(): RequestModel {
        return RequestModel(
            status = "ASSIGNED",
            userType= UserTypes.MOBILISER


        )
    }

    private fun getTodaysVisit() {
        apiController.getApiResponse(
            this,
            getTodaysVisitModel(),
            ApiExtentions.ApiDef.VISIT_LIST_BY_STATUS2.ordinal
        )
    }

    private fun getTodaysVisitModel2(): RequestModel {
        return RequestModel(
            status = "SUBMITTED"
        )
    }

    private fun getTodaysVisit2() {
        apiController.getApiResponse(
            this,
            getTodaysVisitModel2(),
            ApiExtentions.ApiDef.VISIT_LIST_BY_STATUS.ordinal
        )
    }

    private fun getListOfCodeModel(): RequestModel {
        return RequestModel(
            areaId = userInfo.areaId
        )
    }

    private fun getListOfCode() {
        apiController.getApiResponse(
            this,
            getListOfCodeModel(),
            ApiExtentions.ApiDef.LIST_OF_CODES.ordinal
        )
    }

    // Function to hide the keyboard
    private fun hideKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun getSchoolCodes(s: String) {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                getSchoolCodesModel(s),
                ApiExtentions.ApiDef.SCHOOL_CODES.ordinal
            )
        } else {
            //noInternetDialogue(requireContext(), ApiExtentions.ApiDef.SCHOOL_CODES.ordinal, this)
        }

    }

    private fun showDistrictDialog(districtList: ArrayList<District>) {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
        val inflater = getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.list_dialog, null)
        builder.setView(dialogView)
        val alertDialog: android.app.AlertDialog = builder.create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView);
        val adapter: DistrictsAdapter =
            DistrictsAdapter(districtList, object : ListDialogInterface {
                override fun onDistrictSelect(district: District) {
                    districtCallBack?.onDistrictSelect(district)
                    alertDialog.dismiss()
                }

                override fun onStateSelect(state: State) {

                }
            }, requireActivity())

        recyclerView.adapter = adapter

        alertDialog.show()
    }

    private fun showStateDialog(stateList: ArrayList<State>) {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
        val inflater = getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.list_dialog, null)
        builder.setView(dialogView)
        val alertDialog: android.app.AlertDialog = builder.create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView);
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle);

        dialogTitle.text = "Select State"

        val adapter: StateAdapter =
            StateAdapter(stateList, object : ListDialogInterface {
                override fun onDistrictSelect(district: District) {

                }

                override fun onStateSelect(state: State) {
                    stateCallBack?.onStateSelect(state)
                    alertDialog.dismiss()
                }
            }, requireActivity())

        recyclerView.adapter = adapter

        alertDialog.show()
    }

    private fun getSchoolCodesModel(s: String): RequestModel {
        return RequestModel(
            projectId = userInfo.projectId,
            externalId = s
        )
    }

    private fun getSchoolVisits(schoolId: Int) {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                getSVisitsBySchoolCode(schoolId),
                ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE.ordinal
            )
        } else {
//            noInternetDialogue(
//                requireContext(),
//                ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE.ordinal,
//                this
//            )
        }

    }

    private fun getSchoolVisitsCompleted(schoolId: Int) {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                getSVisitsBySchoolCode(schoolId),
                ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE_Completed.ordinal
            )
        } else {
//            noInternetDialogue(
//                requireContext(),
//                ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE_Completed.ordinal,
//                this
//            )
        }

    }

    private fun getDistrictList() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            apiController.getApiResponse(
                this,
                getDistricts(userInfo.projectId),
                ApiExtentions.ApiDef.GET_DISTRICTS.ordinal
            )
        } else {
//            noInternetDialogue(
//                requireContext(),
//                ApiExtentions.ApiDef.GET_DISTRICTS.ordinal,
//                this
//            )
        }

    }

    private fun getStateList() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            apiController.getApiResponse(
                this,
                getDistricts(userInfo.projectId),
                ApiExtentions.ApiDef.GET_STATES.ordinal
            )
        } else {
//            noInternetDialogue(
//                requireContext(),
//                ApiExtentions.ApiDef.GET_STATES.ordinal,
//                this
//            )
        }

    }


    private fun getSVisitsBySchoolCode(id: Int): RequestModel {
        return RequestModel(
            schoolId = id,
        )
    }

    private fun getDistricts(id: String): RequestModel {
        return RequestModel(
            projectId = "1",
        )
    }

    private fun addVisit(id: String, visitNumber: String) {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                addVisitModel(id, visitNumber),
                ApiExtentions.ApiDef.ADD_VISIT.ordinal
            )
        } else {
//            noInternetDialogue(
//                requireContext(),
//                ApiExtentions.ApiDef.ADD_VISIT.ordinal,
//                this
//            )
        }

    }

    private fun addVisitModel(id: String, visitNumber: String): RequestModel {
        return RequestModel(
            location_id = id,
            visit_number = visitNumber,
            visit_status = "ASSIGNED"
        )
    }

    private fun formatDate(date: Date, format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun dayOfWeek(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
            Calendar.SUNDAY -> "SUNDAY"
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            else -> "UNKNOWN"
        }
    }

    fun getDayOfWeek(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
            Calendar.SUNDAY -> "SUNDAY"
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            else -> "UNKNOWN"
        }
    }

    private fun showCustomDialog() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
        val inflater = getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.profile_dialog, null)
        builder.setView(dialogView)
        val alertDialog: android.app.AlertDialog = builder.create()

        val llLogout = dialogView.findViewById<LinearLayout>(R.id.llLogOut);
        val txtMobiliserName = dialogView.findViewById<TextView>(R.id.txtMobiliserName)
        txtMobiliserName.text = userInfo.projectName

        llLogout.setOnClickListener {
            alertDialog.dismiss()
            userInfo.authToken = ""
            redirectToLogin(requireContext())
        }

        alertDialog.show()
    }

    private fun showPerfDialog() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
        val inflater = getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.perf_dialog, null)
        builder.setView(dialogView)
        val alertDialog: android.app.AlertDialog = builder.create()

        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val locationToVisit = dialogView.findViewById<RecyclerView>(R.id.locationToVisit);
        locationToVisit.layoutManager = LinearLayoutManager(context)
        val myVisitsAdapter =
            MyPerfAdapter(perforManceList, perfSelectedposition,object : PerfInterface {
                override fun onSelected(position: Int) {
                    perfSelectedposition = position
                    binding!!.tillDateButton.text = perforManceList[position]
                    when(position){
                        0->{getPerformance("till_date")}
                        1->{getPerformance("today")}
                        2->{getPerformance("yesterday")}
                        3->{getPerformance("this_week")}
                        4->{getPerformance("this_month")}
                    }
                    alertDialog.cancel()
                }

            } , requireContext())
        // Setting the Adapter with the recyclerview
        locationToVisit.adapter = myVisitsAdapter


        alertDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    fun updateLocalList() {
        visitDataViewModel.allSyncData.observe(requireActivity()) { visitDataList ->
            syncDataList = ArrayList(visitDataList)
            binding!!.visitsLeft.text =
                visitDataList.size.toString() + " " + requireActivity().getString(R.string.todays_visit_left)
        }

        loadLocations()
    }

    override fun onResume() {
        super.onResume()

        updateLocalList()
        binding!!.txtProfileName.text = "Hi, " + userInfo.userFullname


    }

    private fun loadLocations() {

//        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
//            setProgressDialog(requireContext(), "Loading Leads")
//            apiController.getApiResponse(
//                this,
//                loadLocationsModel(),
//                ApiExtentions.ApiDef.VISIT_LIST.ordinal
//            )
//        } else {
//            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.VISIT_LIST.ordinal, this)
//        }

        if (dashboardViewModel.attendenceToday.value == null) {
            getAttendence()
        }
        else{
            getTodaysVisit()
        }

    }

    private fun loadLocationsModel(): RequestModel {
        return RequestModel(
            projectId = userInfo.projectId
        )
    }

    private fun getAttendence() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            apiController.getApiResponse(
                this,
                getAttendenceModel(),
                ApiExtentions.ApiDef.GET_ATTENDENCE.ordinal
            )
        } else {
            //noInternetDialogue(requireContext(), ApiExtentions.ApiDef.GET_ATTENDENCE.ordinal, this)
        }

    }

    private fun getAttendenceModel(): RequestModel {
        return RequestModel(
            projectId = userInfo.projectId
        )
    }


    override fun onApiSuccess(o: String?, objectType: Int) {


        when (ApiExtentions.ApiDef.entries[objectType]) {

            ApiExtentions.ApiDef.ADD_NEW_SCHOOL -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    Toast.makeText(requireContext(), "House added successfully", Toast.LENGTH_LONG)
                        .show()
                    houseCodAdded = model.getInt("data").toString()
                    addVisit(model.getInt("data").toString(),"1")
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.ADD_VISIT -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    redirectToAttendence(ProjectInfo(location_id = houseCodAdded))
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.GET_STATES -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<State?>?>() {}.type
                    stateList =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    getDistrictList()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.GET_DISTRICTS -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<District?>?>() {}.type
                    districtList =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    showAddSchoolDialog()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.GET_ATTENDENCE -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<Attendencemodel?>?>() {}.type
                    val items: ArrayList<Attendencemodel> =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    val currentObject = items.get(items.size - 1)
                    dashboardViewModel.attendenceToday.value = currentObject
                    try {
                        items.removeAt(items.size - 1)
                        // Remove the first element
                        items.removeAt(0)
                    } catch (e: Exception) {
                        Log.e(SBDashboardMobiliserFragment::class.qualifiedName, "onApiSuccess: " + e.message)
                    }
                    val adapter = AttendenceAdapter(requireContext(), items)
                    binding!!.gridView.adapter = adapter
                    if (currentObject.date != null && currentObject.date?.length!! > 10) {

                        binding!!.time.text = currentObject.date!!.substring(
                            11,
                            currentObject.date!!.length
                        )
                    }

                    if (currentObject.present != null && currentObject.present!!) {
                        binding!!.punchInButton.visibility = GONE
                        binding!!.punchInButtonDisabled.visibility = VISIBLE
                        binding!!.punchInButton.isEnabled = false
                    } else {
                        binding!!.punchInButton.visibility = VISIBLE
                    }

                    getPerformance("till_date")

                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            ApiExtentions.ApiDef.GET_PERFORMANCE -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {

                    val performanceData = Gson().fromJson(
                        model.getJSONObject("data").toString(),
                        PerformanceData::class.java
                    )
                    when (perfSelectedposition) {
                        0 -> {
                            binding!!.txtVisits.text =
                                performanceData.till_date.total_visits.toString()
                            binding!!.txtAttendance.text =
                                String.format("%.2f", performanceData.till_date.attendance)+ "%"
                            binding!!.txtTotalVisits.text =
                                String.format("%.2f", performanceData.till_date.audit_approval) + "%"
                        }

                        1 -> {
                            binding!!.txtVisits.text =
                                performanceData.today.total_visits.toString()
                            binding!!.txtAttendance.text =
                                performanceData.today.attendance.toString() + "%"
                            binding!!.txtTotalVisits.text =
                                performanceData.today.audit_approval.toString() + "%"
                        }

                        2 -> {
                            binding!!.txtVisits.text =
                                performanceData.yesterday.total_visits.toString()
                            binding!!.txtAttendance.text =
                                performanceData.yesterday.attendance.toString() + "%"
                            binding!!.txtTotalVisits.text =
                                performanceData.yesterday.audit_approval.toString() + "%"
                        }

                        3 -> {
                            binding!!.txtVisits.text =
                                performanceData.this_week.total_visits.toString()
                            binding!!.txtAttendance.text =
                                performanceData.this_week.attendance.toString() + "%"
                            binding!!.txtTotalVisits.text =
                                performanceData.this_week.audit_approval.toString() + "%"
                        }

                        4 -> {
                            binding!!.txtVisits.text =
                                performanceData.this_month.total_visits.toString()
                            binding!!.txtAttendance.text =
                                performanceData.this_month.attendance.toString() + "%"
                            binding!!.txtTotalVisits.text =
                                performanceData.this_month.audit_approval.toString() + "%"
                        }
                    }


                    getTodaysVisit()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE_Completed -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())

                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type

                    visitList =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    visitList.forEachIndexed{index ,item ->
                        visitList[index].visit_status = "SUBMITTED"
                    }
                    //redirectToCompleted(projectInfoCompleted)

                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            ApiExtentions.ApiDef.VISIT_LIST_BY_STATUS2 -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
                    val visitListFrom: ArrayList<ProjectInfo> =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    val visitListFromBE = ArrayList<ProjectInfo>()
                    for(visitList in visitListFrom)
                    {   val visitData = syncDataList.find { sync -> Gson().fromJson(sync.jsonData,RequestModel::class.java).visit_id!!.toInt() == visitList.visit_id }
                        if (visitData == null) {
                            visitListFromBE.add(visitList)
                        }

                    }
                    allVisitList = visitListFromBE
                    val myVisitsAdapter =
                        MyVisitsAdapter(
                            visitListFromBE,
                            this,
                            requireContext()
                        )

                    // Setting the Adapter with the recyclerview
                    binding!!.locationToVisit.adapter = myVisitsAdapter
                    getTodaysVisit2()


                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            ApiExtentions.ApiDef.VISIT_LIST_BY_STATUS -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
                    val visitListFromBE: ArrayList<ProjectInfo> =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);



                    if (visitListFromBE.size > 0 || syncDataList.isNotEmpty()) {

                        // Change visit total dynamically


                        if (syncDataList.isNotEmpty()) {
                            for (data in syncDataList) {
                                val requestModel = Gson().fromJson(data.jsonData, RequestModel::class.java)
                                val projectInfo = ProjectInfo(
                                    visit_number = data.visitNumber.toString(),
                                    location_name = data.locationName,
                                    visit_status = "SUBMITTED",
                                    location_id = data.locationId,
                                    localString = data.jsonData,
                                    external_id1 = requestModel.visitData!!.house_code!!.value.toString(),
                                    visit_id = Gson().fromJson(data.jsonData,RequestModel::class.java).visit_id!!.toInt()
                                )
                                visitListFromBE.add(projectInfo)
                            }
                        } else {
                            binding!!.syncNow.visibility = View.GONE
                        }

                        val myVisitsAdapter =
                            MyVisitsAdapter(
                                visitListFromBE,
                                this,
                                requireContext()
                            )

                        // Setting the Adapter with the recyclerview
                        binding!!.todaysVisit.adapter = myVisitsAdapter
                    } else {
                        binding!!.todaysVisitParent.visibility = View.GONE
                    }

                    getListOfCode()

                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            ApiExtentions.ApiDef.LIST_OF_CODES -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<CodeList?>?>() {}.type
                    val codeList: ArrayList<CodeList> =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    userInfo.codeList = Gson().toJson(codeList)
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
                    requestModel!!.visitData!!.visit_image_1!!.value = uploadImageData.url
                    uploadImage(requestModel!!.visitData!!.visit_image_2!!.value.toString().toUri(), requestModel!!.visit_number!!)
                } else if (uploadImageData != null && imageIndex == 1) {
                    imageIndex += 1;
                    requestModel!!.visitData!!.visit_image_2!!.value = uploadImageData.url
                    if (requestModel!!.visitData!!.visit_image_3 !=null && requestModel!!.visitData!!.visit_image_3!!.value.toString().length > 0) {
                        uploadImage(
                            requestModel!!.visitData!!.visit_image_3!!.value.toString().toUri(),
                            requestModel!!.visit_number!!
                        )

                    } else {
                        submitForm(requestModel!!)
                    }
                } else if (uploadImageData != null && imageIndex == 2) {
                    imageIndex += 1;
                    requestModel!!.visitData!!.visit_image_3!!.value = uploadImageData.url
                    if (requestModel!!.visitData!!.visit_image_4 !=null && requestModel!!.visitData!!.visit_image_4!!.value.toString().length > 0) {
                        uploadImage(
                            requestModel!!.visitData!!.visit_image_4!!.value.toString().toUri(),
                            requestModel!!.visit_number!!
                        )

                    } else {
                        submitForm(requestModel!!)
                    }
                }
                else if (uploadImageData != null && imageIndex == 3) {
                    imageIndex += 1;
                    requestModel!!.visitData!!.visit_image_4!!.value = uploadImageData.url
                    if (requestModel!!.visitData!!.visit_image_5 !=null && requestModel!!.visitData!!.visit_image_5!!.value.toString().length > 0) {
                        uploadImage(
                            requestModel!!.visitData!!.visit_image_5!!.value.toString().toUri(),
                            requestModel!!.visit_number!!
                        )
                    } else {
                        submitForm(requestModel!!)
                    }
                }
                else if (uploadImageData != null && imageIndex == 4) {
                    imageIndex += 1;
                    requestModel!!.visitData!!.visit_image_5!!.value = uploadImageData.url
                    submitForm(requestModel!!)
                }
            }

            ApiExtentions.ApiDef.VISIT_DATA -> {
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
//                    userInfo.didUserSubmitNewVisit = true
//                    val intent = Intent(activity, Dashboard::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    startActivity(intent)
//                    requireActivity().finish()
                    deleteImage(requestModel!!.visitData!!.visit_image_1!!.value.toString().toUri())
                    deleteImage(requestModel!!.visitData!!.visit_image_2!!.value.toString().toUri())
                    if (requestModel!!.visitData!!.visit_image_3 !=null && requestModel!!.visitData!!.visit_image_3!!.value.toString().length > 0) {
                        deleteImage(
                            requestModel!!.visitData!!.visit_image_3!!.value.toString().toUri()
                        )
                    }
                    if (requestModel!!.visitData!!.visit_image_4 !=null && requestModel!!.visitData!!.visit_image_4!!.value.toString().length > 0) {
                        deleteImage(
                            requestModel!!.visitData!!.visit_image_4!!.value.toString().toUri()
                        )
                    }
                    if (requestModel!!.visitData!!.visit_image_5 !=null && requestModel!!.visitData!!.visit_image_5!!.value.toString().length > 0) {
                        deleteImage(
                            requestModel!!.visitData!!.visit_image_5!!.value.toString().toUri()
                        )
                    }
                    visitDataViewModel.deleteById(visitDataTableUploading!!.id)
                    syncDataList.removeIf { it.id == visitDataTableUploading!!.id }
                    fetchVisitData()


                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }
    }

    override fun onApiError(message: String?) {
        if (message?.equals(context?.getString(R.string.session_expire))!!) {
            userInfo.authToken = ""
            redirectionAlertDialogue(requireContext(), message)
        } else {
            redirectionAlertDialogue(requireContext(), message)
        }
    }

    override fun retry(type: Int) {

        when (ApiExtentions.ApiDef.entries[type]) {
            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }

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

    override fun redirectToAttendence(projectInfo: ProjectInfo) {

        Log.d("projectInfo", "redirectToAttendence: ${projectInfo}")
        if (dashboardViewModel.attendenceToday.value?.present == true) {

            val bundle = Bundle()
            bundle.putString("projectInfo", Gson().toJson(projectInfo))
//                    val intent = Intent(activity, WebForm::class.java)
//                    intent.putExtras(bundle)
//                    startActivity(intent)
            findNavController().navigate(
                R.id.action_SBDashboardMobiliserFragment_to_SBPreFormFragment,
                bundle
            )
        } else {
            val bundle = Bundle()
            bundle.putString("projectInfo", Gson().toJson(projectInfo))
            findNavController().navigate(
                R.id.action_SBDashboardMobiliserFragment_to_attendenceFragment42,
                bundle
            )
        }


    }



    /*override fun redirectToAttendence(projectInfo: ProjectInfo) {

        if (dashboardViewModel.attendenceToday.value!!.present!!) {
            redirectToCurriculam(projectInfo)
        } else {
            val bundle = Bundle()
            bundle.putString("projectInfo", Gson().toJson(projectInfo))
            findNavController().navigate(
                R.id.action_dashboardFragment_to_attendenceFragment,
                bundle
            )
        }
    }*/

    override fun onDistrictSelect(district: District) {

    }

    override fun onStateSelect(state: State) {

    }

    private fun requestLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        requestLocation.launch(intent)
    }

    private fun requestPermission() {
        requestPermission.launch(SchoolActivityFragment.REQUIRED_PERMISSIONS)
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

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
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

    private fun isValidSchoolCode(input: String): Boolean {
        // Regular expression to check if the input is alphanumeric and
        // does not contain spaces or special characters
        val regex = "^[a-zA-Z0-9]*$".toRegex()
        return input.matches(regex)
    }

    private fun showViewTemporarily(view: View, duration: Long) {
        if (userInfo.didUserSubmitNewVisit) {
            view.visibility = View.VISIBLE
            handler.postDelayed({
                userInfo.didUserSubmitNewVisit = false
                view.visibility = View.GONE
            }, duration)
        }
    }
}