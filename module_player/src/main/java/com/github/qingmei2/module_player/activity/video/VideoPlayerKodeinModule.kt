package com.github.qingmei2.module_player.activity.video

import com.qingmei2.rhine.ext.viewmodel.addLifecycle
import org.kodein.di.Kodein
import org.kodein.di.android.AndroidComponentsWeakScope
import org.kodein.di.generic.*

const val VIDEOPLAYER_MODULE_TAG = "VIDEOPLAYER_MODULE_TAG"

val videoPlayerKodeinModule = Kodein.Module(VIDEOPLAYER_MODULE_TAG) {

    bind<VideoPlayerViewDelegate>() with scoped(AndroidComponentsWeakScope).singleton {
        VideoPlayerViewDelegate(instance())
    }

    bind<VideoPlayerViewModel>() with scoped(AndroidComponentsWeakScope).singleton {
        VideoPlayerViewModel().apply {
            addLifecycle(instance<VideoPlayerActivity>())
        }
    }
}