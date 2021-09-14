package com.example.microphone.ui.profile
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.microphone.MainActivity
import com.example.microphone.R
import com.example.microphone.adapter.AdapterPost
import com.example.microphone.foruser.LoginActivity
import com.example.microphone.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.view.*

class ProfileFragment : Fragment() {

    var firebaseAuth = FirebaseAuth.getInstance()
    val ref = Firebase.database.reference

    var IMAGE_REQUEST=0
    var Cover_REQUEST=1
    var email: String? = null
    var name: String? = null
    var bio: String? = null
    var userId: String? = null

    var uriImage: Uri? = null
    var uriImageCover: Uri? = null

    lateinit var prog: ProgressDialog
    lateinit var adapterPosts: AdapterPost
    lateinit var postList : ArrayList<Post>

    lateinit var profileViewModel:ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkUserStatus()

        postList= ArrayList()
        prog= ProgressDialog(activity)
        prog.setMessage("Wait..")
        prog.setCancelable(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v=inflater.inflate(R.layout.fragment_profile, container, false)
        //views
        v.prof_btn_change_profile.setOnClickListener {
            showAlertDialogForChangePhotos("profile")
        }
        v.prof_btn_change_cover.setOnClickListener {
            showAlertDialogForChangePhotos("cover")
        }
        v.prof_btn_edit_pen.setOnClickListener {
            var popupMenu=PopupMenu(activity, v.prof_btn_edit_pen)
            popupMenu.menuInflater.inflate(R.menu.pop_menu, popupMenu.menu)
            popupMenu.menu.removeItem(R.id.logout)
            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.change_name ->
                        showUpdateNameBioDialog("name")
                    R.id.change_bio ->
                        showUpdateNameBioDialog("bio")
                }
                true
            })
            popupMenu.show()
        }
        v.prof_btn_setting.setOnClickListener {
            var popupMenu=PopupMenu(activity, v.prof_btn_setting)
            popupMenu.menuInflater.inflate(R.menu.pop_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.change_name ->
                        showUpdateNameBioDialog("name")
                    R.id.change_bio ->
                        showUpdateNameBioDialog("bio")
                    R.id.logout -> {
                        firebaseAuth.signOut()
                        startActivity(Intent(activity, LoginActivity::class.java))
                    }
                }
                true
            })
            popupMenu.show()
        }

        //put linearLayout in recycle
        val linearLayout = LinearLayoutManager(activity)
        linearLayout.stackFromEnd = true
        linearLayout.reverseLayout = true
        v.prof_rec.layoutManager=linearLayout
        adapterPosts = activity?.let { AdapterPost(postList, it) }!!
        v.prof_rec.adapter=adapterPosts

        profileViewModel= activity?.let { ViewModelProviders.of(it).get(ProfileViewModel::class.java) }!!
        profileViewModel.getPostsFromDatabase()
        profileViewModel.getDataForThisUser()

        //observe Posts from viewModel
        profileViewModel.postsMutableLifeData.observe(viewLifecycleOwner, Observer {mylist->
            postList= mylist as ArrayList<Post>
            adapterPosts.setList(postList)
        })
        //observe data for this user from viewModel
        profileViewModel.userMutableLifeData.observe(viewLifecycleOwner, Observer {user->
            v.prof_name.text = user.name
            v.prof_bio.text = user.bio
            name=user.name
            bio= user.bio
            try {
                Picasso.get().load(user.image).into(v.prof_image_profile)
            } catch (e: Exception) {
                Picasso.get().load(R.drawable.ic_profile)
                    .into(v.prof_image_profile)
            }
            try {
                if (user.cover!!.isEmpty()) {
                    v.prof_image_cover.setImageResource(R.drawable.ic_cover)
                } else {
                    Picasso.get().load(user.cover).into(v.prof_image_cover)
                    v.prof_image_cover.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            } catch (e: Exception) {
                v.prof_image_cover.setImageResource(R.drawable.ic_cover)
            }
            prog.dismiss()
        })


        return v
    }


    private fun showUpdateNameBioDialog(key: String){
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Update $key")
        builder.setCancelable(false)
        val linearLayout = LinearLayout(activity)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(10, 10, 10, 10)


        val editText = EditText(activity)
        if (key.equals("name")){
            editText.hint = ""+name
        }else if(key.equals("bio")){
            editText.hint=""+bio
        }
        editText.setHintTextColor(resources.getColor(R.color.colorGray))
        linearLayout.addView(editText)
        builder.setView(linearLayout)


        builder.setPositiveButton("Update") { dialogInterface, which ->
            val value=editText.text.toString()
            if (value==null){
                Toast.makeText(activity, "Where's Your new ${key}", Toast.LENGTH_SHORT).show()
            }else{
                //update key
                ref.child("users").child(userId!!).child("" + key).setValue(value)
            }


        }

        builder.setNegativeButton("Cancel") { dialogInterface, which ->


        }

        val myAlertDialog: AlertDialog = builder.create()
        myAlertDialog.show()

    }
    private fun showAlertDialogForChangePhotos(type: String){
        val alertDialog:AlertDialog.Builder = AlertDialog.Builder(activity)
        // Setting Alert Dialog Title
        alertDialog.setTitle("Are you sure,You want change ${type} photo?")
        // Icon Of Alert Dialog
        alertDialog.setIcon(R.drawable.ic_warning)
        alertDialog.setCancelable(false)

        if (type.equals("profile")){
            alertDialog.setPositiveButton("Yes") { dialogInterface, which ->
                requestPermission()
                changeProfilePhoto()
            }
        }else{
            alertDialog.setPositiveButton("Yes") { dialogInterface, which ->
                requestPermission()
                changeCoverPhoto()
            }
        }
        alertDialog.setNeutralButton("Cancel"){ dialogInterface, which ->
        }
        val myAlertDialog: AlertDialog = alertDialog.create()
        myAlertDialog.show()
    }
    private fun changeProfilePhoto() {
        Intent(Intent.ACTION_GET_CONTENT).also {
            it.type="image/*"
            startActivityForResult(it, IMAGE_REQUEST)
        }
    }
    private fun changeCoverPhoto() {
        Intent(Intent.ACTION_GET_CONTENT).also {
            it.type="image/*"
            startActivityForResult(it, Cover_REQUEST)
        }
    }
    private fun hasReadExternalStoragePermission()= activity?.let {
        ActivityCompat.checkSelfPermission(it, Manifest.permission.READ_EXTERNAL_STORAGE)} == PackageManager.PERMISSION_GRANTED
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
        //profile
        if (resultCode== Activity.RESULT_OK && requestCode == IMAGE_REQUEST){
            uriImage=data?.data
            prog.show()

            uriImage?.let {
                val refy= FirebaseStorage.getInstance().reference.child("images").child(userId!!)
                refy.putFile(it).addOnSuccessListener {
                    var uriTask=it.getStorage().getDownloadUrl()
                    while (!uriTask.isSuccessful);

                    val downloadUri = uriTask.result.toString()
                    if (uriTask.isSuccessful){
                        ref.child("users").child(userId!!).child("image").setValue(downloadUri)
                        prog.dismiss()
                    }
                }.addOnFailureListener {
                    prog.dismiss()
                    Toast.makeText(activity, "" + it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        //cover
        if (resultCode== Activity.RESULT_OK && requestCode == Cover_REQUEST){
            uriImageCover=data?.data
            prog.show()
            uriImageCover?.let {
                val refy= FirebaseStorage.getInstance().reference.child("covers").child(userId!!)
                refy.putFile(it).addOnSuccessListener {
                    var uriTask=it.getStorage().getDownloadUrl()
                    while (!uriTask.isSuccessful);
                    val downloadUri = uriTask.result.toString()

                    if (uriTask.isSuccessful){
                        ref.child("users").child(userId!!).child("cover").setValue(downloadUri)
                        prog.dismiss()
                    }
                }.addOnFailureListener {
                    prog.dismiss()
                    Toast.makeText(activity, "" + it.message, Toast.LENGTH_SHORT).show()
                }
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
            activity?.startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()

        }
    }

}
