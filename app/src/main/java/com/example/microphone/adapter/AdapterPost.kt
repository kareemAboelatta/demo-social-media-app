package com.example.microphone.adapter

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.text.format.DateFormat
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.microphone.ui.home.PostDetailsActivity
import com.example.microphone.R
import com.example.microphone.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_post.view.*
import java.util.*

class AdapterPost(var posts: List<Post>, var context: Context): RecyclerView.Adapter<AdapterPost.PostViewHolder>() {

    var myUid: String? = null

    fun setList(posts: List<Post>){
        this.posts=posts
        notifyDataSetChanged()
    }
  public  var t1: TextToSpeech? = null
    var mProcessLike = false
    private var likesRef : DatabaseReference? = null
    private var postsRef : DatabaseReference? = null



    init {
        myUid = FirebaseAuth.getInstance().currentUser!!.uid
        likesRef = FirebaseDatabase.getInstance().reference.child("Likes")
        postsRef = FirebaseDatabase.getInstance().reference.child("Posts")



        t1 = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                t1!!.language = Locale.ENGLISH
            }
        }

    }

    inner class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)

        return  PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        var curPost=posts[position]

        val userId=curPost.userId
        val userName=curPost.userName
        val userEmail=curPost.userEmail
        val userImage=curPost.userImage

        val caption=curPost.caption
        val postId=curPost.postId
        val postImage=curPost.postImage
        val postTimestamp=curPost.postTime
        val postLikes=curPost.postLikes
        val postComments=curPost.postComments

        val cal = Calendar.getInstance(Locale.getDefault())
        if (postTimestamp != null) {
            cal.timeInMillis = postTimestamp.toLong()
        }
        val pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()


        holder.itemView.apply {
            uNameIv.text=userName
            uTimeIv.text=pTime
            pTitleIv.text=caption
            pLikes.text=""+postLikes+" like"
            pCommentTV.text=""+postComments+" comment"


            //set like for each post
            setLikes(holder, postId!!)


            //set user dp
            try {
                Picasso.get().load(userImage).into(uPictureIv)
            } catch (e: Exception) {
                Picasso.get().load(R.drawable.ic_profile)
                    .into(uPictureIv)
            }

            //post onwner image
            if (postImage.equals("noImage")) {
                //hide image view
                pImageIv.visibility=View.GONE

            } else {
                //hide image view
                pImageIv.visibility=View.VISIBLE
                try {
                    Picasso.get().load(postImage).into(pImageIv)
                } catch (e: Exception) {
                    pImageIv.visibility=View.GONE
                }
            }

            m_btn.setOnClickListener {
                showMoreOptions(m_btn, userId!!, postId!!, postImage!!)
            }

            like_btn.setOnClickListener {
                //get total number of likes for the post ,whose like Btn clicked
                //if currently signed in user has liked it before
                //increase value by 1 , otherwise decrease value by 1
                val postLikes: Int = posts[position].postLikes.toInt()
                mProcessLike = true
                //get id of the post clicked
                val postId: String? = posts[position].postId
                likesRef!!.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (mProcessLike) { //already liked ,so remove  like
                            mProcessLike = if (dataSnapshot.child(postId!!).hasChild(myUid!!)) {
                                postsRef!!.child(postId!!).child("postLikes")
                                    .setValue((postLikes - 1))
                                likesRef!!.child(postId!!).child(myUid!!).removeValue()
                                false
                            } else { //not liked , liked it
                                postsRef!!.child(postId!!).child("postLikes")
                                    .setValue((postLikes + 1))
                                likesRef!!.child(postId).child(myUid!!).setValue("Liked")
                                false
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
                notifyDataSetChanged()
            }


            read_btn.setOnClickListener {
                t1?.speak(caption,TextToSpeech.QUEUE_FLUSH,null)
            }


            comment_btn.setOnClickListener {
                //start post deatials acticty
                val intent = Intent(context, PostDetailsActivity::class.java)
                intent.putExtra("postId", postId) // we will get details of this post by using  this id ,its id of post clicked
                context.startActivity(intent)
            }



        }




    }





    private fun setLikes(holder1: PostViewHolder, postKey: String) {
        likesRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                holder1.itemView.apply {
                    if (dataSnapshot.child(postKey).hasChild(myUid!!)) {
                        //user has liked for this post
                        like_btn.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_like,
                            0,
                            0,
                            0
                        )
                        like_btn.setText("Liked")
                    } else {
                        //user has not liked for this post
                        like_btn.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_like_not,
                            0,
                            0,
                            0
                        )
                        like_btn.setText("Like")
                    }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    private fun showMoreOptions(moreBtn: ImageButton, uid: String, pId: String, pImage: String) {
        val popupMenu = PopupMenu(context, moreBtn, Gravity.END)
        if (uid == myUid) {
            popupMenu.menu.add(Menu.NONE, 0, 0, "Delete")
        }
        popupMenu.menu.add(Menu.NONE, 1, 0, "View details")
        popupMenu.setOnMenuItemClickListener { item ->
            val id = item.itemId
            if (id == 0) {
                //delete
                beginDelete(pId, pImage)
            } else if (id == 1) {
                //start post deatials acticty
                val intent = Intent(context, PostDetailsActivity::class.java)
                intent.putExtra("postId", pId) // we will get details of this post by using  this id ,its id of post clicked
                context.startActivity(intent)
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
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Deleting...")
        val fquery =FirebaseDatabase.getInstance().getReference("Posts").orderByChild("postId").equalTo(
            pId
        )
        fquery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnapshot1 in dataSnapshot.children) {
                    dataSnapshot1.ref.removeValue() // remove values from firebase where pid matches
                    progressDialog.dismiss()
                    Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Deleted Failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteWithImage(pId: String, pImage: String) {
        // Progress bar
        val progressDialog = ProgressDialog(context)
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
                        Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Deleted Failed", Toast.LENGTH_SHORT).show()
                }
            })
        }.addOnFailureListener { e -> //failed
            progressDialog.dismiss()
            Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
        }
    }


    override fun getItemCount(): Int {
        return posts.size
    }
}