package com.example.microphone.ui.publish

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.microphone.R
import com.example.microphone.foruser.LoginActivity
import com.example.microphone.models.Post
import com.example.microphone.ui.home.HomeFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import kotlinx.android.synthetic.main.fragment_post.*
import kotlinx.android.synthetic.main.fragment_post.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class PostFragment : Fragment() {

    val homeFragment = HomeFragment()

    var firebaseAuth = FirebaseAuth.getInstance()

    private val REQUEST_CODE_SPEECH_INPUT = 100
    var IMAGE_REQUEST=0
    var email: String? = null
    var name: String? = null
    var userImage: String? = null
    var userId: String? = null

    var uriImage: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkUserStatus()

        CoroutineScope(Dispatchers.IO).launch {
            val query: Query = FirebaseDatabase.getInstance().getReference("users").orderByChild("id").equalTo(
                userId
            )
            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataSnapshot1 in dataSnapshot.children) {
                        name = "" + dataSnapshot1.child("name").value
                        userImage = "" + dataSnapshot1.child("image").value
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(activity, "" + databaseError.message, Toast.LENGTH_LONG)
                        .show()
                }
            })
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var v= inflater.inflate(R.layout.fragment_post, container, false)


        v.btn_Video.setOnClickListener {
            startActivity(Intent(activity, VideoActivity::class.java))
        }

        v.btn_addPhoto.setOnClickListener {
            requestPermission()
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type="image/*"
                startActivityForResult(it, IMAGE_REQUEST)
            }
        }


        v.btn_post.setOnClickListener {
            var caption=v.ed_caption.text.toString()
            if (caption.isEmpty()){
                Toast.makeText(activity, "Enter caption", Toast.LENGTH_SHORT).show()
                return@setOnClickListener;
            }
            if (uriImage ==null){
                //without image
                uploadData(caption, "noImage")
            }else{
                //with image
                uploadData(caption, uriImage.toString())
            }

        }


        v.post_mic.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                Toast.makeText(activity, " " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        return v
    }


    private fun uploadData(caption: String, uri: String) {

        while (name==null);

        CoroutineScope(Dispatchers.Default).launch {
            val timpeStamp = System.currentTimeMillis().toString()
            val filePathAndName = "Posts/Posts_$timpeStamp"
            if (uri != "noImage") {
                val storageReference = FirebaseStorage.getInstance().reference.child(filePathAndName)
                storageReference.putFile(Uri.parse(uri)).addOnSuccessListener { taskSnapshot ->
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val downloadUri = uriTask.result.toString()
                    if (uriTask.isSuccessful) {
                        val post=Post(
                            userId,
                            name,
                            email,
                            userImage,
                            caption,
                            timpeStamp,
                            downloadUri,
                            timpeStamp,
                            0,
                            0
                        )
                        val ref = FirebaseDatabase.getInstance().getReference("Posts")
                        ref.child(timpeStamp).setValue(post).addOnSuccessListener {
                            Toast.makeText(
                                homeFragment.activity,
                                "Post Published",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener { e ->
                            Toast.makeText(
                                homeFragment.activity,
                                e.localizedMessage + "",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(activity, "" + it.message, Toast.LENGTH_SHORT).show();
                }
            } else {
                val post=Post(
                    userId,
                    name,
                    email,
                    userImage,
                    caption,
                    timpeStamp,
                    "noImage",
                    timpeStamp,
                    0,
                    0
                )
                val ref = FirebaseDatabase.getInstance().getReference("Posts")
                ref.child(timpeStamp).setValue(post).addOnSuccessListener {
                    Toast.makeText(homeFragment.activity, "Post Published", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        homeFragment.activity,
                        e.localizedMessage + "",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        closefragment()

    }

    private fun closefragment() {
        activity?.findViewById<ChipNavigationBar>(R.id.bottom_menu)?.setItemSelected(R.id.home)
        val ft1 = fragmentManager?.beginTransaction()
        ft1?.replace(R.id.conten, homeFragment, "")
        ft1?.commit()
    }

    private fun hasReadExternalStoragePermission()= activity?.let {
        ActivityCompat.checkSelfPermission(
            it,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } ==PackageManager.PERMISSION_GRANTED


    private fun requestPermission(){
        var permissionsToRequest= mutableListOf<String>()
        if (!hasReadExternalStoragePermission()){
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionsToRequest.isNotEmpty()){
            activity?.let { ActivityCompat.requestPermissions(
                it,
                permissionsToRequest.toTypedArray(),
                0
            ) }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==0  && grantResults.isNotEmpty()){
            for (i in grantResults.indices){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    Log.d("PermissionRequest", "${permissions[i]} granted.")
                }

            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK && requestCode ==IMAGE_REQUEST){

            post_image.visibility=View.VISIBLE
            uriImage=data?.data
            post_image.setImageURI(uriImage)

        }

        if (requestCode === REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode === Activity.RESULT_OK && data != null) {
                val result: ArrayList<String> = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                ed_caption.setText(Objects.requireNonNull(result)[0])
            }
        }


    }

    override fun onStart() {
        super.onStart()
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            email = user.email
            userId = user.uid
        } else {
            activity?.startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()

        }
    }

}