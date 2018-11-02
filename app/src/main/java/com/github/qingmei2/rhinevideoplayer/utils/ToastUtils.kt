package com.github.qingmei2.rhinevideoplayer.utils

import com.github.qingmei2.rhinevideoplayer.base.BaseApplication
import com.qingmei2.rhine.ext.toast

inline fun toast(value: () -> String): Unit =
    BaseApplication.INSTANCE.toast(value)