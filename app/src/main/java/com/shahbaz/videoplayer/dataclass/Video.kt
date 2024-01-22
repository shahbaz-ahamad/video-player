package com.shahbaz.videoplayer.dataclass

import android.net.Uri

data class Video(
    val id : String,
    var title: String,
    val duration : Long=0,
    val folderName: String,
    val size: String,
    var path :String,
    var uri : Uri
)

