package com.hul.api

import com.hul.data.*
import com.hul.data.RequestModel
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by Nitin Chorge on 23-11-2020.
 */
interface ApiInterface {

    @POST("app/projects/v1/societies/")
    fun getSocietyList(@Body param: RequestModel?): Call<ResponseBody?>

    @GET("app/projects/v1/locations/")
    fun getCodeList(
        @Query("areaId") areaId: Int?,
        @Query("sendAll") sendAll: Boolean?
    ): Call<ResponseBody?>

    @GET("app/projects/v1/locations/")
    fun getLocationList(@Query("projectId") projectId: String?): Call<ResponseBody?>

    @GET("app/projects/v1/locations/")
    fun getSchoolCodes(
        @Query("projectId") projectId: String?,
        @Query("externalId") externalId: String?
    ): Call<ResponseBody?>

    @GET("app/projects/v1/locations/")
    fun getVillageList(
        @Query("projectId") projectId: Int?,
        @Query("sendAll") sendAll: Boolean?
    ): Call<ResponseBody?>

    @GET("app/users/v1/get_user_attendance/")
    fun getAttendence(): Call<ResponseBody?>

    @GET("app/users/v1/get_attendance_form_fields/")
    fun getAttendenceForm(@Query("projectId") projectId: String?): Call<ResponseBody?>

    @GET("app/visits/v1/get_list_of_visits/")
    fun getVisitList(): Call<ResponseBody?>

    @GET("app/visits/v1/get_list_of_visits/")
    fun getVisitListByStatus(
        @Query("status ") status: String?,
    ): Call<ResponseBody?>

    @GET("app/visits/v1/get_list_of_visits/")
    fun getVisitListByStatus2(
        @Query("status") status: String?,
        @Query("userType") userType: String?,
    ): Call<ResponseBody?>

    @GET("app/visits/v1/get_list_of_visits/")
    fun getVisitListSingle(
        @Query("locationId ") locationId: String?,
    ): Call<ResponseBody?>

    @GET("app/visits/v1/get_list_of_visits/")
    fun getVisitListSingle(
        @Query("userType") userType: String?,
        @Query("mobiliserId") mobiliserId: Int
    ): Call<ResponseBody?>

    @GET("app/visits/v1/get_list_of_visits/")
    fun getVisitListPrevious(
        @Query("userType") userType: String?,
        @Query("mobiliserId") mobiliserId: Int,
        @Query("status") status: String
    ): Call<ResponseBody?>

    @GET("app/visits/v1/get_visit_form_fields/")
    fun getVisitFormFields(
        @Query("projectId") projectId: String?,
        @Query("visitNumber") visitNumber: String
    ): Call<ResponseBody?>

    @GET("app/visits/v1/get_list_of_visits/{id}")
    fun getListOfVisits(
        @Path("id") id: Int
    ): Call<ResponseBody?>

    @GET("app/leads/v1/getLeads/{lead_id}")
    fun getLeadDetails(@Path(value = "lead_id", encoded = true) leadId: String): Call<ResponseBody?>

    @GET("s3upload/v1/getUploadedImagesList/{lead_id}")
    fun getUploadedDocument(
        @Path(
            value = "lead_id",
            encoded = true
        ) leadId: String
    ): Call<ResponseBody?>

    @PUT("app/leads/v1/submitInspection/{lead_id}")
    fun submitLead(
        @Path(value = "lead_id", encoded = true) leadId: String,
        @Body param: RequestModel
    ): Call<ResponseBody?>

    @PUT("app/leads/v1/verifyLeadDetails/{lead_id}")
    fun confirmLead(
        @Path(value = "lead_id", encoded = true) leadId: String,
        @Query("isVerified") isVerified: Boolean,
        @Query("remarks") remarks: String,
        @Body param: RequestModel
    ): Call<ResponseBody?>

    @GET("app/users/v1/get_user_details")
    fun getUserDetails(): Call<ResponseBody?>

    @GET("app/users/v1/performance")
    fun getPerformance(@Query("date_filter") date_filter: String?,): Call<ResponseBody?>

    @GET("app/visits/v1/visitData")
    fun getVisitData(
        @Query("visitId") visitId: Int,
        @Query("project") project: String?,
        @Query("collectedBy") collectedBy: String,
        @Query("loadImages") loadImages: Boolean,
    ): Call<ResponseBody?>

    @POST("app/users/v1/verify_otp")
    fun loginUser(@Body param: RequestModel?): Call<ResponseBody?>

    @POST("app/users/v1/add_device")
    fun deviceInfo(@Body param: RequestModel?): Call<ResponseBody?>

    @POST("/app/visits/v1/addVisit/")
    fun addVisit(@Body param: RequestModel?): Call<ResponseBody?>

    @GET("app/projects/v1/logo/{projectId}")
    fun getLogo(@Path(value = "projectId", encoded = true) projectId: String): Call<ResponseBody?>

    @GET("app/projects/v1/bannerImage/{projectId}")
    fun getBannerImage(
        @Path(
            value = "projectId",
            encoded = true
        ) projectId: String,
    ): Call<ResponseBody?>

    @POST("app/users/v1/mark_attendance/")
    fun markAttendence(
        @Body param: RequestModel,
        @Query("project") project: String,
        @Query("location_id") location_id: String
    ): Call<ResponseBody?>

    @POST("app/users/v1/mark_attendance/")
    fun punchOut(
        @Body param: RequestModel,
        @Query("project") project: String,
        @Query("location_id") location_id: String
    ): Call<ResponseBody?>

    @POST("app/users/v1/send_otp")
    fun sendOTP(@Body param: RequestModel?): Call<ResponseBody?>

    @Multipart
    @POST("app/users/v1/uploadImage/")
    fun upload(
        @Part file: MultipartBody.Part?,
        @Query("projectName") projectName: String?,
        @Query("uploadFor") uploadFor: String?,
        @Query("filename") filename: String?,
        @Query("visitId") visitId: String?,
        @Query("clickDatetime") clickDatetime: String?,
    ): Call<ResponseBody?>

    @POST("app/visits/v1/addVisitData/")
    fun visitData(@Body requestModel: RequestModel): Call<ResponseBody?>

    @POST("app/visits/v1/addVisitData/")
    fun submitForm(@Body requestModel: RequestBody): Call<ResponseBody?>

    @GET("app/projects/v1/areas")
    fun getDistricts(@Query("projectId") projectId: String): Call<ResponseBody?>

    @GET("app/projects/v1/states")
    fun getStates(@Query("projectId") projectId: String): Call<ResponseBody?>

    @POST("app/projects/v1/addLocation/")
    fun addSchool(@Body requestModel: RequestModel): Call<ResponseBody?>

    @GET("app/projects/v1/get_list_of_wards")
    fun getWards(@Query("projectId") projectId: String): Call<ResponseBody?>

    @GET("app/projects/v1/get_list_of_zones")
    fun getZones(@Query("projectId") projectId: String): Call<ResponseBody?>

    companion object {
        //const val BASE_URL = "http://3.7.149.234:8000/"//dev
        const val BASE_URL = "https://api.goodmetrics.in/"//prod
    }
}
