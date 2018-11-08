package com.github.qingmei2.module_player.activity.video

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.github.qingmei2.core.base.BaseActivity
import com.github.qingmei2.module_player.R
import com.github.qingmei2.module_player.databinding.ActivityVideoPlayerBinding
import org.kodein.di.Copy
import org.kodein.di.Kodein
import org.kodein.di.android.retainedKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance

class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding>() {

    override val kodein: Kodein by retainedKodein {
        extend(parentKodein, copy = Copy.All)
        import(videoPlayerKodeinModule)
        bind<VideoPlayerActivity>() with instance(this@VideoPlayerActivity)
    }

    private val delegate: VideoPlayerViewDelegate by instance()

    override val layoutId: Int = R.layout.activity_video_player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.delegate = delegate
    }

    companion object {

        fun launch(activity: FragmentActivity) =
            activity.apply {
                startActivity(Intent(this, VideoPlayerActivity::class.java))
            }
    }
}
