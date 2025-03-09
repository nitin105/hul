package com.hul.camera.skbImagePreview

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.core.net.toUri
import com.google.gson.Gson
import com.hul.HULApplication
import com.hul.R
import com.hul.api.ApiExtentions
import com.hul.api.ApiHandler
import com.hul.api.controller.APIController
import com.hul.api.controller.UploadFileController
import com.hul.camera.CameraComponent
import com.hul.camera.imagePreview.ImagePreviewViewModel
import com.hul.data.ProjectInfo
import com.hul.data.RequestModel
import com.hul.data.UploadImageData
import com.hul.data.VisitData
import com.hul.data.VisitDetails
import com.hul.databinding.FragmentImagePreviewBinding
import com.hul.databinding.FragmentSKBImagePreviewBinding
import com.hul.storage.SharedPreferencesStorage
import com.hul.user.UserInfo
import com.hul.utils.ConnectionDetector
import com.hul.utils.RetryInterface
import com.hul.utils.cancelProgressDialog
import com.hul.utils.noInternetDialogue
import com.hul.utils.redirectionAlertDialogue
import com.hul.utils.setProgressDialog
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class SKBImagePreviewFragment : Fragment(), ApiHandler, RetryInterface {

    private var _binding: FragmentSKBImagePreviewBinding? = null

    private lateinit var cameraComponent: CameraComponent

    @Inject
    lateinit var imagePreviewViewModel: SKBImagePreviewViewModel

    @Inject
    lateinit var userInfo: UserInfo

    @Inject
    lateinit var apiController: APIController

    @Inject
    lateinit var uploadFileController: UploadFileController

    @Inject
    lateinit var prefs: SharedPreferencesStorage

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSKBImagePreviewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        cameraComponent =
            (activity?.application as HULApplication).appComponent.cameraComponent()
                .create()
        cameraComponent.inject(this)
        binding.viewModel = imagePreviewViewModel
        binding.heading.text = requireArguments().getString("heading")

        if (prefs.getString("previewImage").isNotEmpty()) {
            val decodedString = Base64.decode(prefs.getString("previewImage"), Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            binding.ivMobiliser.setImageBitmap(decodedByte)
            binding.llPreview.visibility = View.VISIBLE
        }
        imagePreviewViewModel.projectInfo.value =
            Gson().fromJson(requireArguments().getString("projectInfo"), ProjectInfo::class.java)
        binding.mobiliserName.text = requireArguments().getString("mobilisername")
        binding.villageName.text = imagePreviewViewModel.projectInfo.value!!.location_name

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.save.setOnClickListener {

            uploadImage(imagePreviewViewModel.uri.value!!)


        }
        imagePreviewViewModel.lattitude.value = requireArguments().getString("lattitude")
        imagePreviewViewModel.longitude.value = requireArguments().getString("longitude")

        imagePreviewViewModel.uri.value = addTimestampToImage(
            requireContext(),
            requireArguments().getString("imageUri")!!.toUri()
        )

        val imageUri = imagePreviewViewModel.uri.value
        binding.ivCapture.setImageURI(imageUri)


        binding.retake.setOnClickListener {

            deleteImage(imagePreviewViewModel.uri.value.toString().toUri())
        }

        binding.date.text = SimpleDateFormat("dd MMM yyyy").format(Date())
        binding.time.text = SimpleDateFormat("hh:mm:ss").format(Date())


    }

    fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Video.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor?.moveToFirst()
            column_index?.let { cursor!!.getString(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }

    private fun copyFontToInternalStorage(context: Context, fontFileName: String): String {
        val outputFile = File(context.filesDir, fontFileName)
        if (!outputFile.exists()) {
            try {
                val inputStream: InputStream = context.assets.open(fontFileName)
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Log.d("File Path", outputFile.absolutePath)

        return outputFile.absolutePath
    }

    fun addTimestampToImage(context: Context, imageUri: Uri): Uri? {
        val originalExif = ExifInterface(imageUri.path!!)
        // Load the original image
        val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)

        // Correct orientation if needed
        val correctedBitmap = correctOrientation(originalBitmap, context, imageUri)

        // Create a mutable bitmap to draw onto
        val mutableBitmap = correctedBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Get the dimensions of the bitmap
        val bitmapWidth = mutableBitmap.width
        val bitmapHeight = mutableBitmap.height

        // Calculate the text size and padding based on the image dimensions
        val textSizeInPixels = 0.03f * bitmapHeight
        val padding = 16f * bitmapHeight

        // Draw the timestamp onto the bitmap
        val canvas = Canvas(mutableBitmap)
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        var newString =
            "Lon : ${imagePreviewViewModel.longitude.value} Lat : ${imagePreviewViewModel.lattitude.value}"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSizeInPixels
        }

        // Calculate the width and height of the timestamp text
        val textWidth = paint.measureText(timeStamp)
        val textHeight = paint.descent() - paint.ascent()


        val x = mutableBitmap.width - textWidth - textSizeInPixels // 50 is for some padding
        val y = mutableBitmap.height - textHeight - textSizeInPixels // 50 is for some padding

        canvas.drawText(timeStamp, x, y, paint)

        // Calculate the width and height of the timestamp text
        val textWidthLocation = paint.measureText(newString)
        val textHeightLocation = paint.descent() - paint.ascent()


        val xLocation =
            mutableBitmap.width - textWidthLocation - textSizeInPixels // 50 is for some padding
        val yLocation =
            mutableBitmap.height - textHeightLocation - (textSizeInPixels * 2) // 50 is for some padding

        val paintLocation = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSizeInPixels
        }

        canvas.drawText(newString, xLocation, yLocation, paintLocation)


        // Compress the modified bitmap to achieve a target file size of approximately 150 KB
        var quality = 100 // Start with maximum quality
        var outputStream: ByteArrayOutputStream
        do {
            outputStream = ByteArrayOutputStream()
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 5 // Reduce quality in steps until desired file size is achieved
        } while (outputStream.toByteArray().size > 700 * 1024 && quality > 0)


        // Insert the new image into the MediaStore
        val resolver = context.contentResolver
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, timeStamp + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val uri = resolver.insert(contentUri, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { out: OutputStream ->
                outputStream.writeTo(out)
                out.flush()
            }
        }
        copyDateExifData(requireContext(), imageUri, uri!!)

        return uri
    }


    fun copyDateExifData(context: Context, sourceUri: Uri, destinationUri: Uri) {
        try {
            // Get the source file path from the source URI
            val sourceFilePath = getPathFromUri(context, sourceUri)
            if (sourceFilePath == null) {
                // Handle error: Unable to get source file path
                return
            }

            // Get the destination file path from the destination URI
            val destinationFilePath = getPathFromUri(context, destinationUri)
            if (destinationFilePath == null) {
                // Handle error: Unable to get destination file path
                return
            }

            // Create ExifInterface instances for the source and destination files
            val sourceExif = ExifInterface(sourceFilePath)
            val destinationExif = ExifInterface(destinationFilePath)

            // Get the date-related attributes from the source ExifInterface
            val dateAttributes = listOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME_ORIGINAL
            )

            // Copy date-related attributes from the source to the destination ExifInterface
            for (attribute in dateAttributes) {
                val value = sourceExif.getAttribute(attribute)
                if (value != null) {
                    destinationExif.setAttribute(attribute, value)
                }
            }
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formattedDate = dateFormat.format(currentDate)
            destinationExif.setAttribute(ExifInterface.TAG_DATETIME, formattedDate)
            // Save the changes to the destination file
            destinationExif.saveAttributes()
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle error: IOException occurred
        }
    }

    fun getPathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        when {
            uri.scheme == "content" -> {
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        filePath = it.getString(columnIndex)
                    }
                }
            }

            uri.scheme == "file" -> {
                filePath = uri.path
            }

            else -> {
                // Handle other URI schemes if needed
            }
        }
        return filePath
    }

    private fun correctOrientation(bitmap: Bitmap, context: Context, uri: Uri): Bitmap {
        val exif = ExifInterface(context.contentResolver.openInputStream(uri)!!)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun deleteImage(uri: Uri) {
        try {
            val resolver = requireActivity().contentResolver
            val rowsDeleted = resolver.delete(uri, null, null)
            if (rowsDeleted > 0) {
                requireActivity().onBackPressed()
            } else {
                requireActivity().onBackPressed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the error
        }
    }

    private fun uploadImageModel(): RequestModel {
        var fileName: String = ""
        val visitPrefix = "project_" + userInfo.projectName;
        fileName = visitPrefix + "_location_image.jpeg";

        return RequestModel(
            project = userInfo.projectName,
            uploadFor = "field_audit",
            filename = fileName,
            visit_id = imagePreviewViewModel.projectInfo.value!!.visit_id.toString(),
        )
    }

    private fun uploadImage(imageUri: Uri) {
        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            setProgressDialog(requireContext(), "uploading")
            uploadFileController.getApiResponse(
                this,
                imageUri,
                uploadImageModel(),
                ApiExtentions.ApiDef.UPLOAD_IMAGE.ordinal
            )
        } else {
            noInternetDialogue(requireContext(), ApiExtentions.ApiDef.UPLOAD_IMAGE.ordinal, this)
        }
    }

    override fun onApiSuccess(o: String?, objectType: Int) {

        cancelProgressDialog()
        when (ApiExtentions.ApiDef.values()[objectType]) {

            ApiExtentions.ApiDef.SAVE_SCHOOL_ACTIVITY_DATA -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                if (!model.getBoolean("error")) {
                    Toast.makeText(requireContext(), "Data saved successfully", Toast.LENGTH_LONG)
                        .show()
                    val returnIntent = Intent()

                    returnIntent.putExtra(
                        "position",
                        requireArguments().getInt("position", 0)
                    )
                    returnIntent.putExtra(
                        "imageUrl",
                        imagePreviewViewModel.uri.value.toString()
                    )
                    returnIntent.putExtra(
                        "tag",
                        requireArguments().getString("tag")
                    )

                    requireActivity().setResult(Activity.RESULT_OK, returnIntent)
                    requireActivity().finish()
                } else {
                    redirectionAlertDialogue(requireContext(), model.getString("message"))
                }
            }

            ApiExtentions.ApiDef.UPLOAD_IMAGE -> {
                cancelProgressDialog()
                val model = JSONObject(o.toString())
                val uploadImageData = Gson().fromJson(
                    model.getJSONObject("data").toString(),
                    UploadImageData::class.java
                )

                imagePreviewViewModel.imageApiUrl1.value = uploadImageData.url
                submitForm()

            }

            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }
    }

    fun submitForm() {

        if (ConnectionDetector(requireContext()).isConnectingToInternet()) {
            setProgressDialog(requireContext(), "loading")
            apiController.getApiResponse(
                this,
                submitModel(),
                ApiExtentions.ApiDef.SAVE_SCHOOL_ACTIVITY_DATA.ordinal
            )
        } else {
            noInternetDialogue(
                requireContext(),
                ApiExtentions.ApiDef.SAVE_SCHOOL_ACTIVITY_DATA.ordinal,
                this
            )
        }

    }

    private fun submitModel(): RequestModel {

        return RequestModel(
            project = userInfo.projectName,
            visit_id = imagePreviewViewModel.projectInfo.value!!.visit_id.toString(),
            collected_by = userInfo.userType,
            visitData = VisitData(
                latitude = VisitDetails(value = imagePreviewViewModel.lattitude.value),
                longitude = VisitDetails(value = imagePreviewViewModel.longitude.value),
                visit_id = imagePreviewViewModel.projectInfo.value!!.visit_id.toString(),
                /*name_of_the_school_principal_auditor = VisitDetails(value = form1FillViewModel.visitData.value?.visit_1?.name_of_the_principal),
                number_of_the_school_principal_auditor = VisitDetails(value = form1FillViewModel.visitData.value?.visit_1?.mobile_number_of_the_principal),*/
            )
        )
    }

    override fun onApiError(message: String?) {
        redirectionAlertDialogue(requireContext(), message!!)
    }

    override fun retry(type: Int) {

        when (ApiExtentions.ApiDef.values()[type]) {
            ApiExtentions.ApiDef.UPLOAD_IMAGE -> uploadImage(imagePreviewViewModel.uri.value!!)
            else -> Toast.makeText(requireContext(), "Api Not Integrated", Toast.LENGTH_LONG).show()
        }

    }


}