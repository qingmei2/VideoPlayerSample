package com.github.qingmei2.module_player.activity

import android.os.Build
import android.support.v7.app.AppCompatActivity
import com.alivc.player.VcPlayerLog

open class BaseActivity : AppCompatActivity() {

    protected val isStrangePhone: Boolean
        get() {
            val strangePhone = ("mx5".equals(Build.DEVICE, ignoreCase = true)
                    || "Redmi Note2".equals(Build.DEVICE, ignoreCase = true)
                    || "Z00A_1".equals(Build.DEVICE, ignoreCase = true)
                    || "hwH60-L02".equals(Build.DEVICE, ignoreCase = true)
                    || "hermes".equals(Build.DEVICE, ignoreCase = true)
                    || "V4".equals(Build.DEVICE, ignoreCase = true) && "Meitu".equals(
                Build.MANUFACTURER,
                ignoreCase = true
            )
                    || "m1metal".equals(Build.DEVICE, ignoreCase = true) && "Meizu".equals(
                Build.MANUFACTURER,
                ignoreCase = true
            ))

            VcPlayerLog.e("lfj1115 ", " Build.Device = " + Build.DEVICE + " , isStrange = " + strangePhone)
            return strangePhone
        }
}
