package com.github.qingmei2.rhinevideoplayer.ui.login

import android.support.v4.app.FragmentActivity
import com.github.qingmei2.rhinevideoplayer.ui.MainActivity

class LoginNavigator(
    private val context: FragmentActivity
) {

    fun toMain() =
        MainActivity.launch(context).also {
            context.finish()
        }
}