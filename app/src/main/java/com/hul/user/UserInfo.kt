package com.hul.user

/**
 * Created by Nitin Chorge on 23-11-2020.
 */
import com.hul.storage.Storage
import com.hul.utils.ATTENDENCE_DATE
import com.hul.utils.ATTENDENCE_MARKED
import com.hul.utils.AUTH_TOKEN
import com.hul.utils.CODES_LIST
import com.hul.utils.IS_NEW_VISIT_SUBMITTED
import com.hul.utils.LOCAL_ATTENDENCE
import com.hul.utils.LOCAL_PROJECT_LIST
import com.hul.utils.LOCAL_PUNCH_OUT
import com.hul.utils.LOGIN_ID
import com.hul.utils.MY_AREA
import com.hul.utils.MY_AREA_ID
import com.hul.utils.PENDING_LEAD_DETAIL
import com.hul.utils.PREFERENCE_DATE
import com.hul.utils.PROJECT_ID
import com.hul.utils.PROJECT_Name
import com.hul.utils.PUNCHED_OUT
import com.hul.utils.USER_FULLNAME
import com.hul.utils.USER_TYPE
import com.hul.utils.VILLAGE_LOCAL_DATA
import com.hul.utils.WARD_LIST
import com.hul.utils.ZONE_LIST
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Handles User lifecycle. Manages registrations, logs in and logs out.
 * Knows when the user is logged in.
 */

@Singleton
class UserInfo @Inject constructor(private val storage: Storage) {

    /**
     *  UserDataRepository is specific to a logged in user. This determines if the user
     *  is logged in or not, when the user logs in, a new instance will be created.
     *  When the user logs out, this will be null.
     */
    //var userDataRepository: UserDataRepository? = null


    var loginId: String
        get() = storage.getString(LOGIN_ID)
        set(value) = storage.setString(LOGIN_ID, value)

    var authToken: String
        get() = storage.getString(AUTH_TOKEN)
        set(value) = storage.setString(AUTH_TOKEN, value)

    var pendingLeadString: String
        get() = storage.getString(PENDING_LEAD_DETAIL)
        set(value) = storage.setString(PENDING_LEAD_DETAIL, value)


    var projectId: String
        get() = storage.getString(PROJECT_ID)
        set(value) = storage.setString(PROJECT_ID, value)

    var projectName: String
        get() = storage.getString(PROJECT_Name)
        set(value) = storage.setString(PROJECT_Name, value)

    var userFullname: String
        get() = storage.getString(USER_FULLNAME)
        set(value) = storage.setString(USER_FULLNAME, value)

    var myArea: String
        get() = storage.getString(MY_AREA)
        set(value) = storage.setString(MY_AREA, value)

    var areaId: String
        get() = storage.getString(MY_AREA_ID)
        set(value) = storage.setString(MY_AREA_ID, value)

    var userType: String
        get() = storage.getString(USER_TYPE)
        set(value) = storage.setString(USER_TYPE, value)

    var didUserSubmitNewVisit: Boolean
        get() = storage.getBoolean(IS_NEW_VISIT_SUBMITTED)
        set(value) = storage.setBoolean(IS_NEW_VISIT_SUBMITTED, value)

    var codeList: String
        get() = storage.getString(CODES_LIST)
        set(value) = storage.setString(CODES_LIST, value)

    var localProjectList: String
        get() = storage.getString(LOCAL_PROJECT_LIST)
        set(value) = storage.setString(LOCAL_PROJECT_LIST, value)

    var preferenceDate: String
        get() = storage.getString(PREFERENCE_DATE)
        set(value) = storage.setString(PREFERENCE_DATE, value)

    var villageLocalData: String
        get() = storage.getString(VILLAGE_LOCAL_DATA)
        set(value) = storage.setString(VILLAGE_LOCAL_DATA, value)

    var didUsermarkedAttendence: Boolean
        get() = storage.getBoolean(ATTENDENCE_MARKED)
        set(value) = storage.setBoolean(ATTENDENCE_MARKED, value)

    var didUserPunchedOut: Boolean
        get() = storage.getBoolean(PUNCHED_OUT)
        set(value) = storage.setBoolean(PUNCHED_OUT, value)

    var attendenceDate: String
        get() = storage.getString(ATTENDENCE_DATE)
        set(value) = storage.setString(ATTENDENCE_DATE, value)

    var localAttendence: String
        get() = storage.getString(LOCAL_ATTENDENCE)
        set(value) = storage.setString(LOCAL_ATTENDENCE, value)

    var localPunchOut: String
        get() = storage.getString(LOCAL_PUNCH_OUT)
        set(value) = storage.setString(LOCAL_PUNCH_OUT, value)

    var wardList: String
        get() = storage.getString(WARD_LIST)
        set(value) = storage.setString(WARD_LIST, value)

    var zoneList: String
        get() = storage.getString(ZONE_LIST)
        set(value) = storage.setString(ZONE_LIST, value)

}
