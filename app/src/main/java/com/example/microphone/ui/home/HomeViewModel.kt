package com.example.microphone.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.microphone.models.Post
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

class HomeViewModel: ViewModel() {
    var postsMutableLifeData = MutableLiveData<List<Post>>()
    lateinit var postList : ArrayList<Post>

    fun getPostsFromDatabase(){
        postList= ArrayList()
        val reference = FirebaseDatabase.getInstance().getReference("Posts")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                postList?.clear()
                for (dataSnapshot1 in dataSnapshot.children) {
                    val post = dataSnapshot1.getValue<Post>()
                    postList.add(post!!)
                }
                postsMutableLifeData.value=postList
            }
            override fun onCancelled(databaseError: DatabaseError) {
               // Toast.makeText(, databaseError.message + "", Toast.LENGTH_SHORT).show()
            }
        })
    }
}