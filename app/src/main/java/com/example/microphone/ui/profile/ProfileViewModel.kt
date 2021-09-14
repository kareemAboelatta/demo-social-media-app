package com.example.microphone.ui.profile

import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.microphone.models.Post
import com.example.microphone.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue

class ProfileViewModel : ViewModel() {

    var postsMutableLifeData = MutableLiveData<List<Post>>()
    var userMutableLifeData = MutableLiveData<User>()
    lateinit var postList : ArrayList<Post>
    lateinit var user:User

    fun getPostsFromDatabase(){
        postList= ArrayList()
        val reference = FirebaseDatabase.getInstance().getReference("Posts")
        val query: Query = reference.orderByChild("userId").equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                postList?.clear()
                for (dataSnapshot1 in dataSnapshot.children) {
                    val post = dataSnapshot1.getValue<Post>()
                    postList.add(post!!)
                }
                postsMutableLifeData.value=postList
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
    fun getDataForThisUser(){
        val query: Query = FirebaseDatabase.getInstance().getReference("users").orderByChild("id").equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnapshot1 in dataSnapshot.children) {
                    user = dataSnapshot1.getValue<User>()!!
                }
                userMutableLifeData.value=user
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }
}