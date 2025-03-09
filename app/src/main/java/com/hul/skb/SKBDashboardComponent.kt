package com.hul.skb

import com.hul.di.ActivityScope
import com.hul.skb.mobiliser.SKBMobiliserDashboard
import com.hul.skb.mobiliser.ui.rmp.RMPFragment
import com.hul.skb.mobiliser.ui.scp.SCPFragment
import com.hul.skb.mobiliser.ui.attendence.SKBMobiliserAttendenceFragment
import com.hul.skb.mobiliser.ui.awc.AWCSessionFragment
import com.hul.skb.mobiliser.ui.dashboard.SKBDashboardMobiliserFragment
import com.hul.skb.mobiliser.ui.ipc1.IPC1Fragment
import com.hul.skb.mobiliser.ui.ipc2.IPC2Fragment
import com.hul.skb.mobiliser.ui.preForm.SKBMobileserPreFormFragment
import com.hul.skb.mobiliser.ui.punchout.PunchOutFragment
import com.hul.skb.mobiliser.ui.villagelaunch.VillageLaunchFragment
import dagger.Subcomponent

/**
 * Created by Nitin Chorge on 06-01-2021.
 */

@ActivityScope
@Subcomponent
interface SKBDashboardComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): SKBDashboardComponent
    }

    fun inject(activity: SKBMobiliserDashboard)

    fun inject(fragment: SKBDashboardMobiliserFragment)

    fun inject(fragment: SKBMobiliserAttendenceFragment)

    fun inject(fragment: PunchOutFragment)

    fun inject(fragment: SKBMobileserPreFormFragment)

    fun inject(fragment: VillageLaunchFragment)

    fun inject(fragment: IPC1Fragment)

    fun inject(fragment: AWCSessionFragment)

    fun inject(fragment: IPC2Fragment)

    fun inject(fragment: SCPFragment)

    fun inject(fragment: RMPFragment)

}