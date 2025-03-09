package com.hul.skb.supervisor

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.ui.AppBarConfiguration
import com.hul.R
import com.hul.databinding.ActivitySbsupervisorDashboardBinding
import com.hul.databinding.ActivitySkbsupervisorDashboardBinding

class SKBSupervisorDashboard : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivitySkbsupervisorDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySkbsupervisorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}