package com.hul.skb

import com.hul.di.ActivityScope
import com.hul.skb.supervisor.ui.attendence.AttendencFragment
import com.hul.skb.supervisor.ui.dashboard.SKBDashboardFragment
import dagger.Subcomponent

/**
 * Created by Nitin Chorge on 02-09-2024.
 */
@ActivityScope
@Subcomponent
interface SKBSupervisorDashboardComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): SKBSupervisorDashboardComponent
    }

    fun inject(activity: SKBSupervisorDashboardComponent)

    fun inject(fragment: AttendencFragment)

    fun inject(fragment: SKBDashboardFragment)

}