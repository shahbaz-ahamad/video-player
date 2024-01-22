package com.shahbaz.videoplayer.helper

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.shahbaz.videoplayer.MainActivity
import com.shahbaz.videoplayer.MainActivity.Companion.sortList
import com.shahbaz.videoplayer.dataclass.Folder
import com.shahbaz.videoplayer.dataclass.Video
import java.io.File
fun getAllVideos(context: Context): ArrayList<Video>{
    val sorteditor = context.getSharedPreferences("Sort", AppCompatActivity.MODE_PRIVATE)
    MainActivity.sortValue =sorteditor.getInt("sortIndex",0)
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
    val cursor = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortList[MainActivity.sortValue]
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
                val uri = Uri.fromFile(file)
                val video = Video(videoId.toString(),title,duration,bucketDisplayName,size.toString(),path,uri)

                if(file.exists()){
                    tempList.add(video)
                }

                if(!tempFolder.contains(bucketDisplayName)){
                    tempFolder.add(bucketDisplayName)
                    //adding the folder to the folder list
                    val folder = Folder(bucketId.toString(),bucketDisplayName.toString())
                    MainActivity.folderList.add(folder)//adding the folder to the folder list
                }


            }catch (_: Exception){}

        }
    }
    cursor?.close()

    return tempList
}