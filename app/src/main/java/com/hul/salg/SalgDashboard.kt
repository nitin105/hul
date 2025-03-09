package com.hul.salg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.navigation.ui.AppBarConfiguration
import com.hul.R
import com.hul.databinding.ActivitySalgDashboardBinding
import com.hul.salg.ui.salgPreForm.SalgPreFormFragment

class SalgDashboard : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivitySalgDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySalgDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onBackPressed() {
        val myFragment: SalgPreFormFragment? =
            supportFragmentManager.findFragmentById(R.id.salgPreFormFragment) as SalgPreFormFragment?
        if (myFragment != null && myFragment.isVisible()) {
            val intent = Intent(this, SalgDashboard::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
        else{
            super.onBackPressed()
        }

    }

}