package com.example.microphone.foruser

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.microphone.MainActivity
import com.example.microphone.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var prog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser!=null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }


        prog=ProgressDialog(this)
        prog.setMessage("wait..")


        login_btn_LogIn.setOnClickListener {
            prog.show()
            val username=login_ed_email.text.toString()
            val password=login_ed_password.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()){
                auth.signInWithEmailAndPassword(username, password).addOnCompleteListener {
                    if (it.isSuccessful){
                        prog.dismiss()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }.addOnFailureListener {
                    prog.dismiss()
                    Toast.makeText(this, ""+it.message, Toast.LENGTH_SHORT).show()
                }
            }


        }





    }

    fun toRegister(view: View) {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}