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
import android.util.Log
import com.shahbaz.videoplayer.dataclass.Folder


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.coolpink)
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
                R.id.feedback -> Toast.makeText(this, "Feedback", Toast.LENGTH_SHORT).show()
                R.id.themes_nav -> Toast.makeText(this, "theme", Toast.LENGTH_SHORT).show()
                R.id.sort_order -> Toast.makeText(this, "sort", Toast.LENGTH_SHORT).show()
                R.id.about -> Toast.makeText(this, "about", Toast.LENGTH_SHORT).show()
                R.id.exit -> exitProcess(1)
            }
            return@setNavigationItemSelectedListener true
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

    private fun getAllVideos(): ArrayList<Video>{
        val tempList = ArrayList<Video>()
        val tempFolder = ArrayList<String>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        if (cursor != null) {
            val titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
            val videoIdIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val bucketDisplayNameIndex =
                cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val pathIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val bucketIdIndex = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)


            while (cursor.moveToNext()) {
                val title = if (titleIndex != -1) cursor.getString(titleIndex) else ""
                val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0
                val videoId = if (videoIdIndex != -1) cursor.getLong(videoIdIndex) else 0
                val bucketDisplayName =
                    if (bucketDisplayNameIndex != -1) cursor.getString(bucketDisplayNameIndex) else ""
                val bucketId = if(bucketIdIndex != -1) cursor.getLong(bucketIdIndex) else 0

                val path = if (pathIndex != -1) cursor.getString(pathIndex) else ""
                val duration = if (durationIndex != -1) cursor.getLong(durationIndex) else 0


                try {
                    val file = File(path)
                    val uri =Uri.fromFile(file)
                    val video = Video(videoId.toString(),title,duration,bucketDisplayName,size.toString(),path,uri)

                    if(file.exists()){
                        tempList.add(video)
                    }

                        if(!tempFolder.contains(bucketDisplayName)){
                            tempFolder.add(bucketDisplayName)
                            //adding the folder to the folder list
                            val folder = Folder(bucketId.toString(),bucketDisplayName.toString())
                            folderList.add(folder)//adding the folder to the folder list
                        }


                }catch (_: Exception){}

            }
        }
        cursor?.close()

        return tempList
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
            videoList= getAllVideos()
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
                    videoList= getAllVideos()
                } else {
                    // Permission denied, handle this case (show a message, disable features, etc.)
                    // You may want to inform the user why the permission is necessary
                    requestStoragePermission()
                }

            }
            // Handle other permissions if needed

        }
    }


    companion object {
        private const val READ_EXTERNAL_STORAGE_REQUEST = 123
        lateinit var videoList : ArrayList<Video>
        lateinit var folderList : ArrayList<Folder>

        lateinit var searchList : ArrayList<Video>
        var search : Boolean = false

    }



}