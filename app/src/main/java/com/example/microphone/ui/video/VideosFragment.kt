package com.example.microphone.ui.video
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.microphone.R
import com.example.microphone.adapter.AdapterVideo
import com.example.microphone.models.Video
import kotlinx.android.synthetic.main.fragment_videos.view.*

class VideosFragment : Fragment() {

    lateinit var adapterVideo: AdapterVideo
    lateinit var videoList : ArrayList<Video>
    lateinit var videoViewModel: VideoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoList = ArrayList()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v= inflater.inflate(R.layout.fragment_videos, container, false)

        val layoutManager = LinearLayoutManager(activity)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        v.rec_video.layoutManager=layoutManager
        videoList=ArrayList()

        adapterVideo = activity?.let { AdapterVideo(videoList, it) }!!
        v.rec_video.adapter=adapterVideo

        videoViewModel= activity?.let { ViewModelProviders.of(it).get(VideoViewModel::class.java) }!!
        videoViewModel.getPostsFromDatabase()

        //get Posts from viewModel
        videoViewModel.videosMutableLifeData.observe(viewLifecycleOwner, Observer {mylist->
            videoList= mylist as ArrayList<Video>
            adapterVideo.setList(videoList)
        })


        return v
    }




}