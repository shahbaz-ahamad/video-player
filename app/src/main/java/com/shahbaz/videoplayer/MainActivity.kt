package com.shahbaz.videoplayer


import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.shahbaz.videoplayer.databinding.ActivityMainBinding
import com.shahbaz.videoplayer.dataclass.Video
import java.io.File
import kotlin.system.exitProcess
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shahbaz.videoplayer.databinding.SpeedDialogueBinding
import com.shahbaz.videoplayer.databinding.ThemeDialogueBinding
import com.shahbaz.videoplayer.dataclass.Folder
import com.shahbaz.videoplayer.helper.getAllVideos


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private  var runnable: Runnable?= null
    companion object {
        private const val READ_EXTERNAL_STORAGE_REQUEST = 123
        lateinit var videoList : ArrayList<Video>
        lateinit var folderList : ArrayList<Folder>
        var sortValue : Int =0
        lateinit var searchList : ArrayList<Video>
        var search : Boolean = false
        var themeIndex: Int =1
        val themeList = arrayOf(R.style.coolpink,R.style.coolBlue,R.style.coolPurple,R.style.coolGreen,R.style.coolRed,R.style.coolBlack)
        var dataChanged:Boolean = false
        var adapterChanged:Boolean = false
        val sortList = arrayOf(MediaStore.Video.Media.DATE_ADDED +" DESC",MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.TITLE,MediaStore.Video.Media.TITLE+" DESC",MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.SIZE+" DESC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //retrieve theme index
        val indexEditor = getSharedPreferences("Theme", MODE_PRIVATE)
        themeIndex= indexEditor.getInt("themeIndex",0)
        setTheme(themeList[themeIndex])
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomViewWithNavController()
        videoList = ArrayList()
        folderList = ArrayList()
        checkPermission()
        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.navView.setNavigationItemSelectedListener {

            when (it.itemId) {
                R.id.themes_nav -> {
                    showThemeDialgoue()
                }
                R.id.sort_order -> {
                    sortVideos()
                }
                R.id.about -> Toast.makeText(this, "about", Toast.LENGTH_SHORT).show()
                R.id.exit -> exitProcess(1)
            }
            return@setNavigationItemSelectedListener true
        }
    }

    private fun sortVideos() {
        val menuItmes = arrayOf("Latest","Oldest","Name(A to Z)","Name(Z to A)","File Size(small)","File Size(large)")
        var value = sortValue
        val sortDialogue = MaterialAlertDialogBuilder(this)
            .setTitle("Sort By...")
            .setPositiveButton("Ok"){self,_ ->
                saveSorting(value)

            }
            .setSingleChoiceItems(menuItmes, sortValue){ _, pos ->
                value =pos
            }
            .create()
        sortDialogue.show()

        //to set the color of the button according to the theme
        sortDialogue.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(MaterialColors.getColor(this,R.attr.ThemeColor,Color.RED))

    }

    private fun showThemeDialgoue() {
        val customDilogueTheme =
            LayoutInflater.from(this).inflate(R.layout.theme_dialogue, binding.root, false)
        val bindingTheme = ThemeDialogueBinding.bind(customDilogueTheme)
        val themeDialogue = MaterialAlertDialogBuilder(this).setView(customDilogueTheme)
            .setTitle("Select Color...")
            .create()
        themeDialogue.show()

        //set the background to yellow to the of the selected background
        when(themeIndex){
            0-> bindingTheme.themePink.setBackgroundColor(Color.YELLOW)
            1-> bindingTheme.themeBlue.setBackgroundColor(Color.YELLOW)
            2-> bindingTheme.themePurple.setBackgroundColor(Color.YELLOW)
            3-> bindingTheme.themeGreen.setBackgroundColor(Color.YELLOW)
            4-> bindingTheme.themeRed.setBackgroundColor(Color.YELLOW)
            5-> bindingTheme.themeBlack.setBackgroundColor(Color.YELLOW)
        }

        //choose the theme
        bindingTheme.themePink.setOnClickListener {
           saveTheme(0)
        }
        bindingTheme.themeBlue.setOnClickListener {
            saveTheme(1)
        }
        bindingTheme.themePurple.setOnClickListener {
            saveTheme(2)
        }
        bindingTheme.themeGreen.setOnClickListener {
            saveTheme(3)
        }
        bindingTheme.themeRed.setOnClickListener {
            saveTheme(4)
        }
        bindingTheme.themeBlack.setOnClickListener {
            saveTheme(5)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupBottomViewWithNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        // Associate the NavController with BottomNavigationView
        binding.bottomNavigationView.setupWithNavController(navController)
    }



    private fun checkPermission(){
        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(
                this,
                READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED)  {
            // Permission is already granted
            // You can proceed with accessing videos or other functionalities
            runnable = Runnable {
                if (runnable != null && dataChanged) {
                    videoList = getAllVideos(this@MainActivity)
                    dataChanged = false
                    adapterChanged = true
                }
                if (runnable != null) {
                    Handler(Looper.getMainLooper()).postDelayed(runnable!!, 300)
                }
            }
            Handler(Looper.getMainLooper()).postDelayed(runnable!!, 0)
            videoList = getAllVideos(this@MainActivity)

        } else {
            // Permission is not granted, request it
            requestStoragePermission()
        }


    }

    private fun requestStoragePermission() {
        // Check if the device is running Android 6.0 (API level 23) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_MEDIA_VIDEO,READ_MEDIA_AUDIO),
                READ_EXTERNAL_STORAGE_REQUEST
            )
        }
        // For devices below Android 6.0, the permission is granted at installation time
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with accessing videos
                    videoList= getAllVideos(this@MainActivity)
                } else {
                    // Permission denied, handle this case (show a message, disable features, etc.)
                    // You may want to inform the user why the permission is necessary
                    requestStoragePermission()
                }

            }
            // Handle other permissions if needed

        }
    }


    private fun saveTheme(index : Int){
        val indexeditor = getSharedPreferences("Theme", MODE_PRIVATE).edit()
        indexeditor.putInt("themeIndex",index)
        indexeditor.apply()

            //for restarting app
        finish()
        startActivity(intent)
    }

    private fun saveSorting(index : Int){
        val sorteditor = getSharedPreferences("Sort", MODE_PRIVATE).edit()
        sorteditor.putInt("sortIndex",index)
        sorteditor.apply()
        //for restarting app
        finish()
        startActivity(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        runnable= null
    }
}