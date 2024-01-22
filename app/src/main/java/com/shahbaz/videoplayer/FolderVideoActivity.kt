package com.shahbaz.videoplayer

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.shahbaz.videoplayer.adapter.VideoAdapter
import com.shahbaz.videoplayer.databinding.ActivityFolderVideoBinding
import com.shahbaz.videoplayer.dataclass.Folder
import com.shahbaz.videoplayer.dataclass.Video
import java.io.File

class FolderVideoActivity : AppCompatActivity() {
    private lateinit var binding : ActivityFolderVideoBinding
    private var position :Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.themeList[MainActivity.themeIndex])
        binding=ActivityFolderVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentFolderVideos= ArrayList()

        position= intent.getIntExtra("position",0)
        setupActionBar()
        currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
        binding.tvTotalVideos.text = "Total videos :${currentFolderVideos.size}"
        setupRecyclerview()
    }



    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title=MainActivity.folderList[position].folderName
    }

    private fun setupRecyclerview() {

        binding.recyclerViewVideoFA.apply {
            layoutManager= LinearLayoutManager(this@FolderVideoActivity)
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            adapter= VideoAdapter(currentFolderVideos,true)
        }
    }

    private fun getAllVideos(folderId : String): ArrayList<Video>{
        val tempList = ArrayList<Video>()
        val selection = MediaStore.Video.Media.BUCKET_ID + " like? "
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED
        )
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(folderId),
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



            while (cursor.moveToNext()) {
                val title = if (titleIndex != -1) cursor.getString(titleIndex) else ""
                val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0
                val videoId = if (videoIdIndex != -1) cursor.getLong(videoIdIndex) else 0
                val bucketDisplayName =
                    if (bucketDisplayNameIndex != -1) cursor.getString(bucketDisplayNameIndex) else ""

                val path = if (pathIndex != -1) cursor.getString(pathIndex) else ""
                val duration = if (durationIndex != -1) cursor.getLong(durationIndex) else 0


                try {
                    val file = File(path)
                    val uri = Uri.fromFile(file)
                    val video = Video(videoId.toString(),title,duration,bucketDisplayName,size.toString(),path,uri)

                    if(file.exists()){
                        tempList.add(video)
                    }


                }catch (_: Exception){}

            }
        }
        cursor?.close()

        return tempList
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    companion object{
        lateinit var currentFolderVideos  : ArrayList<Video>
    }
}