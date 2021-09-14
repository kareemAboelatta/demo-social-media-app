package com.example.microphone.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    var userId:String?="",
    var userName:String?="",
    var userEmail:String?="",
    var userImage:String?="",

    var caption:String?="",
    var postId:String?="",
    var postImage:String?="",
    var postTime:String?="",
    var postLikes:Int=0,
    var postComments:Int=0
){
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "userImage" to userImage,

            "caption" to caption,
            "postId" to postId,
            "postImage" to postImage,
            "postTime" to postTime,
            "postLikes" to postLikes,
            "postComments" to postComments
        )
    }
}
