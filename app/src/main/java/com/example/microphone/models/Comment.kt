package com.example.microphone.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Comment(
    var commentId:String?="",
    var comment:String?="",
    var timeStamp:String?="",
    var userId:String?="",
    var userEmail:String?="",
    var userImage:String?="",
    var userName:String?=""

) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "commentId" to commentId,
            "comment" to comment,
            "timeStamp" to timeStamp,
            "userId" to userId,
            "userEmail" to userEmail,
            "userImage" to userImage,
            "userName" to userName
            )
    }
}