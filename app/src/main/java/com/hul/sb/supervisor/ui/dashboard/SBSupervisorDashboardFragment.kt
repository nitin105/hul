package com.hul.sb.supervisor.ui.dashboard

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hul.HULApplication
import com.hul.R
import com.hul.api.ApiExtentions
import com.hul.api.ApiHandler
import com.hul.api.controller.APIController
import com.hul.api.controller.UploadFileController
import com.hul.curriculam.ui.schoolCode.SchoolCodeAdapter
import com.hul.dashboard.ui.dashboard.MyPerfAdapter
import com.hul.dashboard.ui.dashboard.PerfInterface
import com.hul.data.Attendencemodel
import com.hul.data.MappedUser
import com.hul.data.PerformanceData
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.data.ResponseModel
import com.hul.data.SchoolCode
import com.hul.data.UploadImageData
import com.hul.data.UserDetails
import com.hul.data.VisitDetails
import com.hul.databinding.FragmentSBSupervisorDashboardBinding
import com.hul.sb.SBSupervisorDashboardComponent
import com.hul.screens.field_auditor_dashboard.ui.dashboard.AttendenceAdapter
import com.hul.screens.field_auditor_dashboard.ui.dashboard.DashboardFragmentInterface
import com.hul.screens.field_auditor_dashboard.ui.dashboard.MobilisersAdapter
import com.hul.sync.VisitDataTable
import com.hul.sync.VisitDataViewModel
import com.hul.user.UserInfo
import com.hul.utils.ASSIGNED
import com.hul.utils.ConnectionDetector
import com.hul.utils.INITIATED
import com.hul.utils.PARTIALLY_SUBMITTED
import com.hul.utils.RetryInterface
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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SBSupervisorDashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SBSupervisorDashboardFragment : Fragment(), ApiHandler, RetryInterface,
    DashboardFragmentInterface,
    com.hul.sb.supervisor.ui.dashboard.DashboardFragmentInterface {

    private var _binding: FragmentSBSupervisorDashboardBinding? = null

    private var syncDataList: ArrayList<VisitDataTable> = ArrayList()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var dashboardComponent: SBSupervisorDashboardComponent

    val perforManceList = arrayListOf("Till Date", "Today", "Yesterday", "This Week", "This Month")

    var perfSelectedposition = 0

    var adapter: SchoolCodeAdapter? = null

    var visitList: ArrayList<ProjectInfo> = ArrayList()

    @Inject
    lateinit var dashboardViewModel: SBSupervisorDashboardViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var uploadFileController: UploadFileController

    @Inject
    lateinit var apiController: APIController

    @Inject
    lateinit var visitDataViewModel: VisitDataViewModel

    val mobiliserUsers = mutableListOf<MappedUser>()

    lateinit var mobilisersAdapter: MobilisersAdapter

    lateinit var userDetails: UserDetails

    var schoolCodes: ArrayList<SchoolCode> = ArrayList()

    var selectedSchoolCode: SchoolCode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSBSupervisorDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        dashboardComponent =
            (activity?.application as HULApplication).appComponent.sbSupervisorDashboardComponent()
                .create()
        dashboardComponent.inject(this)
        binding.viewModel = dashboardViewModel

        binding.locationToVisit.layoutManager = LinearLayoutManager(context)

        //binding.myArea.text = userInfo.myArea

        binding.punchInButton.setOnClickListener {
            redirectToAttendence(ProjectInfo(location_id = "1"))
        }

        binding.dayToday.text = getCurrentDayOfWeek()
        binding.date.text = formatDate(Date(), "dd MMM yyyy")
        val pInfo: PackageInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0)
        binding.txtVersionName.text = "v"+pInfo.versionName
