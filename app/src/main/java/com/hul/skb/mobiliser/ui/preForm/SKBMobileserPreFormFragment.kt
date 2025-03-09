package com.hul.skb.mobiliser.ui.preForm

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hul.HULApplication
import com.hul.R
import com.hul.api.ApiExtentions
import com.hul.api.ApiHandler
import com.hul.api.controller.APIController
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.databinding.FragmentSBPreFormBinding
import com.hul.databinding.FragmentSKBMobileserPreFormBinding
import com.hul.sb.SBDashboardComponent
import com.hul.sb.mobiliser.ui.sbform1details.SBForm1DetailsFragment
import com.hul.sb.mobiliser.ui.sbform1fill.SBForm1FillFragment
import com.hul.sb.mobiliser.ui.sbform2details.SBForm2DetailsFragment
import com.hul.sb.mobiliser.ui.sbform2fill.SBForm2FillFragment
import com.hul.sb.mobiliser.ui.sbform3details.SBForm3DetailsFragment
import com.hul.sb.mobiliser.ui.sbform3fill.SBForm3FillFragment
import com.hul.sb.mobiliser.ui.sbpreform.SBPreFormViewModel
import com.hul.skb.SKBDashboardComponent
import com.hul.skb.mobiliser.ui.dashboard.MyVisitsAdapter
import com.hul.user.UserInfo
import com.hul.utils.ASSIGNED
import com.hul.utils.ConnectionDetector
import com.hul.utils.INITIATED
import com.hul.utils.PARTIALLY_SUBMITTED
import com.hul.utils.RetryInterface
import com.hul.utils.cancelProgressDialog
import com.hul.utils.noInternetDialogue
import com.hul.utils.redirectionAlertDialogue
import com.hul.web_form.dynamic_forms.DynamicPagerAdapter
import org.json.JSONObject
import java.lang.reflect.Type
import javax.inject.Inject

class SKBMobileserPreFormFragment : Fragment(), ApiHandler, RetryInterface, VillageInterface {

    private var _binding: FragmentSKBMobileserPreFormBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var skbDashboardComponent: SKBDashboardComponent

    @Inject
    lateinit var skbPreFormViewModel: SKBMobileserPreFormViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var apiController: APIController

    var visitList: ArrayList<ProjectInfo> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSKBMobileserPreFormBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        skbDashboardComponent =
            (activity?.application as HULApplication).appComponent.skbDashboardComponent()
                .create()
        skbDashboardComponent.inject(this)
//        sbPreFormViewModel.selectedSchoolCode.value = Gson().fromJson(
//            requireArguments().getString("schoolInformation"),
//            SchoolCode::class.java
//        )
        if (requireArguments() != null) {
            skbPreFormViewModel.projectInfo.value = Gson().fromJson(
                requireArguments().getString("projectInfo"),
                ProjectInfo::class.java
            )

            binding.viewModel = skbPreFormViewModel

            binding.stats.setOnClickListener {
                requireActivity().onBackPressed()
            }

            //getVisits(skbPreFormViewModel.projectInfo.value!!.location_id!!)
        }
        return root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type

//        sbPreFormViewModel.visitList.value =
//            Gson().fromJson(requireArguments().getString("visitList"), listType);
//
//        val uDiceCode = requireArguments().getString("uDiceCode")

        binding.villageToVisit.layoutManager = LinearLayoutManager(context)


        var currentVisit: ProjectInfo? = null;
        var completedVisit: ProjectInfo? = null;

        var firstVisit: ProjectInfo? = null

