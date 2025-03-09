package com.hul.data

import org.json.JSONObject

/**
 * Created by Nitin Chorge on 26-11-2020.
 */
data class RequestModel(
    var date_filter: String? = null,
    var projectId: String? = null,
    var mobile: String? = null,
    var type: String? = null,
    var status: String? = null,
    var otp: String? = null,
    var app_version: Int? = null,
    var password: String? = null,
    var leadId: String? = null,
    var regNo: String? = null,
    var docType: String? = null,
    var docTypeDescription: String? = null,
    var photo_url1: String? = null,
    var photo_url1_description: String? = null,
    var attendance_type: String? = null,
    var photo_url2: String? = null,
    var photo_url2_description: String? = null,
    var photo_url3: String? = null,
    var photo_url3_description: String? = null,
    var remarks: String? = null,
    var project: String? = null,
    var location_id: String? = null,
    var village_name: String? = null,
    var visit_number: String? = null,
    var visit_identifier1: String? = null,
    var visit_identifier2: String? = null,
    var visit_status: String? = null,
    var lattitude: String? = null,
    var longitude: String? = null,
    var externalId: String? = null,
    var visit_id: String? = null,
    var identifier: String? = null,
    var visitData: VisitData? = null,
    val userType: String? = null,
    val mobiliserId: Int? = null,
    val visitId: Int? = null,
    val loadImages: Boolean = false,
    val projectName: String? = null,
    val uploadFor: String? = null,
    val filename: String? = null,

    val collected_by: String = "MOBILISER",
    val schoolVisitData: SchoolVisitData? = null,
    val schoolId: Int? = null,
    val areaId: String? = null,


    val location_name: String? = null,
    val area_id: String? = null,
    val project_id: String? = null,
    val location_type: String? = null,
    val external_id1: String? = null,
    val external_id1_description: String? = null,
    val external_id2: String? = null,
    val external_id2_description: String? = null,
    val location_ward: String? = null,
    val location_district: String? = null,
    val location_state: String? = null,

    val device_id: String? = null,
    val make: String? = null,
    val model: String? = null,
    val os: String? = null,
    val form2Data: JSONObject? = null
)
