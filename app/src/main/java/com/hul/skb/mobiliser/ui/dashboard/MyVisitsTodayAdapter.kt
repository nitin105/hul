package com.hul.skb.mobiliser.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hul.R
import com.hul.data.ProjectInfo
import com.hul.databinding.LocationViewDesignBinding
import com.hul.utils.ASSIGNED
import com.hul.utils.INITIATED
import com.hul.utils.PARTIALLY_SUBMITTED

/**
 * Created by Nitin Chorge on 03-04-2024.
 */
class MyVisitsTodayAdapter(
    private val mList: ArrayList<ProjectInfo>,
    private val dashboardInterface: DashboardFragmentInterface,
    val mContext: Context
) : RecyclerView.Adapter<MyVisitsTodayAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: LocationViewDesignBinding) :
        RecyclerView.ViewHolder(binding.root)

    // inside the onCreateViewHolder inflate the view of SingleItemBinding
    // and return new ViewHolder object containing this layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            LocationViewDesignBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
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
                binding.locationPosition.text =
                    mList[position].visit_identifier1
                binding.visit.text = "Completed"
                binding.visit.setTextColor(
                    Color.parseColor("#53D28C")
                )
                binding.root.setOnClickListener {
                    /*if (mList[position].visit_status.equals(ASSIGNED, ignoreCase = true)
                        || mList[position].visit_status.equals(INITIATED, ignoreCase = true)
                        || mList[position].visit_status.equals(PARTIALLY_SUBMITTED, ignoreCase = true)
                    ) {
                        dashboardInterface.redirectToAttendence(this)
                    }*/
                    //dashboardInterface.redirectToAttendence(this)
                }
            }
        }
    }

    // return the size of languageList
    override fun getItemCount(): Int {
        return mList.size
    }
}