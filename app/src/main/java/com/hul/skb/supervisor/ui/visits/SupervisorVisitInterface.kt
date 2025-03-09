package com.hul.skb.supervisor.ui.visits

import com.hul.data.ProjectInfo

/**
 * Created by Nitin Chorge on 04-09-2024.
 */
interface SupervisorVisitInterface {
    fun redirectToSchoolActivity(projectInfo: ProjectInfo, heading:String)

    fun goToMap(projectInfo: ProjectInfo)
}