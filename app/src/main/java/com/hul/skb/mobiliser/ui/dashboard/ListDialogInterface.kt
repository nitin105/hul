package com.hul.skb.mobiliser.ui.dashboard

import com.hul.data.District
import com.hul.data.State

interface ListDialogInterface {

    fun onDistrictSelect(district : District)
    fun onStateSelect(state : State)
}