package com.github.qingmei2.core.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import com.github.qingmei2.core.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    companion object {

        fun launch(context: FragmentActivity) =
            context.apply {
                startActivity(Intent(this, MainActivity::class.java))
            }
    }
}