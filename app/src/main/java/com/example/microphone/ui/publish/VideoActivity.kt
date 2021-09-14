package com.example.microphone.ui.publish

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.microphone.MainActivity
import com.example.microphone.R
import com.example.microphone.foruser.LoginActivity
import com.example.microphone.ui.home.HomeFragment
import com.example.microphone.models.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoActivity : AppCompatActivity() {

    val homeFragment = HomeFragment()
    var firebaseAuth = FirebaseAuth.getInstance()



    //permission constants
    private val CAMERA_REQUEST_CODE = 100
    private val STORAGE_REQUEST_CODE = 101
    //image pick constants
    private val VIDEO_PICK_CAMERA_CODE = 102
    private val VIDEO_PICK_GALLERY_CODE = 103
    //array of permission
    lateinit var  cameraPermissions : Array<String>
    lateinit var  storagePermissions : Array<String>


    var email: String? = null
    var name: String? = null
    var userImage: String? = null
    var userId: String? = null

    var videoUri: Uri? = null
    var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)


        cameraPermissions =arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        videoPickDialog()

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please Wait")
        progressDialog!!.setMessage("Uploading Video...")
        progressDialog!!.setCanceledOnTouchOutside(false)

        checkUserStatus()
        //to get the data about this user
        CoroutineScope(Dispatchers.IO).launch {
            val query: Query = FirebaseDatabase.getInstance().getReference("users").orderByChild("id").equalTo(userId)
            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataSnapshot1 in dataSnapshot.children) {
                        name = "" + dataSnapshot1.child("name").value
                        userImage = "" + dataSnapshot1.child("image").value
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(
                        this@VideoActivity,"" + databaseError.message,Toast.LENGTH_LONG)
                        .show()
                }
            })
        }





        btn_video_post.setOnClickListener {
            val caption=video_caption.text.toString()
            if (caption.isEmpty()){
                Toast.makeText(this@VideoActivity, "Enter Caption first", Toast.LENGTH_LONG)
            }else{
                uploadData(caption,videoUri.toString())
            }

        }

    }

    private fun uploadData(caption: String, uri: String) {
        progressDialog!!.show()

        while (name==null);

        CoroutineScope(Dispatchers.Default).launch {
            val timpeStamp = System.currentTimeMillis().toString()
            val filePathAndName = "Videos/Videos_$timpeStamp"

            val storageReference = FirebaseStorage.getInstance().reference.child(filePathAndName)
            storageReference.putFile(Uri.parse(uri)).addOnSuccessListener { taskSnapshot ->

                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);

                val downloadUri = uriTask.result.toString()

                if (uriTask.isSuccessful) {
                    val Video=Video(
                        userId,
                        name,
                        email,
                        userImage,
                        caption,
                        timpeStamp,
                        downloadUri,
                        timpeStamp,
                        0,
                        0 )
                    val ref = FirebaseDatabase.getInstance().getReference("Videos")
                    ref.child(timpeStamp).setValue(Video).addOnSuccessListener {
                        Toast.makeText(homeFragment.activity, "Video Published",Toast.LENGTH_SHORT).show()
                        progressDialog!!.dismiss()
                    }.addOnFailureListener { e ->
                        Toast.makeText(homeFragment.activity,e.localizedMessage + "",Toast.LENGTH_SHORT ).show()
                        progressDialog!!.dismiss()
                    }
                }
            }.addOnFailureListener {
                progressDialog!!.dismiss()
                Toast.makeText(applicationContext, "" + it.message, Toast.LENGTH_SHORT).show();
            }

        }

        closefragment()
    }


    private fun closefragment() {
        startActivity(Intent(this, MainActivity::class.java))
    }


    private fun videoPickDialog() {
        val options = arrayOf("Camera", "Gallery")
        //dialog
        val builder = AlertDialog.Builder(this)

        //title
        builder.setTitle("Pick Video From ?!")
        builder.setCancelable(false)
        builder.setItems(options) { dialog, which ->
            if (which == 0) {
                //camera clicked
                if (!checkCameraPremission()) {
                    requestCameraPermission()
                } else {
                    videoPickCamera()
                }
            } else if (which == 1) {
                // gallery clicked
                videoPickGallery()
            }
        }
        //create show dialog
        builder.create().show()
    }
    private fun videoPickGallery() {
        //pick from camera _ intent
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Videos"),
            VIDEO_PICK_GALLERY_CODE
        )
    }
    private  fun videoPickCamera() {
        //pick from camera _ intent
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, VIDEO_PICK_GALLERY_CODE)
    }
    private  fun requestCameraPermission() {
        //check if camera permission is enabled or not
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }
    private fun checkCameraPremission(): Boolean {
        //check if camera permission is enabled or not
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
        return result && result1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> if (grantResults.size > 0) {
                //check
                val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (cameraAccepted && storageAccepted) {
                    videoPickCamera()
                } else {
                    Toast.makeText(
                        this,
                        "Camera & Storage Permission are required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RESULT_OK) {
            videoUri = data!!.data
            //show picked video
            SetVideoToVideoView()
        } else if (requestCode ==  VIDEO_PICK_GALLERY_CODE) {
            videoUri = data!!.data
            //show picked video
            SetVideoToVideoView()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun SetVideoToVideoView() {
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        // set media controller to Video view
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(videoUri)
        videoView.requestFocus()
        videoView.setOnPreparedListener(MediaPlayer.OnPreparedListener {
            videoView.start()
            mediaController.show()
            mediaController.playSoundEffect(4)
        })
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
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}