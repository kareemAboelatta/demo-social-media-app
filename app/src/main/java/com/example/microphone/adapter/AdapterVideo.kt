package com.example.microphone.adapter

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.text.format.DateFormat
import android.view.*
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.microphone.ui.home.PostDetailsActivity
import com.example.microphone.R
import com.example.microphone.models.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_post.view.*
import kotlinx.android.synthetic.main.item_video.view.*
import java.util.*

class AdapterVideo(var posts: List<Video>, var context: Context): RecyclerView.Adapter<AdapterVideo.VideoViewHolder>() {

    var myUid: String? = null

    public  var t1: TextToSpeech? = null
    var mProcessLike = false
    private var likesRef : DatabaseReference? = null
    private var postsRef : DatabaseReference? = null

    fun setList(posts: List<Video>){
        this.posts=posts
        notifyDataSetChanged()
    }


    init {
        myUid = FirebaseAuth.getInstance().currentUser!!.uid
        likesRef = FirebaseDatabase.getInstance().reference.child("Likes")
        postsRef = FirebaseDatabase.getInstance().reference.child("Videos")



        t1 = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                t1!!.language = Locale.ENGLISH
            }
        }

    }

    inner class VideoViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)

        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        var curPost=posts[position]

        val userId=curPost.userId
        val userName=curPost.userName
        val userEmail=curPost.userEmail
        val userImage=curPost.userImage

        val caption=curPost.caption
        val postId=curPost.postId
        val postVideo=curPost.postVideo
        val postTimestamp=curPost.postTime
        val postLikes=curPost.postLikes
        val postComments=curPost.postComments

        val cal = Calendar.getInstance(Locale.getDefault())
        if (postTimestamp != null) {
            cal.timeInMillis = postTimestamp.toLong()
        }
        val pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()


        holder.itemView.apply {

            item_video_uNameIv.text=userName
            item_video_uTimeIv.text=pTime
            item_video_pTitleIv.text=caption
            item_video_pLikes.text=""+postLikes+" like"
            item_video_pCommentTV.text=""+postComments+" comment"


            //set like for each post
            setLikes(holder, postId!!)

            

            //set user dp
            try {
                Picasso.get().load(userImage).into(item_video_uPictureIv)
            } catch (e: Exception) {
                Picasso.get().load(R.drawable.ic_profile)
                    .into(item_video_uPictureIv)
            }



            item_video_m_btn.setOnClickListener {
                showMoreOptions(item_video_m_btn, userId!!, postId!!, postVideo!!)
            }

            item_video_like_btn.setOnClickListener {
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


            setVideoUrl(curPost,holder)

            item_video_read_btn.setOnClickListener {
                Toast.makeText(context,"Not work yet in video it's work in posts only now",Toast.LENGTH_LONG).show()
            }




            item_video_comment_btn.setOnClickListener {
                //start post deatials acticty
/*                val intent = Intent(context, PostDetailsActivity::class.java)
                intent.putExtra("postId", postId) // we will get details of this post by using  this id ,its id of post clicked
                context.startActivity(intent)*/

                Toast.makeText(context,"Not work yet in video it's work in posts only now",Toast.LENGTH_LONG).show()

            }




        }




    }


    private fun setVideoUrl(
        video: Video,
        holder: AdapterVideo.VideoViewHolder
    ) {
        //show progressBar
        holder.itemView.apply { 
            item_video_Progress.visibility=View.VISIBLE
            val videoUrl: String? = video.postVideo
            //MediaController for play , pause ,seekBar ,time  ,etc
            val mediaController = MediaController(context)
            mediaController.setAnchorView(item_video_frame)
            val videoUri = Uri.parse(videoUrl)
            item_video_rowV_video.setMediaController(mediaController)
            item_video_rowV_video.setVideoURI(videoUri)
            item_video_rowV_video.requestFocus()
            item_video_rowV_video.fitsSystemWindows
            item_video_rowV_video.setOnPreparedListener { mediaPlayer ->
                //Video is ready to play
                item_video_Progress.visibility=View.GONE
                item_video_play.visibility=View.VISIBLE
                item_video_play.setOnClickListener {
                    mediaPlayer.start()
                    item_video_play.visibility=View.GONE
                }

            }


            item_video_rowV_video.setOnInfoListener(MediaPlayer.OnInfoListener { mp, what, extra -> //to check if buffering ,rendering etc
                when (what) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {

                        //rendering started
                        item_video_Progress.visibility=View.GONE
                        return@OnInfoListener true
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {

                        //buffering started
                        item_video_Progress.visibility=View.GONE
                        return@OnInfoListener true
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {

                        // buffering end
                        item_video_Progress.visibility=View.GONE
                        return@OnInfoListener true
                    }
                }
                false
            })
            item_video_rowV_video.setOnCompletionListener(OnCompletionListener { mediaPlayer ->
                mediaPlayer.start() //restart video if completed
            })


        }





    }


    private fun setLikes(holder1: VideoViewHolder, postKey: String) {
        likesRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                holder1.itemView.apply {
                    if (dataSnapshot.child(postKey).hasChild(myUid!!)) {
                        //user has liked for this post
                        item_video_like_btn.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_like,
                            0,
                            0,
                            0
                        )
                        item_video_like_btn.text="Liked"
                    } else {
                        //user has not liked for this post
                        item_video_like_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_not,0,0,0)
                        item_video_like_btn.text="Like"
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
        val fquery = FirebaseDatabase.getInstance().getReference("Videos").orderByChild("postId").equalTo(
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
            val fquery = FirebaseDatabase.getInstance().getReference("Videos").orderByChild("postId").equalTo(
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