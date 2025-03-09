package com.hul.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hul.R
import com.hul.sb.supervisor.SBSupervisorDashboard
import java.io.File
import java.io.IOException

/**
 * Created by Nitin Chorge on 05-09-2024.
 */
class AudioRecordService : Service() {

    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var recordingFile: File

    override fun onCreate() {
        super.onCreate()

        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        recordingFile = File(outputDir, "background_recording_${System.currentTimeMillis()}.mp3")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(recordingFile.absolutePath)
        }

        try {
            mediaRecorder.prepare()
        } catch (e: IOException) {
            Log.e("AudioRecordService", "MediaRecorder preparation failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()

        mediaRecorder.start()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder.stop()
        mediaRecorder.release()

        sendBroadcastWithFileUri()
    }

    private fun sendBroadcastWithFileUri() {
        val ourIntent = Intent("com.hul.RECORDED_FILE_URI").apply{ putExtra("fileUri", Uri.fromFile(recordingFile).toString()) }
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(ourIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundService() {
        val notificationChannelId = "AudioRecordServiceChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Audio Recording Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, SBSupervisorDashboard::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE // Add this flag
        )

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Recording Audio")
            .setContentText("Recording audio in the background")
            .setSmallIcon(R.drawable.audio)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }
}