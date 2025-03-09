package com.hul.skb.supervisor.ui.visits

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hul.data.ProjectInfo
import com.hul.databinding.ItemSkbSupervisorVisitBinding
import com.hul.databinding.ItemSupervisorVisitBinding
import com.hul.utils.ASSIGNED
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Created by Nitin Chorge on 03-04-2024.
 */
class SupervisorVisitsAdapter(
    private var mList: List<ProjectInfo>,
    private val callback: SupervisorVisitInterface,
    val mContext: Context
) : RecyclerView.Adapter<SupervisorVisitsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSkbSupervisorVisitBinding) :
        RecyclerView.ViewHolder(binding.root)

    // inside the onCreateViewHolder inflate the view of SingleItemBinding
    // and return new ViewHolder object containing this layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemSkbSupervisorVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    fun updateVisits(newVisits: List<ProjectInfo>) {
        this.mList = newVisits as ArrayList<ProjectInfo>
        notifyDataSetChanged()
    }

    // bind the items with each item
    // of the list languageList
    // which than will be
    // shown in recycler view
    // to keep it simple we are
    // not setting any image data to view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(mList[position]) {
                val fromUser: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                val myFormat: SimpleDateFormat = SimpleDateFormat("dd MMM, yyyy")

                try {
                    val reformattedStr: String = myFormat.format(fromUser.parse(mList[position].created_date))
                    binding.txtSchoolName.text =
                        mList[position].visit_identifier1 + " | " + reformattedStr
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

                binding.txtVisitStatus.text = mList[position].location_name
//                    if (mList[position].visit_status.equals(SUBMITTED, ignoreCase = true)
//                        || mList[position].visit_status.equals(
//                            SUB_AGENCY_APPROVED,
//                            ignoreCase = true
//                        )
//                    ) "Pending" else "Completed"
//                binding.txtVisitStatus.setTextColor(
//                    Color.parseColor(
//                        if (mList[position].visit_status.equals(
//                                SUBMITTED, ignoreCase = true
//                            ) || mList[position].visit_status.equals(
//                                SUB_AGENCY_APPROVED, ignoreCase = true
//                            )
//                        ) "#FF9F43" else "#53D28C"
//                    )
//                )
                if(mList[position].visit_status.equals(ASSIGNED, ignoreCase = true))
                {
                    binding.capture.visibility = View.VISIBLE
                    binding.chevron.visibility = View.GONE
                }
                else{
                    binding.capture.visibility = View.GONE
                    binding.chevron.visibility = View.VISIBLE
                }
                binding.capture.setOnClickListener {
                    callback.redirectToSchoolActivity(this, binding.txtSchoolName.text.toString())
                }
//                binding.llDirection.setOnClickListener {
//                    callback.goToMap(this)
//                }
            }
        }
    }

    // return the size of languageList
    override fun getItemCount(): Int {
        return mList.size
    }
}