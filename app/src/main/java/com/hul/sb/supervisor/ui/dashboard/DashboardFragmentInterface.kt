package com.hul.sb.supervisor.ui.dashboard

import com.hul.data.ProjectInfo

interface DashboardFragmentInterface {

    fun redirectToAttendence(projectInfo : ProjectInfo)

    fun addToLocal(projectInfo : ProjectInfo)
    fun removeFromLocal(position : Int)
}