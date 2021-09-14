package com.example.microphone.ui.video

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.microphone.models.Post
import com.example.microphone.models.Video
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

class VideoViewModel : ViewModel() {
    var videosMutableLifeData = MutableLiveData<List<Video>>()
    lateinit var postList : ArrayList<Video>

    fun getPostsFromDatabase(){
        postList= ArrayList()
        val reference = FirebaseDatabase.getInstance().getReference("Videos")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                postList?.clear()
                for (dataSnapshot1 in dataSnapshot.children) {
                    val post = dataSnapshot1.getValue<Video>()
                    postList.add(post!!)
                }
                videosMutableLifeData.value=postList
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Toast.makeText(, databaseError.message + "", Toast.LENGTH_SHORT).show()
            }
        })
    }
}