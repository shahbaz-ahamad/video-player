package com.shahbaz.videoplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.browse.MediaBrowser.MediaItem
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.AspectRatioListener
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shahbaz.videoplayer.databinding.ActivityPlayerBinding
import com.shahbaz.videoplayer.databinding.MoreFeaturesBinding
import com.shahbaz.videoplayer.databinding.SpeedDialogueBinding
import com.shahbaz.videoplayer.dataclass.Video
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var playPuuseButton: ImageButton
    private lateinit var fullscreenButton: ImageButton
    private lateinit var videoTitle: TextView

    companion object {
        private var audioManager: AudioManager? = null
        lateinit var player: SimpleExoPlayer
        lateinit var playerlist: ArrayList<Video>
        var position: Int = -1
        var repeat: Boolean = false
        var isFullScreen: Boolean = false
        var isLocked: Boolean = false
        lateinit var trackSelector: DefaultTrackSelector
        var isSubtitle: Boolean = true
        var speed: Float = 1.0f
        var timer: Timer? = null
        var pipStatus: Int = 0
        var nowPlayingId: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.themeList[MainActivity.themeIndex])
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        //to display the video arround the edge of the camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)

        supportActionBar?.hide()
        setContentView(binding.root)

        //find the id of the custom control
        playPuuseButton = findViewById(R.id.playPuuseButton)
        videoTitle = findViewById(R.id.video_title)
        fullscreenButton = findViewById(R.id.fullscreenButton)

        //for the full screen mode
        WindowCompat.setDecorFitsSystemWindows(window, false)//false to hide the status bar
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        }


        //for handling video file intent
        try {
            if (intent?.data != null && intent.data?.scheme?.contentEquals("content") == true) {
                playerlist = ArrayList()
                position = 0
                val cursor = contentResolver.query(
                    intent.data!!,
                    arrayOf(MediaStore.Video.Media.DATA),
                    null,
                    null,
                    null
                )
                cursor?.let {
                    it.moveToFirst()
                    try {
                        val path =
                            it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                        val file = File(path)
                        val video = Video("", file.name, 0L, "", "", "", Uri.fromFile(file))
                        playerlist.add(video)
                        cursor.close()
                    } catch (e: Exception) {
                        val tempPath = getPathFromURI(context = this, uri = intent.data!!)
                        val tempFile = File(tempPath)
                        val video = Video("", tempFile.name, 0L, "", "", "", Uri.fromFile(tempFile))
                        playerlist.add(video)
                        cursor.close()
                    }

                }
                createPlayer()
                initilizeBinding()
            } else {
                initalizeLayout()
                initilizeBinding()
            }
        } catch (e: Exception) {
            Toast.makeText(this@PlayerActivity, e.toString(), Toast.LENGTH_SHORT).show()
            Log.d("error", e.toString())
        }


    }


    private fun initalizeLayout() {

        when (intent.getStringExtra("Class")) {
            "AllVideos" -> {
                playerlist = ArrayList()
                playerlist.addAll(MainActivity.videoList)
                createPlayer()
            }

            "FolderActivity" -> {
                playerlist = ArrayList()
                playerlist.addAll(FolderVideoActivity.currentFolderVideos)
                createPlayer()
            }

            "searchView" -> {
                playerlist = ArrayList()
                playerlist.addAll(MainActivity.searchList)
                createPlayer()
            }

            "nowPlaying" -> {
                speed = 1.0f
                videoTitle.apply {
                    text = playerlist[position].title
                    isSelected = true
                }
                DoubleTapEnable()
                playVideo()
                playInFullScreen(enable = isFullScreen)
                SeekBarFeatures()

            }


        }
//        createPlayer()

    }

    private fun nextAndPrevVideo(isNext: Boolean = true) {

        if (isNext) {
            setPostion()
        } else {
            setPostion(false)
        }
        createPlayer()
    }

    private fun setPostion(isIncrement: Boolean = true) {
        //change the video only when the repear is off
        if (!repeat) {
            if (isIncrement) {
                if (playerlist.size - 1 == position) {
                    //no next video
                    Toast.makeText(this@PlayerActivity, "No Next Video", Toast.LENGTH_SHORT).show()
                } else {
                    ++position
                }
            } else {
                if (position == 0) {
                    //no prev video
                    Toast.makeText(this@PlayerActivity, "No Previous Video", Toast.LENGTH_SHORT)
                        .show()

                } else {
                    --position
                }
            }
        }

    }

    private fun initilizeBinding() {


        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        playPuuseButton.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }

        findViewById<ImageButton>(R.id.nextButton).setOnClickListener {
            nextAndPrevVideo()
        }
        findViewById<ImageButton>(R.id.prevButton).setOnClickListener {
            nextAndPrevVideo(false)
        }

        //for the repeat button
        findViewById<ImageButton>(R.id.repeatBtn).setOnClickListener {

            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }

        //for the full screen
        fullscreenButton.setOnClickListener {
            if (isFullScreen) {
                isFullScreen = false
                playInFullScreen(false)
            } else {
                isFullScreen = true
                playInFullScreen(true)
            }
        }

        //for the lockButton
        binding.lockButton.setOnClickListener {
            if (!isLocked) {
                //then hide it
                isLocked = true
                binding.playerview.hideController()
                binding.playerview.useController = false
                binding.lockButton.setImageResource(R.drawable.lock)
            } else {
                isLocked = false
                binding.playerview.useController = true
                binding.playerview.showController()
                binding.lockButton.setImageResource(R.drawable.lock_open)
            }
        }

        findViewById<ImageButton>(R.id.moreFeatures).setOnClickListener {
            pauseVideo()
            //show the custom dailogue
            val customDialogue =
                LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialogue)

            val dilagoue =
                MaterialAlertDialogBuilder(this).setView(customDialogue).setOnCancelListener {
                    playVideo()
                    playPuuseButton.visibility = View.VISIBLE
                }
                    .setBackground(ColorDrawable(0x8021130d.toInt()))
                    .create()
            dilagoue.show()


            //for the audio track click
            bindingMF.audioTrack.setOnClickListener {
                dilagoue.dismiss()

                //extract th audio from the trackselector
                val audioTrack = HashSet<String>()
                for (i in 0 until player.currentTrackGroups.length) {
                    if (player.currentTrackGroups.get(i)
                            .getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT
                    ) {
                        audioTrack.add(
                            Locale(
                                player.currentTrackGroups.get(i).getFormat(0).language.toString()
                            ).displayLanguage
                        )
                    }
                }

                //convert the array of the audiotrack to charsequenece
                val tempAudioTrack = audioTrack.toTypedArray()
                MaterialAlertDialogBuilder(this, R.style.alertDialogue)
                    .setTitle("Select Language!")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x8021130d.toInt()))
                    .setItems(tempAudioTrack) { _, position ->
                        val selectedLanguage = tempAudioTrack[position].toString()
                        Toast.makeText(
                            this@PlayerActivity,
                            selectedLanguage + " Selected",
                            Toast.LENGTH_SHORT
                        ).show()
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setPreferredAudioLanguage(selectedLanguage)
                        )
                    }
                    .create()
                    .show()
            }


            //subtitle
            bindingMF.subtitleButton.setOnClickListener {
                if (isSubtitle) {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, true
                        ).build()
                    Toast.makeText(this@PlayerActivity, "Subtitle Off", Toast.LENGTH_SHORT).show()
                    isSubtitle = false
                } else {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, false
                        ).build()
                    Toast.makeText(this@PlayerActivity, "Subtitle On", Toast.LENGTH_SHORT).show()
                    isSubtitle = true

                }
                dilagoue.dismiss()
                playVideo()
            }


            bindingMF.speed.setOnClickListener {
                dilagoue.dismiss()
                playVideo()
                val customDilogueSpeed =
                    LayoutInflater.from(this).inflate(R.layout.speed_dialogue, binding.root, false)
                val bindingSpeed = SpeedDialogueBinding.bind(customDilogueSpeed)
                val speedDialogue = MaterialAlertDialogBuilder(this).setView(customDilogueSpeed)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->

                        self.dismiss()//to dismis the dilagoue
                    }
                    .setBackground(ColorDrawable(0x8021130d.toInt()))
                    .create()

                speedDialogue.show()
                bindingSpeed.tvSpeed.text = "${DecimalFormat("#.##").format(speed)} X"
                bindingSpeed.decreaseButton.setOnClickListener {
                    changeSpped(false)
                    bindingSpeed.tvSpeed.text = "${DecimalFormat("#.##").format(speed)} X"
                }

                bindingSpeed.increaseButton.setOnClickListener {
                    changeSpped(true)
                    bindingSpeed.tvSpeed.text = "${DecimalFormat("#.##").format(speed)}X"
                }
            }



            bindingMF.timer.setOnClickListener {
                dilagoue.dismiss()
                if (timer != null) {
                    Toast.makeText(
                        this@PlayerActivity,
                        "Timer already running\nClose app to stop timer",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    var sleepTime = 15
                    val customDilogueSpeed = LayoutInflater.from(this)
                        .inflate(R.layout.speed_dialogue, binding.root, false)
                    val bindingSpeed = SpeedDialogueBinding.bind(customDilogueSpeed)
                    val speedDialogue = MaterialAlertDialogBuilder(this).setView(customDilogueSpeed)
                        .setPositiveButton("OK") { self, _ ->

                            timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    moveTaskToBack(true)
                                    exitProcess(1)
                                }
                            }

                            timer!!.schedule(task, sleepTime * 60 * 1000.toLong())
                            self.dismiss()//to dismis the dilagoue
                            playVideo()
                        }
                        .setBackground(ColorDrawable(0x8021130d.toInt()))
                        .create()
                    speedDialogue.show()
                    bindingSpeed.tvSpeed.text = "$sleepTime Min"
                    bindingSpeed.decreaseButton.setOnClickListener {
                        if (sleepTime > 15) {
                            sleepTime -= 15
                        }

                        bindingSpeed.tvSpeed.text = "$sleepTime Min"
                    }

                    bindingSpeed.increaseButton.setOnClickListener {
                        if (sleepTime < 120) {
                            sleepTime += 15
                        }
                        bindingSpeed.tvSpeed.text = "$sleepTime Min"
                    }

                }
            }

            bindingMF.pictureInPicture.setOnClickListener {
                val appOpsService = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appOpsService.checkOpNoThrow(
                        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                        android.os.Process.myUid(),
                        packageName
                    ) ==
                            AppOpsManager.MODE_ALLOWED
                } else {
                    //if permission not allowed
                    false
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (status) {
                        this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                        dilagoue.dismiss()
                        binding.playerview.hideController()
                        playVideo()
                        pipStatus = 0

                    } else {
                        val intent = Intent(
                            "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(
                        this@PlayerActivity,
                        "picture in picture mode not supported",
                        Toast.LENGTH_SHORT
                    ).show()
                    playVideo()
                }
            }
        }


    }

    private fun playVideo() {
        playPuuseButton.setImageResource(R.drawable.pause)
        player.play()
    }

    private fun pauseVideo() {
        playPuuseButton.setImageResource(R.drawable.play)
        player.pause()
    }

    private fun createPlayer() {

        try {
            player.release()
        } catch (e: Exception) {
        }

        speed = 1.0f
        //initialize the trackSelector
        trackSelector = DefaultTrackSelector(this)

        videoTitle.apply {
            text = playerlist[position].title
            isSelected = true
        }
        player =
            SimpleExoPlayer.Builder(this@PlayerActivity).setTrackSelector(trackSelector).build()
        DoubleTapEnable()
        val mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(playerlist[position].uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()

        //when the video is completed
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                if (playbackState == Player.STATE_ENDED) {
                    nextAndPrevVideo()
                }
            }
        })


        //for the seekbar preivew
        SeekBarFeatures()
        //listner for the playerview
        binding.playerview.setControllerVisibilityListener {
            when {
                isLocked -> {
                    binding.lockButton.visibility = View.VISIBLE
                }

                binding.playerview.isControllerVisible -> binding.lockButton.visibility =
                    View.VISIBLE

                else -> {
                    binding.lockButton.visibility = View.GONE
                }
            }

        }
        playInFullScreen(enable = isFullScreen)
        nowPlayingId = playerlist[position].id

    }


    private fun playInFullScreen(enable: Boolean) {
        if (enable) {
            binding.playerview.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullscreenButton.setImageResource(R.drawable.fullscreen_exit)

        } else {
            binding.playerview.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullscreenButton.setImageResource(R.drawable.fullscreen)
        }
    }

    private fun changeSpped(isSpeedIncrement: Boolean) {

        if (isSpeedIncrement) {

            if (speed < 3.0f) {
                speed += 0.25f
            }
        } else {

            if (speed > 0.25f) {
                speed -= 0.25f
            }
        }
        player.setPlaybackSpeed(speed)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("Class", "FolderActivity")
                2 -> intent.putExtra("Class", "SearchedVideos")
                3 -> intent.putExtra("Class", "AllVideos")
            }
            startActivity(intent)
        }
        if (!isInPictureInPictureMode) pauseVideo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()

        audioManager?.abandonAudioFocus(this)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("class", "FolderActivity")
                2 -> intent.putExtra("class", "SearchedVideos")
                3 -> intent.putExtra("class", "AllVideos")
            }
            startActivity(intent)
        }
        if (!isInPictureInPictureMode) pauseVideo()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        if (audioManager == null) {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

        audioManager!!.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    //used to get path of video selected by user (if column data fails to get path)
    private fun getPathFromURI(context: Context, uri: Uri): String {
        var filePath = ""
        // ExternalStorageProvider
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(':')
        val type = split[0]

        return if ("primary".equals(type, ignoreCase = true)) {
            "${Environment.getExternalStorageDirectory()}/${split[1]}"
        } else {
            //getExternalMediaDirs() added in API 21
            val external = context.externalMediaDirs
            if (external.size > 1) {
                filePath = external[1].absolutePath
                filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1]
            }
            filePath
        }
    }


    private fun DoubleTapEnable() {
        binding.playerview.player = player
        binding.ytOVerlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOVerlay.visibility = View.INVISIBLE
            }

            override fun onAnimationStart() {
                binding.ytOVerlay.visibility = View.VISIBLE
            }

        })

        binding.ytOVerlay.player(player)
    }

    private fun SeekBarFeatures(){
        findViewById<DefaultTimeBar>(com.google.android.exoplayer2.ui.R.id.exo_progress)
            .addListener(object  : TimeBar.OnScrubListener{
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    pauseVideo()
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    player.seekTo(position)
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    playVideo()
                }

            })
    }
}