package com.github.qingmei2.core.utils

import com.github.qingmei2.core.base.BaseApplication
import com.qingmei2.rhine.ext.toast

inline fun toast(value: () -> String): Unit =
    BaseApplication.INSTANCE.toast(value)