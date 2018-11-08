package com.github.qingmei2.module_player.activity

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.qingmei2.module_player.R
import com.github.qingmei2.module_player.constants.PlayParameter
import kotlinx.android.synthetic.main.fragment_player_urlplay_layout.*

/**
 * vid设置界面
 * Created by Mulberry on 2018/4/4.
 */
class AliyunUrlPlayFragment : Fragment() {

    private var onNotifyActivityListener: OnNotifyActivityListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_player_urlplay_layout, container, false)

    /**
     * start player by Url
     */
    fun startPlayerByUrl() {
        if (!TextUtils.isEmpty(etPlayUrl.text.toString())) {
            val intent = Intent()
            intent.setClass(this.activity!!, AliyunPlayerSkinActivity::class.java)

            PlayParameter.PLAY_PARAM_TYPE = "localSource"
            PlayParameter.PLAY_PARAM_URL = etPlayUrl.text.toString()

            onNotifyActivityListener?.onNotifyActivity()
        } else {
            Toast.makeText(this.activity!!.applicationContext, R.string.play_url_null_toast, Toast.LENGTH_LONG).show()
        }
    }

    fun setOnNotifyActivityListener(listener: OnNotifyActivityListener) {
        this.onNotifyActivityListener = listener
    }
}
