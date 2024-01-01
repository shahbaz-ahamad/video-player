package com.shahbaz.videoplayer.adapter

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shahbaz.videoplayer.MainActivity
import com.shahbaz.videoplayer.PlayerActivity
import com.shahbaz.videoplayer.R
import com.shahbaz.videoplayer.databinding.VideoViewBinding
import com.shahbaz.videoplayer.dataclass.Video

class VideoAdapter(private var videoList: ArrayList<Video>, private val isFolder: Boolean = false) :
    RecyclerView.Adapter<VideoAdapter.viewholder>() {


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
                    sendIntent(holder.itemView.context,position, "nowPlaying")
                }

                isFolder -> {
                    PlayerActivity.pipStatus=1
                    sendIntent(holder.itemView.context,position, "FolderActivity")
                }

                MainActivity.search -> {
                    PlayerActivity.pipStatus=2
                    sendIntent(holder.itemView.context,position, "searchView")
                }

                else -> {
                    PlayerActivity.pipStatus= 3
                    sendIntent(holder.itemView.context,position, "AllVideos")
                }
            }

        }
    }

    private fun sendIntent(context: Context,position: Int, ref: String) {
        PlayerActivity.position=position
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("Class",ref)
        ContextCompat.startActivity(context, intent, null)
    }

   fun updateList(searchList : ArrayList<Video>){
        videoList= ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }
}