package com.hul.camera

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.hul.R
import com.hul.databinding.ActivityCameraBinding

class CameraActivity : FragmentActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bundle = Bundle()

        bundle.putInt("position", intent.getIntExtra("position", 0))
        bundle.putString("imageType", intent.getStringExtra("imageType"))
        bundle.putString("heading", intent.getStringExtra("heading"))
        if (intent.getStringExtra("tag") != null)
            bundle.putString("tag", intent.getStringExtra("tag"))
        else
            bundle.putString("tag", "")

        if (intent.getStringExtra("project") != null)
        bundle.putString("project", intent.getStringExtra("project"))
        if (intent.getStringExtra("projectInfo") != null)
        bundle.putString("projectInfo", intent.getStringExtra("projectInfo"))
        if (intent.getStringExtra("mobilisername") != null)
        bundle.putString("mobilisername", intent.getStringExtra("mobilisername"))


        findNavController(R.id.nav_host_fragment_camera).setGraph(R.navigation.camera_nav_graph)
        if (intent.getStringExtra("viewType") != null && intent.getStringExtra("viewType").equals("Portrait"))
        {
            bundle.putString("viewType", intent.getStringExtra("Portrait"))
            findNavController(R.id.nav_host_fragment_camera).navigate(
                R.id.cameraPreviewPotraitFragment,
                bundle
            )
        }
        else{
            bundle.putString("viewType", intent.getStringExtra("Landscape"))
            findNavController(R.id.nav_host_fragment_camera).navigate(
                R.id.cameraPreviewFragment,
                bundle
            )
        }



//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }


    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment_camera)
        if (navController.currentBackStackEntry?.destination?.id == R.id.cameraPreviewFragment) {
            // If we are at the last fragment, finish the activity
            finish() // Explicitly finish the activity
        } else {
            // Otherwise, let the NavController handle the back press
            if (!navController.popBackStack()) {
                super.onBackPressed()
            }
        }
    }

}




//
//}