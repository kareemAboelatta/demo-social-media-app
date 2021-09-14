package com.example.microphone.adapter

import android.app.AlertDialog
import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.microphone.R
import com.example.microphone.models.Comment
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_comment.view.*
import java.util.*

class AdapterComment(
    var list: List<Comment>,
    var context: Context,
    var myUserId: String,
    var postId: String
) : RecyclerView.Adapter<AdapterComment.CommentViewHolder>(){

    inner class CommentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val curComment=list[position]

        val cal = Calendar.getInstance(Locale.getDefault())
        if (curComment.timeStamp != null) {
            cal.timeInMillis = curComment.timeStamp!!.toLong()
        }
        val commentTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()


        holder.itemView.apply {
            item_comment.text=curComment.comment
            item_comment_nameTv.text=curComment.userName
            item_comment_timeTv.text=commentTime


            try {
                Picasso.get().load(curComment.userImage).placeholder(R.drawable.ic_profile).into(
                    item_comment_avatarTv
                )
            } catch (e: Exception) {
                Picasso.get().load(R.drawable.ic_profile).into(item_comment_avatarTv)
            }

            item_comment_long_click.setOnLongClickListener {
                if (myUserId == curComment.userId) {
                    //my comment
                    //show delete dialog
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Delete")
                    builder.setMessage("are you sure to delete this comment")
                    builder.setPositiveButton("Delete") { dialog, which ->
                        deleteComment(curComment.commentId)

                    }
                    builder.setNegativeButton(
                        "Cancel") { dialog, which ->
                        dialog.dismiss()
                    }
                    builder.create().show()
                } else {
                    Toast.makeText(context, "Can't delete others's comments.", Toast.LENGTH_SHORT)
                        .show()
                }
                false
            }
        }

    }

    private fun deleteComment(commentId: String?) {
        val ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId)
        ref.child("Comments").child(commentId!!).removeValue() // it will delete the comment

        //now update the comment count
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val comments = "" + dataSnapshot.child("postComments").value
                val newCommentVal = comments.toInt() - 1
                ref.child("postComments").setValue(newCommentVal)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        Toast.makeText(context, "Comment deleted..", Toast.LENGTH_SHORT).show()

    }

    override fun getItemCount(): Int {
        return  list.size
    }


}