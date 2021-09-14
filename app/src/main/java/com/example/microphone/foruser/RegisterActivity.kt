package com.example.microphone.foruser

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.microphone.MainActivity
import com.example.microphone.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    val database = Firebase.database
    val myRef = database.getReference()
    var uri: Uri ?=null
    var storageRef = FirebaseStorage.getInstance().reference
    lateinit var prog:ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = FirebaseAuth.getInstance()

        prog=ProgressDialog(this)
        prog.setMessage("wait..")



        //for image
        reg_image.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type="image/*"
                startActivityForResult(it, 0)
            }
        }

        reg_btn_register.setOnClickListener {
            if (vald()){
                prog.show()
                registerUser()
            }else{
                return@setOnClickListener
            }
        }






    }


    private fun registerUser() {
        val email = reg_ed_email.text.toString()
        val password = reg_ed_password_confirm.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auth.createUserWithEmailAndPassword(email, password).await()
                    val user = auth.currentUser
                    user?.let { user ->
                        val hashMap: HashMap<String, String> = HashMap<String, String>()
                        uri?.let {
                            val refy=storageRef.child("images").child(user.uid)
                            refy.putFile(it).addOnSuccessListener {
                                var uriTask=it.getStorage().getDownloadUrl()

                                while (!uriTask.isSuccessful);

                                val downloadUri = uriTask.result.toString()
                                if (uriTask.isSuccessful){
                                    hashMap.put("name", reg_ed_name.text.toString())
                                    hashMap.put("bio", reg_ed_bio.text.toString())
                                    hashMap.put("email", reg_ed_email.text.toString())
                                    hashMap.put("id", user.uid)
                                    hashMap.put("image",downloadUri)
                                    hashMap.put("cover","")
                                    myRef.child("users").child(user.uid+"").setValue(hashMap)
                                    prog.dismiss()
                                    Toast.makeText(this@RegisterActivity,"Success", Toast.LENGTH_LONG).show()
                                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                    finish()

                                }

                            }.addOnFailureListener {
                                Toast.makeText(this@RegisterActivity, ""+it.message, Toast.LENGTH_SHORT).show()
                            }

                        }

                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, e.message + "", Toast.LENGTH_LONG).show()
                    }
                }
            }

    }

    private fun vald():Boolean{
        if (reg_ed_name.text.isEmpty()){
            reg_ed_name.error="required"
            return false
        }else if (reg_ed_bio.text.isEmpty()){
            reg_ed_bio.error="required"
            return false
        }else if (reg_ed_email.text.isEmpty()){
            reg_ed_email.error="required"
            return false
        }else if (reg_ed_password.text.isEmpty()){
            Toast.makeText(this, "Enter Your password bro !!", Toast.LENGTH_SHORT).show()
            return false
        }else if (!(reg_ed_password_confirm.text.toString().equals(reg_ed_password.text.toString()))){
            Toast.makeText(
                this,
                "Check your passwords bro and stop to drink beer again ok ?!",
                Toast.LENGTH_LONG
            ).show()
            return false
        }else if (uri==null){
            Toast.makeText(this, "Select image !!", Toast.LENGTH_LONG).show()
            return false
        }else{
            return true
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK && requestCode ==0){
            uri=data?.data
            reg_image.setImageURI(uri)
        }
    }

    fun backToLogin(view: View) {
        startActivity(Intent(this, LoginActivity::class.java))

    }


    private fun checkLoggedInState() {
        val user = auth.currentUser
        if (user == null) { // not logged in
        } else {

        }
    }

    override fun onStart() {
        super.onStart()
        checkLoggedInState()
    }
}