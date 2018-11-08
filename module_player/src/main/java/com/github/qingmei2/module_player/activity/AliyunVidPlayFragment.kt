package com.github.qingmei2.module_player.activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.qingmei2.module_player.R
import com.github.qingmei2.module_player.constants.PlayParameter
import com.github.qingmei2.module_player.utils.VidStsUtil
import com.qingmei2.rhine.ext.toast
import kotlinx.android.synthetic.main.alivc_player_view_vid_sts.*
import java.lang.ref.WeakReference

/**
 * vid设置界面
 * Created by Mulberry on 2018/4/4.
 */
class AliyunVidPlayFragment : Fragment() {
    /**
     * get StsToken stats
     */
    private var inRequest: Boolean = false

    private var onNotifyActivityListener: OnNotifyActivityListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_player_vidplay_layout, container, false)


    fun startToPlayerByVid() {
        val mVid = etVid.text.toString()
        val akId = etAkId.text.toString()
        val akSecret = etAkSecret.text.toString()
        val scuToken = etScuToken.text.toString()
        PlayParameter.PLAY_PARAM_TYPE = "vidsts"
        if (mVid.isEmpty() || akId.isEmpty() || akSecret.isEmpty() || scuToken.isEmpty()) {
            if (inRequest) {
                return
            }

            inRequest = true
            VidStsUtil.getVidSts(mVid, MyStsListener(this))
        } else {

            PlayParameter.PLAY_PARAM_VID = mVid
            PlayParameter.PLAY_PARAM_AK_ID = akId
            PlayParameter.PLAY_PARAM_AK_SECRE = akSecret
            PlayParameter.PLAY_PARAM_SCU_TOKEN = scuToken

            activity?.apply {
                setResult(CODE_RESULT_VID)
                finish()
            }
        }
    }

    private class MyStsListener(view: AliyunVidPlayFragment) : VidStsUtil.OnStsResultListener {

        private val weakActivity: WeakReference<AliyunVidPlayFragment> = WeakReference(view)

        override fun onSuccess(vid: String, akid: String, akSecret: String, token: String) =
            weakActivity.get()?.onStsSuccess(vid, akid, akSecret, token) ?: Unit

        override fun onFail() =
            weakActivity.get()?.onStsFail() ?: Unit
    }

    private fun onStsFail() {
        context?.toast { getString(R.string.request_vidsts_fail) }
        inRequest = false
    }

    private fun onStsSuccess(mVid: String, akid: String, akSecret: String, token: String) {
        PlayParameter.PLAY_PARAM_VID = mVid
        PlayParameter.PLAY_PARAM_AK_ID = akid
        PlayParameter.PLAY_PARAM_AK_SECRE = akSecret
        PlayParameter.PLAY_PARAM_SCU_TOKEN = token

        onNotifyActivityListener?.onNotifyActivity()

        inRequest = false
    }

    fun setOnNotifyActivityListener(listener: OnNotifyActivityListener) {
        this.onNotifyActivityListener = listener
    }

    companion object {

        /**
         * 返回给上个activity的resultcode: 100为vid播放类型, 200为URL播放类型
         */
        private const val CODE_RESULT_VID = 100
    }
}
