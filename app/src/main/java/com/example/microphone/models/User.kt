package com.example.microphone.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var name:String?="",
    var bio:String?="",
    var email:String?="",
    var id:String?="",

    var image:String?="",
    var cover:String?="",

){
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "bio" to bio,
            "email" to email,
            "id" to id,

            "image" to image,
            "cover" to cover,

        )
    }
}
