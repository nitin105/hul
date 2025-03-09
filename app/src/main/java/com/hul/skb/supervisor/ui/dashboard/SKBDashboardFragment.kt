package com.hul.skb.supervisor.ui.dashboard

import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.BitmapFactory
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.hul.curriculam.Curriculam
import com.hul.dashboard.ui.dashboard.MyPerfAdapter
import com.hul.dashboard.ui.dashboard.PerfInterface
import com.hul.data.Attendencemodel
import com.hul.data.MappedUser
import com.hul.data.PerformanceData
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.data.ResponseModel
import com.hul.data.UserDetails
import com.hul.databinding.FragmentDashboardAuditorBinding
import com.hul.databinding.FragmentSKBDashboardBinding
import com.hul.screens.field_auditor_dashboard.FieldAuditorDashboardComponent
import com.hul.screens.field_auditor_dashboard.ui.dashboard.AttendenceAdapter
import com.hul.screens.field_auditor_dashboard.ui.dashboard.DashboardViewModel
import com.hul.skb.SKBSupervisorDashboardComponent
import com.hul.user.UserInfo
import com.hul.utils.ConnectionDetector
import com.hul.utils.RetryInterface
import com.hul.utils.cancelProgressDialog
import com.hul.utils.noInternetDialogue
import com.hul.utils.redirectToLogin
import com.hul.utils.redirectionAlertDialogue
import org.json.JSONObject
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class SKBDashboardFragment : Fragment(), ApiHandler, RetryInterface, DashboardFragmentInterface {

    private var _binding: FragmentSKBDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var dashboardComponent: SKBSupervisorDashboardComponent

    val perforManceList = arrayListOf("Till Date", "Today", "Yesterday", "This Week", "This Month")

    var perfSelectedposition = 0

    @Inject
    lateinit var dashboardViewModel: SKBDashboardViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var apiController: APIController

    val mobiliserUsers = mutableListOf<MappedUser>()

    lateinit var mobilisersAdapter: MobilisersAdapter

    lateinit var userDetails: UserDetails

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSKBDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        dashboardComponent =
            (activity?.application as HULApplication).appComponent.skbSupervisorDashboardComponent()
                .create()
        dashboardComponent.inject(this)
        binding.viewModel = dashboardViewModel

        binding.recyclerViewMobilisers.layoutManager = LinearLayoutManager(context)

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

        binding.tillDateButton.setOnClickListener { showPerfDialog() }

        binding.rlProfile.setOnClickListener {
            showCustomDialog()
        }

        return root
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
            MyPerfAdapter(perforManceList, perfSelectedposition, object : PerfInterface {
                override fun onSelected(position: Int) {
                    perfSelectedposition = position
                    binding!!.tillDateButton.text = perforManceList[position]
                    when (position) {
                        0 -> {
                            getPerformance("till_date")
                        }

                        1 -> {
                            getPerformance("today")
                        }

                        2 -> {
                            getPerformance("yesterday")
                        }

                        3 -> {
                            getPerformance("this_week")
                        }

                        4 -> {
                            getPerformance("this_month")
                        }
                    }
                    alertDialog.cancel()
                }

            }, requireContext())
        // Setting the Adapter with the recyclerview
        locationToVisit.adapter = myVisitsAdapter


        alertDialog.show()
    }

    override fun onApiSuccess(o: String?, objectType: Int) {

        cancelProgressDialog()
        when (ApiExtentions.ApiDef.entries[objectType]) {

            ApiExtentions.ApiDef.GET_USER_DETAILS -> {
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    userDetails = Gson().fromJson(
                        model.getJSONObject("data").toString(),
                        UserDetails::class.java
                    )

                    binding.txtLatter.text = userDetails.user_fullname.split("")[1].uppercase()

                    mobiliserUsers.addAll(userDetails.users_mapped)
                    mobilisersAdapter =
                        MobilisersAdapter(ArrayList(mobiliserUsers), this, requireContext())
                    binding.recyclerViewMobilisers.adapter = mobilisersAdapter

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
                    when (perfSelectedposition) {
                        0 -> {
                            binding.txtVisits.text =
                                performanceData.till_date.total_visits.toString()
                            binding.txtAttendance.text =
                                performanceData.till_date.attendance.toString() + "%"
                            binding.txtTotalVisits.text =
                                performanceData.till_date.audit_approval.toString() + "%"
                        }

                        1 -> {
                            binding.txtVisits.text =
                                performanceData.today.total_visits.toString()
                            binding.txtAttendance.text =
                                performanceData.today.attendance.toString() + "%"
                            binding.txtTotalVisits.text =
                                performanceData.today.audit_approval.toString() + "%"
                        }

                        2 -> {
                            binding.txtVisits.text =
                                performanceData.yesterday.total_visits.toString()
                            binding.txtAttendance.text =
                                performanceData.yesterday.attendance.toString() + "%"
                            binding.txtTotalVisits.text =
                                performanceData.yesterday.audit_approval.toString() + "%"
                        }

                        3 -> {
                            binding.txtVisits.text =
                                performanceData.this_week.total_visits.toString()
                            binding.txtAttendance.text =
                                performanceData.this_week.attendance.toString() + "%"
                            binding.txtTotalVisits.text =
                                performanceData.this_week.audit_approval.toString() + "%"
                        }

                        4 -> {
                            binding.txtVisits.text =
                                performanceData.this_month.total_visits.toString()
                            binding.txtAttendance.text =
                                performanceData.this_month.attendance.toString() + "%"
                            binding.txtTotalVisits.text =
                                performanceData.this_month.audit_approval.toString() + "%"
                        }
                    }

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
                } else {
                    redirectionAlertDialogue(requireContext(), model.message!!)
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
                R.id.action_SKBDashboardFragment_to_attendencFragment,
                bundle
            )
        }
    }

    private fun redirectToCurriculam(projectInfo: ProjectInfo) {
        val intent = Intent(activity, Curriculam::class.java)
        intent.putExtra("projectInfo", Gson().toJson(projectInfo))
        startActivity(intent)
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
        if (!dashboardViewModel.attendenceToday.value!!.present!!) {
            redirectToAttendence(ProjectInfo(location_id = "1"))
        } else {
            val bundle = Bundle()
            bundle.putString("mobiliserData", Gson().toJson(mobiliserData))
            findNavController().navigate(
                R.id.action_SKBDashboardFragment_to_supervisorVisitsFragment2,
                bundle
            )
        }

    }
}