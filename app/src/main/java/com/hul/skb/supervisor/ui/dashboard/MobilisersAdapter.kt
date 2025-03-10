package com.hul.skb.supervisor.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hul.R
import com.hul.data.MappedUser
import com.hul.data.ProjectInfo
import com.hul.databinding.ItemMobiliserBinding
import com.hul.databinding.LocationViewDesignBinding

/**
 * Created by Nitin Chorge on 03-04-2024.
 */
class MobilisersAdapter(private val mList: ArrayList<MappedUser>, private val dashboardInterface: DashboardFragmentInterface, val mContext : Context) : RecyclerView.Adapter<MobilisersAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMobiliserBinding) : RecyclerView.ViewHolder(binding.root)

    // inside the onCreateViewHolder inflate the view of SingleItemBinding
    // and return new ViewHolder object containing this layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMobiliserBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    // bind the items with each item
    // of the list languageList
    // which than will be
    // shown in recycler view
    // to keep it simple we are
    // not setting any image data to view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mList[position]){
                val item = mList[position]
               binding.txtMobiliser.text = item.user_fullname
                binding.txtVisit.text = item.user_type
                binding.root.setOnClickListener{
                    dashboardInterface.redirectToVisits(this)
                }
            }
        }
    }

    // return the size of languageList
    override fun getItemCount(): Int {
        return mList.size
    }
}