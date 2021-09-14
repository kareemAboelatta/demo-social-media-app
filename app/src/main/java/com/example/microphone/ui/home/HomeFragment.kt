package com.example.microphone.ui.home
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.microphone.adapter.AdapterPost
import com.example.microphone.R
import com.example.microphone.models.Post
import kotlinx.android.synthetic.main.fragment_home.view.*
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    lateinit var adapterPosts: AdapterPost
    lateinit var postList : ArrayList<Post>
    lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postList= ArrayList()

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v= inflater.inflate(R.layout.fragment_home, container, false)

        val layoutManager = LinearLayoutManager(activity)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        v.rec_home.layoutManager=layoutManager
        postList=ArrayList()

        adapterPosts = activity?.let { AdapterPost(postList, it) }!!
        v.rec_home.adapter=adapterPosts

        homeViewModel= activity?.let { ViewModelProviders.of(it).get(HomeViewModel::class.java) }!!
        homeViewModel.getPostsFromDatabase()

        //get Posts from viewModel
        homeViewModel.postsMutableLifeData.observe(viewLifecycleOwner, Observer {mylist->
            postList= mylist as ArrayList<Post>
            adapterPosts.setList(postList)
        })
        return v
    }
}