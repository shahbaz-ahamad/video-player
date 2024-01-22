package com.shahbaz.videoplayer.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.shahbaz.videoplayer.MainActivity
import com.shahbaz.videoplayer.PlayerActivity
import com.shahbaz.videoplayer.R
import com.shahbaz.videoplayer.adapter.VideoAdapter
import com.shahbaz.videoplayer.databinding.FragmentAllVideosBinding
import com.shahbaz.videoplayer.helper.getAllVideos


class AllVideosFragment : Fragment() {
    private lateinit var binding : FragmentAllVideosBinding
    private lateinit var videoAdapter:VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireContext().theme.applyStyle(MainActivity.themeList[MainActivity.themeIndex],true)
        // Inflate the layout for this fragment
        binding= FragmentAllVideosBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoAdapter = VideoAdapter(MainActivity.videoList)
        setupRecyclerview()
        binding.tvTotalVideos.text="Total Video:${MainActivity.videoList.size.toString()}"

        binding.nowPlaying.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("Class","nowPlaying")
            startActivity(intent)
        }

        binding.root.setOnRefreshListener {

            MainActivity.videoList= getAllVideos(requireContext())
            videoAdapter.updateList(MainActivity.videoList)
            binding.tvTotalVideos.text="Total Video:${MainActivity.videoList.size.toString()}"
            binding.root.isRefreshing=false
        }


    }

    private fun setupRecyclerview() {
        binding.recyclerViewVideo.apply {
            layoutManager=LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            adapter=videoAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_view,menu)
        val searchView =menu.findItem(R.id.search_view)?.actionView as SearchView
        searchView.setOnQueryTextListener(object  : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText != null){
                    //Toast.makeText(requireContext(),newText.toString(),Toast.LENGTH_SHORT).show()
                    MainActivity.searchList = ArrayList()
                    for (video in MainActivity.videoList){
                        if(video.title.lowercase().contains(newText.lowercase())){
                            MainActivity.searchList.add(video)

                        }
                    }
                    MainActivity.search=true
                    videoAdapter.updateList(MainActivity.searchList)
                }
                return true
            }

        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onResume() {
        super.onResume()
        if(PlayerActivity.position != -1){
            binding.nowPlaying.visibility=View.VISIBLE
        }
        if(MainActivity.adapterChanged){
            videoAdapter.notifyDataSetChanged()
            MainActivity.adapterChanged=false
        }
    }
}