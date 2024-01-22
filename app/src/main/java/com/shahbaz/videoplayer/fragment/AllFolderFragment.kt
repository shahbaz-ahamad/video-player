package com.shahbaz.videoplayer.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.shahbaz.videoplayer.MainActivity
import com.shahbaz.videoplayer.R
import com.shahbaz.videoplayer.adapter.FolderAdapter
import com.shahbaz.videoplayer.databinding.FragmentAllFolderBinding

class AllFolderFragment : Fragment() {
    private lateinit var binding: FragmentAllFolderBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireContext().theme.applyStyle(MainActivity.themeList[MainActivity.themeIndex],true)
        // Inflate the layout for this fragment
        binding= FragmentAllFolderBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerview()

        binding.tvTotalFolder.text="Total Folder:${MainActivity.folderList.size}"
    }

    private fun setupRecyclerview() {
        binding.recyclerViewFolder.apply {
            setHasFixedSize(true)
            layoutManager=LinearLayoutManager(requireContext())
            setItemViewCacheSize(10)
            adapter=FolderAdapter(MainActivity.folderList)
        }
    }

}