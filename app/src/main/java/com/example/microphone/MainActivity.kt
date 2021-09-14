package com.example.microphone

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.microphone.ui.home.HomeFragment
import com.example.microphone.ui.publish.PostFragment
import com.example.microphone.ui.profile.ProfileFragment
import com.example.microphone.ui.video.VideosFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //default fragment
        val homeFragment = HomeFragment()
        val ft1 = supportFragmentManager.beginTransaction()
        ft1.replace(R.id.conten, homeFragment, "")
        ft1.commit()
        bottom_menu.setItemSelected(R.id.home)


        bottom_menu.setOnItemSelectedListener { id->
            when(id){
                R.id.home -> {
                    val homeFragment = HomeFragment()
                    val ft1 = supportFragmentManager.beginTransaction()
                    ft1.replace(R.id.conten, homeFragment, "")
                    ft1.commit()
                }
                R.id.profile -> {
                    val profileFragment = ProfileFragment()
                    val ft2 = supportFragmentManager.beginTransaction()
                    ft2.replace(R.id.conten, profileFragment, "profile")
                    ft2.commit()
                }
                R.id.add_post -> {

                    val postFragment = PostFragment()
                    val ft2 = supportFragmentManager.beginTransaction()
                    ft2.replace(R.id.conten, postFragment, "")
                    ft2.commit()

                }
                R.id.videos -> {
                    val videosFragment = VideosFragment()
                    val ft2 = supportFragmentManager.beginTransaction()
                    ft2.replace(R.id.conten, videosFragment, "")
                    ft2.commit()

                }
            }

        }

        //swipe to refresh
        swiperefresh.setColorScheme(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary, R.color.colorInsta);
        swiperefresh.setOnRefreshListener {
            Handler().postDelayed(Runnable {
                swiperefresh.setRefreshing(false)
                bottom_menu.setItemSelected(R.id.home)
                recreate()
             }, 4000)

        }
    }


}