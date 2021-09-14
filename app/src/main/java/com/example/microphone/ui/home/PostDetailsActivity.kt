package com.example.microphone.ui.home

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.microphone.R
import com.example.microphone.adapter.AdapterComment
import com.example.microphone.foruser.LoginActivity
import com.example.microphone.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_post_details.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import java.util.*
import kotlin.collections.ArrayList

class PostDetailsActivity : AppCompatActivity() {

    //to get details of user and post
    var myUserId: String? = null
    var postImage: String? = null
    var hisUserId: String? = null
    var myEmail: String? = null
    var myName: String? = null
    var myImage: String? = null
    var postId: String? = null
    var postLikes: String? = null
    var hisImage: String? = null
    var hisName: String? = null

    var mProcessComment = false
    var mProcessLike = false
    var progressDialog: ProgressDialog? = null

    var t1: TextToSpeech? = null

    lateinit var commentList : ArrayList<Comment>
    lateinit var adapter: AdapterComment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_details)
        val intent = intent
        postId = intent.getStringExtra("postId")

        checkUserStatus()

        commentList= ArrayList()

        t1 = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                t1!!.language = Locale.ENGLISH
            }
        }

        det_btn_like.setOnClickListener {
            likePost()
        }

        det_btn_comment.setOnClickListener {
            postComment()
        }


        loadPostInfo()
        loadUserInfo()
        setLikes()

        loadComments()




    }

    private fun loadComments() {
        //layout (linear) for recycleview

        val layoutManager = LinearLayoutManager(this)

        rec_comments.layoutManager=layoutManager

        //path of the post ,to get it's comments
        val ref =FirebaseDatabase.getInstance().getReference("Posts").child(postId!!).child("Comments")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                commentList.clear()
                for (dataSnapshot1 in dataSnapshot.children) {
                    val comment=dataSnapshot1.getValue<Comment>()
                    commentList.add(comment!!)

                    //pass myUid  and postId as parameter of constructor of comment adapter

                    //setup adapter
                    adapter = AdapterComment( commentList,applicationContext, myUserId!!, postId!!)
                    rec_comments.setAdapter(adapter)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }


    private fun postComment() {
        progressDialog = ProgressDialog(this@PostDetailsActivity)
        progressDialog!!.setMessage("Adding Comment ....")

        //get data from comment edit text

        val comment= det_commentEt.text.toString()
        //validate
        if (TextUtils.isEmpty(comment)) {
            Toast.makeText(this@PostDetailsActivity, "Comment is Empty...", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val timeStamp = System.currentTimeMillis().toString()
        //each post will have a child "Comments " tha will conten comments of the post
        val ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId!!).child("Comments")

        val myComment=Comment(
            timeStamp,
            comment,
            timeStamp,
            myUserId!!,
            myEmail!!,
            myImage!!,
            myName!!
        )

        //put this data in DB :
        ref.child(timeStamp).setValue(myComment).addOnSuccessListener { // added
            progressDialog!!.dismiss()
            Toast.makeText(this@PostDetailsActivity, "Comment added", Toast.LENGTH_SHORT).show()
            det_commentEt.setText("")
            mProcessComment = true
            updateCommentCount()
        }.addOnFailureListener { e -> //failed
            progressDialog!!.dismiss()
            Toast.makeText(this@PostDetailsActivity, "" + e.message, Toast.LENGTH_LONG).show()
        }

    }
    private fun updateCommentCount() {

        //whenever user adds comments increase the comments counts as we did for like count
        mProcessComment = true
        val ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId!!)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (mProcessComment) {
                    val comments = "" + dataSnapshot.child("postComments").value
                    val newCommentVal = comments.toInt() + 1
                    ref.child("postComments").setValue(newCommentVal)
                    mProcessComment = false
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun likePost() {

        //get total number of likes for the post ,whose like Btn clicked
        //if currently signed in user has liked it before
        //increase value by 1 , otherwise decrease value by 1
        mProcessLike = true
        //get id of the post clicked

        //get id of the post clicked
        val likesRef = FirebaseDatabase.getInstance().reference.child("Likes")
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")
        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (mProcessLike) {
                    mProcessLike = if (dataSnapshot.child(postId!!).hasChild(myUserId!!)) {
                        //already liked ,so remove  like
                        postsRef.child(postId!!).child("postLikes")
                            .setValue((postLikes!!.toInt() - 1))
                        likesRef.child(postId!!).child(myUserId!!).removeValue()
                        false

                        //   likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.like,0,0,0);
                        //     likeBtn.setText("Like");
                    } else {
                        //not liked , liked it
                        postsRef.child(postId!!).child("postLikes")
                            .setValue((postLikes!!.toInt() + 1))
                        likesRef.child(postId!!).child(myUserId!!).setValue("Liked")
                        false
                        // likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.like_done,0,0,0);
                        // likeBtn.setText("Liked");
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    private fun setLikes() {
        val likesRef = FirebaseDatabase.getInstance().reference.child("Likes")

        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.child(postId!!).hasChild(myUserId!!)) {
                    //user has liked for this post
                    det_btn_like.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_like,
                        0,
                        0,
                        0
                    )
                    det_btn_like.setText("Liked")
                } else {
                    //user has not liked for this post
                    det_btn_like.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_like_not, 0, 0, 0
                    )
                    det_btn_like.setText("Like")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    private fun loadPostInfo() {

        //get post using id
        val ref = FirebaseDatabase.getInstance().getReference("Posts")
        val query = ref.orderByChild("postId").equalTo(postId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                //keep checking the posts until get the required post
                for (dataSnapshot1 in dataSnapshot.children) {
                    val caption = "" + dataSnapshot1.child("caption").value
                    postLikes = "" + dataSnapshot1.child("postLikes").value
                    val pTimeStamp = "" + dataSnapshot1.child("postTime").value
                    postImage = "" + dataSnapshot1.child("postImage").value
                    hisImage = "" + dataSnapshot1.child("userImage").value
                    hisUserId = "" + dataSnapshot.child("userId").value
                    val userEmail = "" + dataSnapshot1.child("userEmail").value
                    hisName = "" + dataSnapshot1.child("userName").value
                    val commentCount = "" + dataSnapshot1.child("postComments").value

                    //convert tempStamp to proper   format
                    val cal = Calendar.getInstance(Locale.getDefault())
                    cal.timeInMillis = pTimeStamp.toLong()
                    val postTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()

                    det_pTitleIv.text = caption
                    det_pLikes.text = postLikes + " Likes"
                    det_pTimeIv.text = postTime
                    det_pCommentTV.text = "$commentCount Comments"
                    det_uNameIv.text = hisName


                    det_btn_read.setOnClickListener {
                        t1?.speak(caption, TextToSpeech.QUEUE_FLUSH, null)
                    }

                    //set image of user who posted this post
                    if (postImage.equals("noImage")) {
                        //hide image view
                        det_pImageIv.visibility = View.GONE
                    } else {
                        //hide image view
                        det_pImageIv.visibility = View.VISIBLE
                        try {
                            Picasso.get().load(postImage).into(det_pImageIv)
                        } catch (e: java.lang.Exception) {
                        }
                    }
                    //set user image in comment
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_profile)
                            .into(det_userPictureIv)
                    } catch (e: java.lang.Exception) {
                        Picasso.get().load(R.drawable.ic_profile).into(det_userPictureIv)
                    }

                    det_more_btn.setOnClickListener {
                        showMoreOptions(det_more_btn, hisUserId!!, postId!!, postImage!!)
                    }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun loadUserInfo() {
        //get current user info
/*        val myRef: Query = FirebaseDatabase.getInstance().getReference("users")
        myRef.orderByChild("userId").equalTo(myUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataSnapshot1 in dataSnapshot.children) {
                        myName = "" + dataSnapshot1.child("name").value
                        myImage = "" + dataSnapshot1.child("image").value
                        try {
                            Picasso.get().load(myImage).into(det_cAvatarTv)
                        } catch (e: Exception) {
                            Picasso.get().load(R.drawable.ic_profile).into(det_cAvatarTv)
                        }
                    }
                    Toast.makeText(this@PostDetailsActivity,"You Comment by: " + myEmail,Toast.LENGTH_LONG ).show()
                    det_commentEt.setText(myUserId)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })*/

        val query: Query = FirebaseDatabase.getInstance().getReference("users").orderByChild("id").equalTo(myUserId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnapshot1 in dataSnapshot.children) {
                    myName = "" + dataSnapshot1.child("name").value
                    myImage = "" + dataSnapshot1.child("image").value
                }
                try {
                    Picasso.get().load(myImage).into(det_cAvatarTv)
                } catch (e: Exception) {
                    Picasso.get().load(R.drawable.ic_profile).into(det_cAvatarTv)
                }
                Toast.makeText(this@PostDetailsActivity,"You Comment by: " + myName,Toast.LENGTH_LONG ).show()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@PostDetailsActivity, "" + databaseError.message, Toast.LENGTH_LONG)
                    .show()

            }
        })


    }

    private fun showMoreOptions(moreBtn: ImageButton, uid: String, pId: String, pImage: String) {
        val popupMenu = PopupMenu(this, moreBtn, Gravity.END)
        if (hisUserId == myUserId) {
            popupMenu.menu.add(Menu.NONE, 0, 0, "Delete")
        }
        popupMenu.menu.add(Menu.NONE, 1, 0, "View details")
        popupMenu.setOnMenuItemClickListener { item ->
            val id = item.itemId
            if (id == 0) {
                //delete
                beginDelete(pId,pImage)
            } else if (id == 1) {
                //start post detials acticty
                val intent = Intent(this, PostDetailsActivity::class.java)
                intent.putExtra("postId", pId) // we will get details of this post by using  this id ,its id of post clicked
                startActivity(intent)
            }
            false
        }
        popupMenu.show()
    }


    private fun beginDelete(pId: String, pImage: String) {
        if (pImage == "noImage") {
            deleteWithoutImage(pId)
        } else {
            deleteWithImage(pId, pImage)
        }
    }

    private fun deleteWithoutImage(pId: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Deleting...")
        val fquery =FirebaseDatabase.getInstance().getReference("Posts").orderByChild("postId").equalTo(
            pId
        )
        fquery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnapshot1 in dataSnapshot.children) {
                    dataSnapshot1.ref.removeValue() // remove values from firebase where pid matches
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "Deleted Successfully", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, "Deleted Failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteWithImage(pId: String, pImage: String) {
        // Progress bar
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Deleting...")

        /*steps :
         * delete Image using  url
         *Delete from database using id post
         *
         * */
        val picStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pImage)
        picStorageReference.delete().addOnSuccessListener {
            // image deleted ,not delete from database
            val fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("postId").equalTo(
                pId
            )
            fquery.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataSnapshot1 in dataSnapshot.children) {
                        dataSnapshot1.ref.removeValue() // remove values from firebase where pid matches
                        progressDialog.dismiss()
                        Toast.makeText(
                            applicationContext,
                            "Deleted Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(applicationContext, "Deleted Failed", Toast.LENGTH_SHORT).show()
                }
            })
        }.addOnFailureListener { e -> //failed
            progressDialog.dismiss()
            Toast.makeText(applicationContext, "" + e.message, Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        t1?.stop()
        t1?.shutdown()
    }

    override fun onStart() {
        super.onStart()
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            myEmail = user.email
            myUserId = user.uid
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}