package com.shahbaz.videoplayer.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shahbaz.videoplayer.FolderVideoActivity
import com.shahbaz.videoplayer.databinding.FolderViewBinding
import com.shahbaz.videoplayer.dataclass.Folder


class FolderAdapter(val folderList:ArrayList<Folder>):RecyclerView.Adapter<FolderAdapter.viewholder>() {

    class viewholder (val binding: FolderViewBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(folder: Folder) {
        binding.apply {
            folderTitle.text=folder.folderName
        }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewholder {
        return viewholder(FolderViewBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
        return folderList.size
    }

    override fun onBindViewHolder(holder: viewholder, position: Int) {
        val folder = folderList[position]
        holder.bind(folder)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context,FolderVideoActivity::class.java)
            intent.putExtra("position",position)
            ContextCompat.startActivity(holder.itemView.context,intent,null)
        }
    }


}