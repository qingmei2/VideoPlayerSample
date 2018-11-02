package com.github.qingmei2.rhinevideoplayer

import android.content.SharedPreferences
import com.qingmei2.rhine.util.prefs.boolean
import com.qingmei2.rhine.util.prefs.string

class PrefsHelper(prefs: SharedPreferences) {

    var autoLogin by prefs.boolean("autoLogin", true)

    var username by prefs.string("username", "")
    var password by prefs.string("password", "")
}