        // Connect TabLayout with ViewPager2
        getVisits(skbPreFormViewModel.projectInfo.value!!.location_id!!)

    }

    private fun addNewTab(title: String, fragment: Fragment) {
//        dynamicPagerAdapter.addFragment(fragment, title)
//        dynamicPagerAdapter.notifyItemInserted(dynamicPagerAdapter.itemCount - 1)
    }


    private fun getVisits(id: String) {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            //setProgressDialog(requireContext(), "Loading Leads")
            apiController.getApiResponse(
                this,
                getVisitsModel(id),
                ApiExtentions.ApiDef.VISIT_LIST_BY_ID.ordinal
            )
        } else {
//            noInternetDialogue(
//                requireContext(),
//                ApiExtentions.ApiDef.VISIT_LIST_BY_ID.ordinal,
//                this
//            )
            val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
            visitList =
                Gson().fromJson(skbPreFormViewModel.projectInfo.value!!.visitList, listType);
            loadVisits()
        }

    }

    private fun loadVisits()
    {
        var active = arrayListOf<Int>()
        skbPreFormViewModel.projectInfoList.value = ArrayList<ProjectInfo>()
        for (data in visitList) {
            if (data.visit_status.equals("ASSIGNED")) {
                skbPreFormViewModel.projectInfoList.value!!.add(data)
                when (data.visit_type!!) {
                    "Village Launch" -> {
                        active.add(0)

                    }

                    "IPC1" -> {
                        active.add(1)

                    }

                    "Anganwadi Session" -> {
                        active.add(2)

                    }

                    "SCP" -> {
                        active.add(3)

                    }

                    "RMP" -> {
                        active.add(4)

                    }

                    "IPC2" -> {
                        active.add(5)

                    }
                }
            }
        }

        var visitListFromBE = arrayListOf(
            "Village Launch",
            "IPC 1",
            "Anganwadi  Session",
            "SCP",
            "RMP",
            "IPC 2"
        )

        val myVillageAdapter =
            MyVillageAdapter(
                visitListFromBE,
                this,
                requireContext(),
                active
            )

        // Setting the Adapter with the recyclerview
        binding!!.villageToVisit.adapter = myVillageAdapter
    }

    private fun getVisitsModel(id: String): RequestModel {
        return RequestModel(
            visit_id = id,
        )
    }

    override fun onApiSuccess(o: String?, objectType: Int) {


        when (ApiExtentions.ApiDef.entries[objectType]) {

            ApiExtentions.ApiDef.VISIT_LIST_BY_ID -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    val listType: Type = object : TypeToken<List<ProjectInfo?>?>() {}.type
                    visitList =
                        Gson().fromJson(model.getJSONArray("data").toString(), listType);
                    loadVisits()

//                    if(sbPreFormViewModel.projectInfo.value!!.visit_status.equals("SUBMITTED") && sbPreFormViewModel.projectInfo.value!!.visit_number!!.toInt() == 1)
//                    {
//                        visitList.get(0).visit_status = "SUBMITTED"
//                    }else if(sbPreFormViewModel.projectInfo.value!!.visit_status.equals("SUBMITTED") && sbPreFormViewModel.projectInfo.value!!.visit_number!!.toInt() == 2)
//                    {
//                        visitList.get(1).visit_status = "SUBMITTED"
//                    }else if(sbPreFormViewModel.projectInfo.value!!.visit_status.equals("SUBMITTED") && sbPreFormViewModel.projectInfo.value!!.visit_number!!.toInt() == 3)
//                    {
//                        visitList.get(2).visit_status = "SUBMITTED"
//                    }
//                    setTabs()
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

    override fun redirect(position: Int) {
        val bundle = Bundle()
        //bundle.putString("projectInfo", Gson().toJson(skbPreFormViewModel.projectInfoList.value!!.get(position)))

        when (position) {
            0 -> {
                skbPreFormViewModel.projectInfoList.value!!.forEachIndexed { index, projectInfo ->
                    if (projectInfo.visit_type.equals("Village Launch"))
                    {
                        bundle.putString("projectInfo", Gson().toJson(projectInfo))
                    }

                }
                findNavController().navigate(
                    R.id.action_SKBMobileserPreFormFragment_to_villageLaunchFragment,
                    bundle
                )
            }

            1 -> {
                skbPreFormViewModel.projectInfoList.value!!.forEachIndexed { index, projectInfo ->
                    if (projectInfo.visit_type.equals("IPC1"))
                    {
                        bundle.putString("projectInfo", Gson().toJson(projectInfo))
                    }

                }
                findNavController().navigate(
                    R.id.action_SKBMobileserPreFormFragment_to_IPC1Fragment,
                    bundle
                )
            }

            2 -> {
                skbPreFormViewModel.projectInfoList.value!!.forEachIndexed { index, projectInfo ->
                    if (projectInfo.visit_type.equals("Anganwadi Session"))
                    {
                        bundle.putString("projectInfo", Gson().toJson(projectInfo))
                    }

                }
                findNavController().navigate(
                    R.id.action_SKBMobileserPreFormFragment_to_AWCSessionFragment,
                    bundle
                )
            }

            3 -> {
                skbPreFormViewModel.projectInfoList.value!!.forEachIndexed { index, projectInfo ->
                    if (projectInfo.visit_type.equals("SCP"))
                    {
                        bundle.putString("projectInfo", Gson().toJson(projectInfo))
                    }

                }
                findNavController().navigate(
                    R.id.action_SKBMobileserPreFormFragment_to_SCPFragment,
                    bundle
                )
            }

            4 -> {
                skbPreFormViewModel.projectInfoList.value!!.forEachIndexed { index, projectInfo ->
                    if (projectInfo.visit_type.equals("RMP"))
                    {
                        bundle.putString("projectInfo", Gson().toJson(projectInfo))
                    }

                }
                findNavController().navigate(
                    R.id.action_SKBMobileserPreFormFragment_to_RMPFragment,
                    bundle
                )
            }

            5 -> {
                skbPreFormViewModel.projectInfoList.value!!.forEachIndexed { index, projectInfo ->
                    if (projectInfo.visit_type.equals("IPC2"))
                    {
                        bundle.putString("projectInfo", Gson().toJson(projectInfo))
                    }

                }
                findNavController().navigate(
                    R.id.action_SKBMobileserPreFormFragment_to_IPC2Fragment,
                    bundle
                )
            }
        }

    }

}