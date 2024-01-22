package com.shahbaz.videoplayer.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shahbaz.videoplayer.FolderVideoActivity
import com.shahbaz.videoplayer.MainActivity
import com.shahbaz.videoplayer.PlayerActivity
import com.shahbaz.videoplayer.R
import com.shahbaz.videoplayer.databinding.RenameLayoutBinding
import com.shahbaz.videoplayer.databinding.SpeedDialogueBinding
import com.shahbaz.videoplayer.databinding.VideoDetailsBinding
import com.shahbaz.videoplayer.databinding.VideoMoreFeaturesBinding
import com.shahbaz.videoplayer.databinding.VideoViewBinding
import com.shahbaz.videoplayer.dataclass.Video
import kotlinx.coroutines.NonDisposableHandle.parent
import java.io.File

class VideoAdapter(private var videoList: ArrayList<Video>, private val isFolder: Boolean = false) :
    RecyclerView.Adapter<VideoAdapter.viewholder>() {

    companion object {
        private const val LAST_PLAYED_VIDEO_TITLE_KEY = "last_played_video_title"
    }

    class viewholder(val binding: VideoViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(videolist: Video) {
            binding.apply {
                videoName.text = videolist.title
                videoFolderName.text = videolist.folderName
                videoDuration.text =
                    (DateUtils.formatElapsedTime(videolist.duration / 1000)).toString()

                Glide
                    .with(itemView)
                    .load(videolist.uri)
                    .placeholder(R.drawable.nav_header)
                    .into(videoImage)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewholder {
        return viewholder(
            VideoViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    override fun onBindViewHolder(holder: viewholder, position: Int) {
        val videolist = videoList[position]
        holder.bind(videolist)

        holder.itemView.setOnClickListener {

            when {
                videoList[position].id == PlayerActivity.nowPlayingId -> {
                    sendIntent(holder.itemView.context, position, "nowPlaying")
                }

                isFolder -> {
                    PlayerActivity.pipStatus = 1
                    sendIntent(holder.itemView.context, position, "FolderActivity")
                }

                MainActivity.search -> {
                    PlayerActivity.pipStatus = 2
                    sendIntent(holder.itemView.context, position, "searchView")
                }

                else -> {
                    PlayerActivity.pipStatus = 3
                    sendIntent(holder.itemView.context, position, "AllVideos")
                }
            }

        }

        holder.itemView.setOnLongClickListener {
            val moreOption =
                LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.video_more_features, holder.binding.root, false)
            val bindingMoreVideoFeature = VideoMoreFeaturesBinding.bind(moreOption)
            val moreDialogue =
                MaterialAlertDialogBuilder(holder.itemView.context).setView(moreOption)
                    .create()
            moreDialogue.show()

            bindingMoreVideoFeature.rename.setOnClickListener {
                requestStoragePermission(holder.itemView.context)
                moreDialogue.dismiss()
                val renameDilagoue =
                    LayoutInflater.from(holder.itemView.context)
                        .inflate(R.layout.rename_layout, holder.binding.root, false)
                val bindingRename = RenameLayoutBinding.bind(renameDilagoue)
                val moreDialogueRename =
                    MaterialAlertDialogBuilder(holder.itemView.context).setView(renameDilagoue)
                        .setPositiveButton("Rename") { self, _ ->
//                            self.dismiss()
                            val currentfile = File(videoList[position].path)
                            val newName = bindingRename.renamefield.text
                            if (newName != null && newName.isNotEmpty() && currentfile.exists()) {
                                val newFile = File(
                                    currentfile.parentFile,
                                    newName.toString() + "." + currentfile.extension
                                )
                                if (currentfile.renameTo(newFile)) {
                                    MediaScannerConnection.scanFile(
                                        holder.itemView.context, arrayOf(newFile.toString()),
                                        arrayOf("video/*"), null
                                    )
                                    when {
                                        MainActivity.search -> {
                                            MainActivity.searchList[position].title =
                                                newName.toString()
                                            MainActivity.searchList[position].path = newFile.path
                                            MainActivity.searchList[position].uri =
                                                Uri.fromFile(newFile)
                                            notifyItemChanged(position)
                                        }

                                        isFolder -> {
                                            FolderVideoActivity.currentFolderVideos[position].title =
                                                newName.toString()
                                            FolderVideoActivity.currentFolderVideos[position].path =
                                                newFile.path
                                            FolderVideoActivity.currentFolderVideos[position].uri =
                                                Uri.fromFile(newFile)
                                            MainActivity.dataChanged = true
                                            notifyItemChanged(position)

                                        }

                                        else -> {
                                            MainActivity.videoList[position].title =
                                                newName.toString()
                                            MainActivity.videoList[position].path = newFile.path
                                            MainActivity.videoList[position].uri =
                                                Uri.fromFile(newFile)
                                            notifyItemChanged(position)

                                        }
                                    }

                                } else {
                                    Toast.makeText(
                                        holder.itemView.context,
                                        "Permission Denied!!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .setNegativeButton("Cancel") { self, _ ->
                            self.dismiss()
                        }
                        .create()
                moreDialogueRename.show()
                bindingRename.renamefield.text = SpannableStringBuilder(videoList[position].title)
                moreDialogueRename.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(holder.itemView.context, R.attr.ThemeColor, Color.RED)
                )
                moreDialogueRename.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
                    MaterialColors.getColor(holder.itemView.context, R.attr.ThemeColor, Color.RED)
                )
            }

            bindingMoreVideoFeature.share.setOnClickListener {
                moreDialogue.dismiss()
                var shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                ContextCompat.startActivity(
                    holder.itemView.context,
                    Intent.createChooser(shareIntent, "Sharing video file"),
                    null
                )
            }

            bindingMoreVideoFeature.info.setOnClickListener {
                moreDialogue.dismiss()
                val infoDilagoue =
                    LayoutInflater.from(holder.itemView.context)
                        .inflate(R.layout.video_details, holder.binding.root, false)
                val bindingInfo = VideoDetailsBinding.bind(infoDilagoue)

                val moreDialogueInfo =
                    MaterialAlertDialogBuilder(holder.itemView.context).setView(infoDilagoue)
                        .setPositiveButton("Ok") { self, _ ->
                            self.dismiss()
                        }
                        .create()
                moreDialogueInfo.show()
                moreDialogueInfo.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(holder.itemView.context, R.attr.ThemeColor, Color.RED)
                )

                val infotext = SpannableStringBuilder().bold { append("Details\n\nName:") }
                    .append(videoList[position].title)
                    .bold { append("\n\nDuration:") }.append(DateUtils.formatElapsedTime(videoList[position].duration/1000))
                    .bold { append("\n\nFile Size:") }.append(Formatter.formatShortFileSize(holder.itemView.context,videoList[position].size.toLong()))
                    .bold { append("\n\nLocation:") }.append(videoList[position].path)
                bindingInfo.tvDetails.text=infotext

            }

            bindingMoreVideoFeature.delete.setOnClickListener {
                moreDialogue.dismiss()
                requestStoragePermission(holder.itemView.context)
                val dialgouedelete = MaterialAlertDialogBuilder(holder.itemView.context)
                    .setTitle("Are you sure , want to delete?")
                    .setMessage(videoList[position].title)
                    .setPositiveButton("Delete"){self,_ ->

                        val file =File(videoList[position].path)
                        if(file.exists() && file.delete()){
                            MediaScannerConnection.scanFile(holder.itemView.context, arrayOf(file.path),
                                arrayOf("video/*"),null)
                            when{
                                MainActivity.search ->{
                                    MainActivity.dataChanged=true
                                    videoList.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                isFolder ->{
                                    MainActivity.dataChanged=true
                                    FolderVideoActivity.currentFolderVideos.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                else->
                                {
                                MainActivity.videoList.removeAt(position)
                                notifyDataSetChanged()
                                }
                            }

                        }else{
                            Toast.makeText(holder.itemView.context,"Something Went Wrong!!",Toast.LENGTH_SHORT).show()
                        }

                        self.dismiss()
                    }
                    .setNegativeButton("cancel"){self,_ ->
                        self.dismiss()
                    }
                    .create()

                dialgouedelete.show()
                dialgouedelete.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(holder.itemView.context, R.attr.ThemeColor, Color.RED)
                )

                dialgouedelete.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
                    MaterialColors.getColor(holder.itemView.context, R.attr.ThemeColor, Color.RED)
                )
            }
            return@setOnLongClickListener true
        }


    }


    private fun sendIntent(context: Context, position: Int, ref: String) {
        PlayerActivity.position = position
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("Class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    fun updateList(searchList: ArrayList<Video>) {
        videoList = ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }


    //for requesting android 11 or higher storage permision
    private fun requestStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${context.applicationContext.packageName}")
                ContextCompat.startActivity(context, intent, null)
            }
        }

    }
}
