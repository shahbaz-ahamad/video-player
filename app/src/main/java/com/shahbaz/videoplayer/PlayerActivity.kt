package com.shahbaz.videoplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.browse.MediaBrowser.MediaItem
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.AspectRatioListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shahbaz.videoplayer.databinding.ActivityPlayerBinding
import com.shahbaz.videoplayer.databinding.MoreFeaturesBinding
import com.shahbaz.videoplayer.databinding.SpeedDialogueBinding
import com.shahbaz.videoplayer.dataclass.Video
import java.text.DecimalFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var runnable: Runnable

    companion object {
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
        var nowPlayingId: String=""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        //to display the video arround the edge of the camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)

        supportActionBar?.hide()
        setTheme(R.style.playerActivity)
        setContentView(binding.root)

        //for the full screen mode
        WindowCompat.setDecorFitsSystemWindows(window, false)//false to hide the status bar
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        }


        initalizeLayout()
        initilizeBinding()

        binding.forwardFrameLayout.setOnClickListener(DoubleClickListener(callback = object : DoubleClickListener.Callback{
            override fun doubleClicked() {
                binding.playerview.showController()
                binding.forwardButton.visibility=View.VISIBLE
                player.seekTo(player.currentPosition + 10000)
            }
        }))

        binding.rewindFrameLayout.setOnClickListener(DoubleClickListener(callback = object : DoubleClickListener.Callback{
            override fun doubleClicked() {
                binding.playerview.showController()
                binding.rewindButton.visibility=View.VISIBLE
                player.seekTo(player.currentPosition - 10000)
            }
        }))

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

            "nowPlaying"-> {
                speed = 1.0f
                binding.videoTitle.apply {
                    text = playerlist[position].title
                    isSelected = true
                }
                binding.playerview.player = player
                playVideo()
                playInFullScreen(enable = isFullScreen)
                setVisibility()
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


        binding.backButton.setOnClickListener {
            finish()
        }

        binding.playPuuseButton.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }

        binding.nextButton.setOnClickListener {
            nextAndPrevVideo()
        }
        binding.prevButton.setOnClickListener {
            nextAndPrevVideo(false)
        }

        //for the repeat button
        binding.repeatBtn.setOnClickListener {

            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }

        //for the full screen
        binding.fullscreenButton.setOnClickListener {
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
                binding.playerview.showController()
                binding.playerview.useController = true
                binding.lockButton.setImageResource(R.drawable.lock_open)

            }
        }

        binding.moreFeatures.setOnClickListener {
            pauseVideo()
            //show the custom dailogue
            val customDialogue =
                LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialogue)

            val dilagoue =
                MaterialAlertDialogBuilder(this).setView(customDialogue).setOnCancelListener {
                    playVideo()
                    binding.playPuuseButton.visibility = View.VISIBLE
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
        binding.playPuuseButton.setImageResource(R.drawable.pause)
        player.play()
    }

    private fun pauseVideo() {
        binding.playPuuseButton.setImageResource(R.drawable.play)
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

        binding.videoTitle.apply {
            text = playerlist[position].title
            isSelected = true
        }
        player =
            SimpleExoPlayer.Builder(this@PlayerActivity).setTrackSelector(trackSelector).build()
        binding.playerview.player = player
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

        playInFullScreen(enable = isFullScreen)
        setVisibility()
        nowPlayingId= playerlist[position].id

    }


    private fun playInFullScreen(enable: Boolean) {
        if (enable) {
            binding.playerview.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            binding.fullscreenButton.setImageResource(R.drawable.fullscreen_exit)

        } else {
            binding.playerview.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            binding.fullscreenButton.setImageResource(R.drawable.fullscreen)
        }
    }

    private fun setVisibility() {
        runnable = Runnable {
            if (binding.playerview.isControllerVisible) {
                changeVisibilty(View.VISIBLE)
            } else {
                changeVisibilty(View.INVISIBLE)
            }
            Handler(Looper.getMainLooper()).postDelayed(runnable, 300)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
    }

    private fun changeVisibilty(visibilty: Int) {
        binding.topController.visibility = visibilty
        binding.playPuuseButton.visibility = visibilty
        binding.bottomController.visibility = visibilty
        binding.lockButton.visibility = visibilty

        binding.rewindButton.visibility=View.GONE
        binding.forwardButton.visibility=View.GONE

        if (isLocked) {
            binding.lockButton.visibility = View.VISIBLE
        }

        //to dissable the forwad and backward when the screen is locked
        binding.forwardFrameLayout.visibility=visibilty
        binding.rewindFrameLayout.visibility=visibilty
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
        if(pipStatus != 0){
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when(pipStatus){
                1 -> intent.putExtra("Class","FolderActivity")
                2 -> intent.putExtra("Class","SearchedVideos")
                3 -> intent.putExtra("Class","AllVideos")
            }
            startActivity(intent)
        }
        if(!isInPictureInPictureMode) pauseVideo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }


}