//        binding.tillDateButton.setOnClickListener {
//            showOptionsDialog()
//        }

        //binding.tillDateButton.setOnClickListener { showPerfDialog() }

        binding.rlProfile.setOnClickListener {
            showCustomDialog()
        }

        binding!!.syncNow.setOnClickListener {
            if (syncDataList.isNotEmpty()) {
                setProgressDialog(requireContext(), "Syncing Data")
                binding!!.syncNow.isEnabled = false
                binding!!.syncNow.isClickable = false
                startSync(syncDataList[syncDataList.size - 1])
            }
        }


        binding!!.schoolCode.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                selectedSchoolCode = null
                binding!!.llGetDirection.visibility = View.GONE
            } else {
                hideKeyboard(view)
            }
        }

        binding!!.schoolCode.setOnItemClickListener { parent, view, position, id ->
            selectedSchoolCode = schoolCodes[position]
            binding!!.llGetDirection.visibility =
                if (selectedSchoolCode!!.lattitude == null) View.GONE else View.VISIBLE
            binding!!.schoolCode.setText(selectedSchoolCode!!.external_id1)
            schoolCodes[position].id?.let { getSchoolVisits(it) }
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

                val check = schoolCodes.filter { it.external_id1.equals(s.toString()) }
                if (binding!!.schoolCode.text.isNotEmpty() && check.size == 0 && s.toString().length < 20) {
                    getSchoolCodes(binding!!.schoolCode.text.toString())
                }
            }
        })


        updateLocalList()

        return root
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
            noInternetDialogue(
                requireContext(),
                ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE.ordinal,
                this
            )
        }

    }

    private fun getSVisitsBySchoolCode(id: Int): RequestModel {
        return RequestModel(
            schoolId = id,
        )
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
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.SCHOOL_CODES.ordinal, this)
        }

    }

    private fun getSchoolCodesModel(s: String): RequestModel {
        return RequestModel(
            projectId = userInfo.projectId,
            externalId = s
        )
    }

    private fun hideKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private var imageIndex: Int = 0

    private var isSyncing: Boolean = false

    private var visitDataTableUploading: VisitDataTable? = null

    private var requestModel: RequestModel? = null

    private val handler = Handler(Looper.getMainLooper())

    fun updateLocalList() {
        if (binding != null) {
            visitDataViewModel.allSyncData.observe(requireActivity()) { visitDataList ->
                syncDataList = ArrayList(visitDataList)
                _binding?.let {
                    it.visitsLeft.text =
                        visitDataList.size.toString() + " " + requireActivity().getString(R.string.todays_visit_left)
                }
                if (syncDataList.size > 0) {

                } else {
                    _binding?.let { binding!!.todaysVisitParent.visibility = View.GONE }
                }

                val visitListFromBE = ArrayList<ProjectInfo>()
                for (data in syncDataList) {
                    val projectInfo = ProjectInfo(
                        visit_number = data.visitNumber.toString(),
                        location_name = data.locationName,
                        visit_status = "SUBMITTED",
                        location_id = data.locationId,
                        localString = data.jsonData,
                        visit_id = Gson().fromJson(
                            data.jsonData,
                            RequestModel::class.java
                        ).visit_id!!.toInt()
                    )
                    visitListFromBE.add(projectInfo)
                }

                val myVisitsAdapter =
                    MySupervisorVisitsAdapter(
                        visitListFromBE,
                        requireContext()
                    )

                // Setting the Adapter with the recyclerview
                _binding?.let { binding!!.todaysVisit.adapter = myVisitsAdapter }
            }
        }
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
                cancelProgressDialog()
                showViewTemporarily(binding!!.llVisitSuccessToast, 2000)
                updateLocalList()
            }
        }
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

    private fun startSync(visitDataTable: VisitDataTable) {
        imageIndex = 0
        isSyncing = true
        visitDataTableUploading = visitDataTable
        requestModel = Gson().fromJson(visitDataTable.jsonData, RequestModel::class.java)
        Log.d("TAG", "startSync: ${requestModel}")
        Log.d(
            "TAG",
            "startSync: ${requestModel!!.visitData!!.visit_image_1!!.value.toString().toUri()}"
        )
        uploadImage(
            requestModel!!.visitData!!.visit_image_1!!.value.toString().toUri(),
            requestModel!!.visit_number!!
        )
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

    private fun uploadImageModel(visitNumber: String): RequestModel {
        var fileName: String = ""
        val visitPrefix = "project_" + userInfo.projectName;

        when (imageIndex) {
            0 -> {
                fileName =
                    visitPrefix + "_selfie_image_outside_the_door_before_entering_the_house.jpeg";
            }

            1 -> {
                fileName =
                    visitPrefix + "_long_shot_of_mobiliser_showing_suvidha_photobook_in_the_household.jpeg";
            }
        }

        return RequestModel(
            project = userInfo.projectName,
            uploadFor = "field_audit",
            filename = fileName,
            visit_id = requestModel!!.visit_id
        )
    }

    fun formatDate(date: Date, format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun getCurrentDayOfWeek(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        mobiliserUsers.clear()
        getLogo()
    }

    private fun showCustomDialog() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
        val inflater = getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.profile_dialog, null)
        builder.setView(dialogView)
        val alertDialog: android.app.AlertDialog = builder.create()

        val llLogout = dialogView.findViewById<LinearLayout>(R.id.llLogOut);
        val txtMobiliserName = dialogView.findViewById<TextView>(R.id.txtMobiliserName)
        txtMobiliserName.text = userDetails.user_fullname

        llLogout.setOnClickListener {
            alertDialog.dismiss()
            userInfo.authToken = ""
            redirectToLogin(requireContext())
        }

        alertDialog.show()
    }

    private fun getLogo() {
        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            apiController.getApiResponse(
                this,
                RequestModel(projectId = userInfo.projectId),
                ApiExtentions.ApiDef.GET_LOGO.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.GET_LOGO.ordinal, this)
        }
    }

    private fun getMobilisers() {
        apiController.getApiResponse(
            this,
            getUserDetailsModel(),
            ApiExtentions.ApiDef.GET_USER_DETAILS.ordinal
        )
    }

    private fun getPerformance(filter: String) {
        apiController.getApiResponse(
            this,
            getPerformanceModel(filter),
            ApiExtentions.ApiDef.GET_PERFORMANCE.ordinal
        )
    }


    private fun getUserDetailsModel(): RequestModel {
        return RequestModel()
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

    private fun getPerformanceModel(filter: String): RequestModel {
        return RequestModel(
            date_filter = filter
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

    override fun onApiSuccess(o: String?, objectType: Int) {

        cancelProgressDialog()
        when (ApiExtentions.ApiDef.entries[objectType]) {

            ApiExtentions.ApiDef.VISIT_LIST_BY_SCHOOL_CODE -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())

                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type

                    visitList =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);

                    val pendingVisits: ArrayList<ProjectInfo> = ArrayList()

                    var flag = true

//                    for (project in visitList) {
//                        if (project.visit_status.equals(ASSIGNED, ignoreCase = true)
//                            || project.visit_status.equals(INITIATED, ignoreCase = true)
//                            || project.visit_status.equals(
//                                PARTIALLY_SUBMITTED,
//                                ignoreCase = true
//                            )
//                        ) {
//                            flag = false
//                            project.let { pendingVisits.add(it) }
//                        }
//                    }
                    /*
                    val sortedList = visitList.sortedBy { it.visit_number }

                    val listToShowInAdapter: ArrayList<ProjectInfo> = ArrayList();
                    if (sortedList.isNotEmpty()) {
                        listToShowInAdapter.add(sortedList.last())
                    }

                     Old Code
                    val sortedList = visitList.sortedBy { it.visit_number }

                    val listToShowInAdapter: ArrayList<ProjectInfo> = ArrayList()
                    listToShowInAdapter.addAll(sortedList)
                     */

//                    val sortedList = visitList.sortedBy { it.visit_number }
//
//                    //val filteredList = sortedList.filter { it.visit_number?.toIntOrNull() == 1 }
//
//                    val listToShowInAdapter: ArrayList<ProjectInfo> = ArrayList()
//                    for (item in sortedList) {
//                        if (item.visit_status.equals(ASSIGNED, ignoreCase = true)
//                            || item.visit_status.equals(INITIATED, ignoreCase = true)
//                            || item.visit_status.equals(PARTIALLY_SUBMITTED, ignoreCase = true)
//                        ) {
//                            listToShowInAdapter.add(item)
//                            break
//                        }
//                    }


                    val myVisitsAdapter =
                        MyVisitsSupervisorAdapter(visitList, this, requireContext())
                    // Setting the Adapter with the recyclerview
                    binding!!.locationToVisit.adapter = myVisitsAdapter
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            ApiExtentions.ApiDef.SCHOOL_CODES -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<SchoolCode?>?>() {}.type
                    schoolCodes =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    if (schoolCodes.size > 0) {
                        selectedSchoolCode = schoolCodes[0]
                        binding!!.llGetDirection.visibility =
                            if (selectedSchoolCode!!.lattitude == null) View.GONE else View.VISIBLE
                    } else {
                        selectedSchoolCode = SchoolCode(-1)
                        selectedSchoolCode?.external_id1 = "Add New"
                        schoolCodes.add(selectedSchoolCode!!)
                    }

                    adapter = SchoolCodeAdapter(
                        requireContext(),
                        R.layout.school_code_dropdown,
                        schoolCodes
                    )

                    // Set the adapter to the AutoCompleteTextView
                    binding!!.schoolCode.setAdapter(adapter)
                    binding!!.schoolCode.requestFocus()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }


            ApiExtentions.ApiDef.GET_USER_DETAILS -> {
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    userDetails = Gson().fromJson(
                        model.getJSONObject("data").toString(),
                        UserDetails::class.java
                    )

//                    binding.txtLatter.text = userDetails.user_fullname.split("")[1].uppercase()
//                    binding.visitNumbers.text = userDetails.users_mapped.size.toString() +" "+requireContext().getString(R.string.communicators)
//                    mobiliserUsers.addAll(userDetails.users_mapped)
//                    mobilisersAdapter =
//                        MobilisersAdapter(ArrayList(mobiliserUsers), this, requireContext())
//                    binding.recyclerViewMobilisers.adapter = mobilisersAdapter
//
                    binding.txtProfileName.setText("Hi, " + userDetails.user_fullname)

                    getPerformance("till_date")
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.GET_PERFORMANCE -> {
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val performanceData = Gson().fromJson(
                        model.getJSONObject("data").toString(),
                        PerformanceData::class.java
                    )
//                    when (perfSelectedposition) {
//                        0 -> {
//                            binding.txtVisits.text =
//                                performanceData.till_date.total_visits.toString()
//                            binding.txtAttendance.text =
//                                performanceData.till_date.attendance.toString() + "%"
//                            binding.txtTotalVisits.text =
//                                performanceData.till_date.audit_approval.toString() + "%"
//                        }
//
//                        1 -> {
//                            binding.txtVisits.text =
//                                performanceData.today.total_visits.toString()
//                            binding.txtAttendance.text =
//                                performanceData.today.attendance.toString() + "%"
//                            binding.txtTotalVisits.text =
//                                performanceData.today.audit_approval.toString() + "%"
//                        }
//
//                        2 -> {
//                            binding.txtVisits.text =
//                                performanceData.yesterday.total_visits.toString()
//                            binding.txtAttendance.text =
//                                performanceData.yesterday.attendance.toString() + "%"
//                            binding.txtTotalVisits.text =
//                                performanceData.yesterday.audit_approval.toString() + "%"
//                        }
//
//                        3 -> {
//                            binding.txtVisits.text =
//                                performanceData.this_week.total_visits.toString()
//                            binding.txtAttendance.text =
//                                performanceData.this_week.attendance.toString() + "%"
//                            binding.txtTotalVisits.text =
//                                performanceData.this_week.audit_approval.toString() + "%"
//                        }
//
//                        4 -> {
//                            binding.txtVisits.text =
//                                performanceData.this_month.total_visits.toString()
//                            binding.txtAttendance.text =
//                                performanceData.this_month.attendance.toString() + "%"
//                            binding.txtTotalVisits.text =
//                                performanceData.this_month.audit_approval.toString() + "%"
//                        }
//                    }

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
                    dashboardViewModel.attendenceToday.value = currentObject
                    items.removeAt(items.size - 1)
                    // Remove the first element
                    items.removeAt(0)
                    val adapter = AttendenceAdapter(requireContext(), items)
                    binding.gridView.adapter = adapter
                    if (currentObject.date!!.length > 10) {

                        binding.time.text = currentObject.date!!.substring(
                            11,
                            currentObject.date!!.length
                        )
                    }

                    if (currentObject.present!!) {
                        if (currentObject.present!!) {
                            binding.punchInButton.visibility = View.GONE
                            binding.punchInButtonDisabled.visibility = View.VISIBLE
                            binding.punchInButton.isEnabled = false
                        } else {
                            binding.punchInButton.visibility = View.VISIBLE
                        }
                    } else {
                        binding.punchInButton.visibility = View.VISIBLE
                    }

                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }

            }

            ApiExtentions.ApiDef.GET_LOGO -> {
                val model: ResponseModel = Gson().fromJson(o, ResponseModel::class.java)
                if (!model.error) {
                    val imageBytes =
                        Base64.decode(model.data!!.get("logo").toString(), Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    // Currently disabling dynamic logo, as static logo set already
                    // binding.imgLogo.setImageBitmap(decodedImage)

                    // Get Mobilisers
                    getMobilisers()
                    //getPerformance("till_date")
                } else {
                    redirectionAlertDialogue(requireContext(), model.message!!)
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
                    uploadImage(
                        requestModel!!.visitData!!.visit_image_2!!.value.toString().toUri(),
                        requestModel!!.visit_number!!
                    )
                } else if (uploadImageData != null && imageIndex == 1) {
                    imageIndex += 1;
                    requestModel!!.visitData!!.visit_image_2 = null
                    requestModel!!.visitData!!.visit_recording_1 = VisitDetails("")
                    requestModel!!.visitData!!.visit_recording_1!!.value = uploadImageData.url
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
            redirectionAlertDialogue(requireContext(), message!!)
        } else {
            redirectionAlertDialogue(requireContext(), message!!)
        }
    }

    override fun retry(type: Int) {

        when (ApiExtentions.ApiDef.entries[type]) {
            ApiExtentions.ApiDef.GET_USER_DETAILS -> getMobilisers()
            ApiExtentions.ApiDef.GET_ATTENDENCE -> getAttendance()
            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }

    }

    override fun redirectToAttendence(projectInfo: ProjectInfo) {

        if (dashboardViewModel.attendenceToday.value!!.present!!) {
            redirectToCurriculam(projectInfo)
        } else {
            val bundle = Bundle()
            bundle.putString("projectInfo", Gson().toJson(projectInfo))
            findNavController().navigate(
                R.id.action_SBSupervisorDashboardFragment_to_SBSupervisorAttendenceFragment,
                bundle
            )
        }
    }

    override fun addToLocal(projectInfo: ProjectInfo) {
        TODO("Not yet implemented")
    }

    override fun removeFromLocal(position: Int) {
        TODO("Not yet implemented")
    }

    private fun redirectToCurriculam(projectInfo: ProjectInfo) {
        if (projectInfo.visit_status.equals(ASSIGNED, ignoreCase = true)
            || projectInfo.visit_status.equals(INITIATED, ignoreCase = true)
            || projectInfo.visit_status.equals(
                PARTIALLY_SUBMITTED,
                ignoreCase = true
            )
        ) {
            val bundle = Bundle()
            bundle.putString("content2", Gson().toJson(projectInfo))
            findNavController().navigate(
                R.id.action_SBSupervisorDashboardFragment_to_supervisorFormFragment,
                bundle
            )
        } else {
            Toast.makeText(requireContext(), "Visit status is completed", Toast.LENGTH_LONG).show();
        }
    }

    private fun showOptionsDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView: View = inflater.inflate(R.layout.performance_dialog_options, null)

        val checkTillDate: RadioButton = dialogView.findViewById(R.id.check_till_date)
        val checkToday: RadioButton = dialogView.findViewById(R.id.check_today)
        val checkYesterday: RadioButton = dialogView.findViewById(R.id.check_yesterday)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
            .setTitle("Select Date Option")
            .setPositiveButton("OK") { dialog, _ ->
                if (checkTillDate.isChecked) {
                    checkToday.isChecked = false
                    checkYesterday.isChecked = false
                }
                if (checkToday.isChecked) {
                    checkYesterday.isChecked = false
                    checkTillDate.isChecked = false
                }
                if (checkYesterday.isChecked) {
                    checkToday.isChecked = false
                    checkTillDate.isChecked = false
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }

    override fun redirectToVisits(mobiliserData: MappedUser) {
//        if (!dashboardViewModel.attendenceToday.value!!.present!!) {
//            redirectToAttendence(ProjectInfo(location_id = "1"))
//        } else {
        val bundle = Bundle()
        bundle.putString("mobiliserData", Gson().toJson(mobiliserData))
        findNavController().navigate(
            R.id.action_SBSupervisorDashboardFragment_to_supervisorVisitsFragment,
            bundle
        )
        //}

    }
}