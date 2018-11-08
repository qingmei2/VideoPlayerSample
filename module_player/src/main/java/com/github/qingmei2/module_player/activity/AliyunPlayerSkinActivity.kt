package com.github.qingmei2.module_player.activity

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.*
import com.alivc.player.VcPlayerLog
import com.aliyun.vodplayer.downloader.*
import com.aliyun.vodplayer.media.AliyunLocalSource
import com.aliyun.vodplayer.media.AliyunVidSts
import com.aliyun.vodplayer.media.IAliyunVodPlayer
import com.github.qingmei2.module_player.R
import com.github.qingmei2.module_player.constants.PlayParameter
import com.github.qingmei2.module_player.playlist.AlivcPlayListAdapter
import com.github.qingmei2.module_player.playlist.AlivcPlayListManager
import com.github.qingmei2.module_player.playlist.AlivcVideoInfo
import com.github.qingmei2.module_player.utils.Commen
import com.github.qingmei2.module_player.utils.ScreenUtils
import com.github.qingmei2.module_player.utils.VidStsUtil
import com.github.qingmei2.module_player.utils.download.DownloadDBHelper
import com.github.qingmei2.module_player.view.choice.AlivcShowMoreDialog
import com.github.qingmei2.module_player.view.control.ControlView
import com.github.qingmei2.module_player.view.download.*
import com.github.qingmei2.module_player.view.more.AliyunShowMoreValue
import com.github.qingmei2.module_player.view.more.ShowMoreView
import com.github.qingmei2.module_player.view.more.SpeedValue
import com.github.qingmei2.module_player.view.tipsview.ErrorInfo
import com.github.qingmei2.module_player.widget.AliyunScreenMode
import com.github.qingmei2.module_player.widget.AliyunVodPlayerView

import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * 播放器和播放列表界面 Created by Mulberry on 2018/4/9.
 */
class AliyunPlayerSkinActivity : BaseActivity() {

    private var dbHelper: DownloadDBHelper? = null
    private var config: AliyunDownloadConfig? = null
    private var playerHandler: PlayerHandler? = null
    private var dialogDownloadView: DownloadView? = null
    private var showMoreDialog: AlivcShowMoreDialog? = null

    private val format = SimpleDateFormat("HH:mm:ss.SS")
    private val logStrs = ArrayList<String>()

    private var currentScreenMode = AliyunScreenMode.Small
    private var tvLogs: TextView? = null
    private var tvTabLogs: TextView? = null
    private var tvTabDownloadVideo: TextView? = null
    private var ivLogs: ImageView? = null
    private var ivDownloadVideo: ImageView? = null
    private var llClearLogs: LinearLayout? = null
    private var rlLogsContent: RelativeLayout? = null
    private var rlDownloadManagerContent: RelativeLayout? = null
    private var tvVideoList: TextView? = null
    private var ivVideoList: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var llVideoList: LinearLayout? = null
    private var tvStartSetting: TextView? = null

    private var downloadView: DownloadView? = null
    private var mAliyunVodPlayerView: AliyunVodPlayerView? = null

    private var downloadDataProvider: DownloadDataProvider? = null
    private var downloadManager: AliyunDownloadManager? = null
    private var alivcPlayListAdapter: AlivcPlayListAdapter? = null

    private var alivcVideoInfos: ArrayList<AlivcVideoInfo.Video>? = null
    private var currentError = ErrorInfo.Normal
    /**
     * get StsToken stats
     */
    private var inRequest: Boolean = false

    /**
     * 当前tab
     */
    private var currentTab = TAB_VIDEO_LIST
    private var commenUtils: Commen? = null
    private var oldTime: Long = 0
    /**
     * 当前点击的视频列表的下标
     */
    private var currentVidItemPosition: Int = 0

    private var currentVideoPosition: Int = 0

    private var downloadDialog: Dialog? = null

    private var aliyunDownloadMediaInfo: AliyunDownloadMediaInfo? = null
    private val currentDownloadIndex: Long = 0
    /**
     * 开始下载的事件监听
     */
    private val viewClickListener = object : AddDownloadView.OnViewClickListener {
        override fun onCancel() {
            if (downloadDialog != null) {
                downloadDialog!!.dismiss()
            }
        }

        override fun onDownload(info: AliyunDownloadMediaInfo) {
            if (downloadDialog != null) {
                downloadDialog!!.dismiss()
            }

            aliyunDownloadMediaInfo = info

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                val permission = ContextCompat.checkSelfPermission(
                    this@AliyunPlayerSkinActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                        this@AliyunPlayerSkinActivity, PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                    )

                } else {
                    addNewInfo(info)
                }
            } else {
                addNewInfo(info)
            }

        }
    }

    internal var aliyunDownloadMediaInfoList: MutableList<AliyunDownloadMediaInfo>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isStrangePhone) {
            //            setTheme(R.style.ActTheme);
        } else {
            setTheme(R.style.NoActionTheme)
        }
        copyAssets()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.alivc_player_layout_skin)

        requestVidSts()
        dbHelper = DownloadDBHelper.getDownloadHelper(applicationContext, 1)
        initAliyunPlayerView()
        initLogView()
        initDownloadView()
        initVideoListView()

    }

    private fun copyAssets() {
        commenUtils = Commen.getInstance(applicationContext).copyAssetsToSD("encrypt", "aliyun")
        commenUtils!!.setFileOperateCallback(

            object : Commen.FileOperateCallback {
                override fun onSuccess() {
                    config = AliyunDownloadConfig()
                    config!!.secretImagePath = Environment.getExternalStorageDirectory().absolutePath +
                            "/aliyun/encryptedApp.dat"
                    //        config.setDownloadPassword("123456789");
                    val file = File(Environment.getExternalStorageDirectory().absolutePath + "/test_save/")
                    if (!file.exists()) {
                        file.mkdir()
                    }
                    config!!.downloadDir = file.absolutePath
                    //设置同时下载个数
                    config!!.maxNums = 2
                    // 获取AliyunDownloadManager对象
                    downloadManager = AliyunDownloadManager.getInstance(applicationContext)
                    downloadManager!!.setDownloadConfig(config)

                    downloadDataProvider = DownloadDataProvider.getSingleton(applicationContext)
                    // 更新sts回调
                    downloadManager!!.setRefreshStsCallback(MyRefreshStsCallback())
                    // 视频下载的回调
                    downloadManager!!.setDownloadInfoListener(MyDownloadInfoListener(downloadView!!))
                    //
                    downloadViewSetting(downloadView!!)
                }

                override fun onFailed(error: String) {}
            })
    }

    private fun initAliyunPlayerView() {
        mAliyunVodPlayerView = findViewById<View>(R.id.video_view) as AliyunVodPlayerView
        //保持屏幕敞亮
        mAliyunVodPlayerView!!.keepScreenOn = true
        PlayParameter.PLAY_PARAM_URL = DEFAULT_URL
        val sdDir = Environment.getExternalStorageDirectory().absolutePath + "/test_save_cache"
        mAliyunVodPlayerView!!.setPlayingCache(false, sdDir, 60 * 60 /*时长, s */, 300 /*大小，MB*/)
        mAliyunVodPlayerView!!.setTheme(AliyunVodPlayerView.Theme.Blue)
        //mAliyunVodPlayerView.setCirclePlay(true);
        mAliyunVodPlayerView!!.setAutoPlay(true)

        mAliyunVodPlayerView!!.setOnPreparedListener(MyPrepareListener(this))
        mAliyunVodPlayerView!!.setNetConnectedListener(MyNetConnectedListener(this))
        mAliyunVodPlayerView!!.setOnCompletionListener(MyCompletionListener(this))
        mAliyunVodPlayerView!!.setOnFirstFrameStartListener(MyFrameInfoListener(this))
        mAliyunVodPlayerView!!.setOnChangeQualityListener(MyChangeQualityListener(this))
        mAliyunVodPlayerView!!.setOnStoppedListener(MyStoppedListener(this))
        mAliyunVodPlayerView!!.setmOnPlayerViewClickListener(MyPlayViewClickListener())
        mAliyunVodPlayerView!!.setOrientationChangeListener(MyOrientationChangeListener(this))
        mAliyunVodPlayerView!!.setOnUrlTimeExpiredListener(MyOnUrlTimeExpiredListener(this))
        mAliyunVodPlayerView!!.setOnShowMoreClickListener(MyShowMoreClickLisener(this))
        mAliyunVodPlayerView!!.setOnPlayStateBtnClickListener(MyPlayStateBtnClickListener(this))
        mAliyunVodPlayerView!!.setOnSeekCompleteListener(MySeekCompleteListener(this))
        mAliyunVodPlayerView!!.setOnSeekStartListener(MySeekStartListener(this))
        mAliyunVodPlayerView!!.enableNativeLog()

    }

    /**
     * 请求sts
     */
    private fun requestVidSts() {
        if (inRequest) {
            return
        }
        inRequest = true
        PlayParameter.PLAY_PARAM_VID = DEFAULT_VID
        VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, MyStsListener(this))
    }

    /**
     * 获取播放列表数据
     */
    private fun loadPlayList() {

        AlivcPlayListManager.getInstance().fetchPlayList(
            PlayParameter.PLAY_PARAM_AK_ID,
            PlayParameter.PLAY_PARAM_AK_SECRE,
            PlayParameter.PLAY_PARAM_SCU_TOKEN
        ) { code, videos ->
            runOnUiThread {
                if (alivcVideoInfos != null && alivcVideoInfos!!.size == 0) {
                    alivcVideoInfos!!.clear()
                    alivcVideoInfos!!.addAll(videos)
                    alivcPlayListAdapter!!.notifyDataSetChanged()
                    // 请求sts成功后, 加载播放资源,和视频列表
                    val video = alivcVideoInfos!![0]
                    PlayParameter.PLAY_PARAM_VID = video.videoId
                    setPlaySource()
                }
            }
        }
    }

    /**
     * init视频列表tab
     */
    private fun initVideoListView() {
        tvVideoList = findViewById(R.id.tv_tab_video_list)
        ivVideoList = findViewById(R.id.iv_video_list)
        recyclerView = findViewById(R.id.video_list)
        llVideoList = findViewById(R.id.ll_video_list)
        tvStartSetting = findViewById(R.id.tv_start_player)
        alivcVideoInfos = ArrayList()
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        alivcPlayListAdapter = AlivcPlayListAdapter(this, alivcVideoInfos)

        ivVideoList!!.isActivated = true
        llVideoList!!.visibility = View.VISIBLE
        rlDownloadManagerContent!!.visibility = View.GONE
        rlLogsContent!!.visibility = View.GONE

        tvVideoList!!.setOnClickListener {
            currentTab = TAB_VIDEO_LIST
            ivVideoList!!.isActivated = true
            ivLogs!!.isActivated = false
            ivDownloadVideo!!.isActivated = false
            downloadView!!.changeDownloadEditState(false)
            llVideoList!!.visibility = View.VISIBLE
            rlDownloadManagerContent!!.visibility = View.GONE
            rlLogsContent!!.visibility = View.GONE
        }

        recyclerView!!.adapter = alivcPlayListAdapter

        alivcPlayListAdapter!!.setOnVideoListItemClick(AlivcPlayListAdapter.OnVideoListItemClick { position ->
            val currentClickTime = System.currentTimeMillis()
            // 防止快速点击
            if (currentVidItemPosition == position && currentClickTime - oldTime <= 2000) {
                return@OnVideoListItemClick
            }
            PlayParameter.PLAY_PARAM_TYPE = "vidsts"
            // 点击视频列表, 切换播放的视频
            changePlaySource(position)
            oldTime = currentClickTime
            currentVidItemPosition = position
        })

        // 开启vid和url设置界面
        tvStartSetting!!.setOnClickListener {
            val intent = Intent(this@AliyunPlayerSkinActivity, AliyunPlayerSettingActivity::class.java)
            // 开启时, 默认为vid
            startActivityForResult(intent, CODE_REQUEST_SETTING)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setPlaySource()
        loadPlayList()
    }

    /**
     * 切换播放资源
     *
     * @param position 需要播放的数据在集合中的下标
     */
    private fun changePlaySource(position: Int) {

        currentVideoPosition = position

        val video = alivcVideoInfos!![position]

        changePlayVidSource(video.videoId, video.title)
    }

    /**
     * 播放本地资源
     *
     * @param url
     * @param title
     */
    private fun changePlayLocalSource(url: String, title: String) {
        val alsb = AliyunLocalSource.AliyunLocalSourceBuilder()
        alsb.setSource(url)
        alsb.setTitle(title)
        val localSource = alsb.build()
        mAliyunVodPlayerView!!.setLocalSource(localSource)
    }

    /**
     * 切换播放vid资源
     *
     * @param vid   切换视频的vid
     * @param title 切换视频的title
     */
    private fun changePlayVidSource(vid: String, title: String) {
        val vidSts = AliyunVidSts()
        vidSts.vid = vid
        vidSts.acId = PlayParameter.PLAY_PARAM_AK_ID
        vidSts.akSceret = PlayParameter.PLAY_PARAM_AK_SECRE
        vidSts.securityToken = PlayParameter.PLAY_PARAM_SCU_TOKEN
        vidSts.title = title
        mAliyunVodPlayerView!!.setVidSts(vidSts)
        downloadManager!!.prepareDownloadMedia(vidSts)
    }

    /**
     * init 日志tab
     */
    private fun initLogView() {
        tvLogs = findViewById<View>(R.id.tv_logs) as TextView
        tvTabLogs = findViewById<View>(R.id.tv_tab_logs) as TextView
        ivLogs = findViewById<View>(R.id.iv_logs) as ImageView
        llClearLogs = findViewById<View>(R.id.ll_clear_logs) as LinearLayout
        rlLogsContent = findViewById<View>(R.id.rl_logs_content) as RelativeLayout

        //日志Tab默认不选择
        ivLogs!!.isActivated = false

        //日志清除
        llClearLogs!!.setOnClickListener {
            logStrs.clear()
            tvLogs!!.text = ""
        }

        tvTabLogs!!.setOnClickListener {
            currentTab = TAB_LOG
            // TODO: 2018/4/10 show logs contents view
            ivLogs!!.isActivated = true
            ivDownloadVideo!!.isActivated = false
            ivVideoList!!.isActivated = false
            rlLogsContent!!.visibility = View.VISIBLE
            downloadView!!.changeDownloadEditState(false)
            rlDownloadManagerContent!!.visibility = View.GONE
            llVideoList!!.visibility = View.GONE
        }
    }

    /**
     * init下载(离线视频)tab
     */
    private fun initDownloadView() {
        tvTabDownloadVideo = findViewById<View>(R.id.tv_tab_download_video) as TextView
        ivDownloadVideo = findViewById<View>(R.id.iv_download_video) as ImageView
        rlDownloadManagerContent = findViewById<View>(R.id.rl_download_manager_content) as RelativeLayout
        downloadView = findViewById<View>(R.id.download_view) as DownloadView
        //离线下载Tab默认不选择
        ivDownloadVideo!!.isActivated = false
        tvTabDownloadVideo!!.setOnClickListener {
            currentTab = TAB_DOWNLOAD_LIST
            // TODO: 2018/4/10 show download content
            ivDownloadVideo!!.isActivated = true
            ivLogs!!.isActivated = false
            ivVideoList!!.isActivated = false
            rlLogsContent!!.visibility = View.GONE
            llVideoList!!.visibility = View.GONE
            rlDownloadManagerContent!!.visibility = View.VISIBLE
            //Drawable drawable = getResources().getDrawable(R.drawable.alivc_new_download);
            //drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());

            updateDownloadTaskTip()
        }
    }

    /**
     * downloadView的配置 里面配置了需要下载的视频的信息, 事件监听等 抽取该方法的主要目的是, 横屏下download dialog的离线视频列表中也用到了downloadView, 而两者显示内容和数据是同步的,
     * 所以在此进行抽取 AliyunPlayerSkinActivity.class#showAddDownloadView(DownloadVie view)中使用
     *
     * @param downloadView
     */
    private fun downloadViewSetting(downloadView: DownloadView) {
        downloadView.addAllDownloadMediaInfo(downloadDataProvider!!.allDownloadMediaInfo)

        downloadView.setOnDownloadViewListener(object : DownloadView.OnDownloadViewListener {
            override fun onStop(downloadMediaInfo: AliyunDownloadMediaInfo) {
                downloadManager!!.stopDownloadMedia(downloadMediaInfo)
            }

            override fun onStart(downloadMediaInfo: AliyunDownloadMediaInfo) {
                downloadManager!!.startDownloadMedia(downloadMediaInfo)
            }

            override fun onDeleteDownloadInfo(alivcDownloadMediaInfos: ArrayList<AlivcDownloadMediaInfo>?) {
                // 视频删除的dialog
                val alivcDialog = AlivcDialog(this@AliyunPlayerSkinActivity)
                alivcDialog.setDialogIcon(R.drawable.icon_delete_tips)
                alivcDialog.setMessage(resources.getString(R.string.alivc_delete_confirm))
                alivcDialog.setOnConfirmclickListener(
                    resources.getString(R.string.alivc_dialog_sure)
                ) {
                    alivcDialog.dismiss()
                    if (alivcDownloadMediaInfos != null && alivcDownloadMediaInfos.size > 0) {
                        downloadView.deleteDownloadInfo()
                        if (dialogDownloadView != null) {

                            dialogDownloadView!!.deleteDownloadInfo()
                        }
                        downloadDataProvider!!.deleteAllDownloadInfo(alivcDownloadMediaInfos)
                    } else {
                        Toast.makeText(this@AliyunPlayerSkinActivity, "没有删除的视频选项...", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                alivcDialog.setOnCancelOnclickListener(
                    resources.getString(R.string.alivc_dialog_cancle)
                ) { alivcDialog.dismiss() }
                alivcDialog.show()
            }
        })

        downloadView.setOnDownloadedItemClickListener(object : DownloadView.OnDownloadItemClickListener {
            override fun onDownloadedItemClick(positin: Int) {

                val downloadedList = downloadDataProvider!!.allDownloadMediaInfo
                //// 存入顺序和显示顺序相反,  所以进行倒序

                val tempList = ArrayList<AliyunDownloadMediaInfo>()
                val size = downloadedList.size
                for (i in 0 until size) {
                    if (downloadedList[i].progress == 100) {
                        tempList.add(downloadedList[i])
                    }
                }

                Collections.reverse(tempList)
                tempList.add(downloadedList[downloadedList.size - 1])
                for (i in 0 until size) {
                    val downloadMediaInfo = downloadedList[i]
                    if (!tempList.contains(downloadMediaInfo)) {
                        tempList.add(downloadMediaInfo)
                    }
                }

                if (positin < 0) {
                    Toast.makeText(this@AliyunPlayerSkinActivity, "视频资源不存在", Toast.LENGTH_SHORT).show()
                    return
                }

                // 如果点击列表中的视频, 需要将类型改为vid
                val aliyunDownloadMediaInfo = tempList[positin]
                PlayParameter.PLAY_PARAM_TYPE = "localSource"
                if (aliyunDownloadMediaInfo != null) {
                    PlayParameter.PLAY_PARAM_URL = aliyunDownloadMediaInfo.savePath
                    mAliyunVodPlayerView!!.updateScreenShow()
                    changePlayLocalSource(PlayParameter.PLAY_PARAM_URL, aliyunDownloadMediaInfo.title)
                }
            }

            override fun onDownloadingItemClick(infos: ArrayList<AlivcDownloadMediaInfo>, position: Int) {
                val alivcInfo = infos[position]
                val aliyunDownloadInfo = alivcInfo.aliyunDownloadMediaInfo
                val status = aliyunDownloadInfo.status
                if (status == AliyunDownloadMediaInfo.Status.Error || status == AliyunDownloadMediaInfo.Status.Wait) {
                    //downloadManager.removeDownloadMedia(aliyunDownloadInfo);
                    downloadManager!!.startDownloadMedia(aliyunDownloadInfo)
                }
            }

        })
    }

    private class MyPrepareListener(skinActivity: AliyunPlayerSkinActivity) : IAliyunVodPlayer.OnPreparedListener {

        private val activityWeakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            activityWeakReference = WeakReference(skinActivity)
        }

        override fun onPrepared() {
            val activity = activityWeakReference.get()
            activity?.onPrepared()
        }
    }

    private fun onPrepared() {
        logStrs.add(format.format(Date()) + getString(R.string.log_prepare_success))

        for (log in logStrs) {
            tvLogs!!.append(log + "\n")
        }
        Toast.makeText(
            this@AliyunPlayerSkinActivity.applicationContext, R.string.toast_prepare_success,
            Toast.LENGTH_SHORT
        ).show()
    }

    private class MyCompletionListener(skinActivity: AliyunPlayerSkinActivity) : IAliyunVodPlayer.OnCompletionListener {

        private val activityWeakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            activityWeakReference = WeakReference(skinActivity)
        }

        override fun onCompletion() {

            val activity = activityWeakReference.get()
            activity?.onCompletion()
        }
    }

    private fun onCompletion() {
        logStrs.add(format.format(Date()) + getString(R.string.log_play_completion))
        for (log in logStrs) {
            tvLogs!!.append(log + "\n")
        }
        Toast.makeText(
            this@AliyunPlayerSkinActivity.applicationContext, R.string.toast_play_compleion,
            Toast.LENGTH_SHORT
        ).show()

        // 当前视频播放结束, 播放下一个视频
        onNext()
    }

    private fun onNext() {
        if (currentError == ErrorInfo.UnConnectInternet) {
            // 此处需要判断网络和播放类型
            // 网络资源, 播放完自动波下一个, 无网状态提示ErrorTipsView
            // 本地资源, 播放完需要重播, 显示Replay, 此处不需要处理
            if ("vidsts" == PlayParameter.PLAY_PARAM_TYPE) {
                mAliyunVodPlayerView!!.showErrorTipView(4014, -1, "当前网络不可用")
            }
            return
        }

        currentVideoPosition++
        if (currentVideoPosition >= alivcVideoInfos!!.size - 1) {
            //列表循环播放，如发现播放完成了从列表的第一个开始重新播放
            currentVideoPosition = 0
        }

        if (alivcVideoInfos!!.size > 0) {
            val video = alivcVideoInfos!![currentVideoPosition]
            if (video != null) {
                changePlayVidSource(video.videoId, video.title)
            }
        }

    }

    private class MyFrameInfoListener(skinActivity: AliyunPlayerSkinActivity) :
        IAliyunVodPlayer.OnFirstFrameStartListener {

        private val activityWeakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            activityWeakReference = WeakReference(skinActivity)
        }

        override fun onFirstFrameStart() {

            val activity = activityWeakReference.get()
            activity?.onFirstFrameStart()
        }
    }

    private fun onFirstFrameStart() {
        val debugInfo = mAliyunVodPlayerView!!.allDebugInfo
        var createPts: Long = 0
        if (debugInfo!!["create_player"] != null) {
            val time = debugInfo["create_player"]
            createPts = java.lang.Double.parseDouble(time!!).toLong()
            logStrs.add(format.format(Date(createPts)) + getString(R.string.log_player_create_success))
        }
        if (debugInfo["open-url"] != null) {
            val time = debugInfo["open-url"]
            val openPts = java.lang.Double.parseDouble(time!!).toLong() + createPts
            logStrs.add(format.format(Date(openPts)) + getString(R.string.log_open_url_success))
        }
        if (debugInfo["find-stream"] != null) {
            val time = debugInfo["find-stream"]
            val findPts = java.lang.Double.parseDouble(time!!).toLong() + createPts
            logStrs.add(format.format(Date(findPts)) + getString(R.string.log_request_stream_success))
        }
        if (debugInfo["open-stream"] != null) {
            val time = debugInfo["open-stream"]
            val openPts = java.lang.Double.parseDouble(time!!).toLong() + createPts
            logStrs.add(format.format(Date(openPts)) + getString(R.string.log_start_open_stream))
        }
        logStrs.add(format.format(Date()) + getString(R.string.log_first_frame_played))
        for (log in logStrs) {
            tvLogs!!.append(log + "\n")
        }
    }

    private inner class MyPlayViewClickListener : AliyunVodPlayerView.OnPlayerViewClickListener {
        override fun onClick(screenMode: AliyunScreenMode, viewType: AliyunVodPlayerView.PlayViewType) {
            // 如果当前的Type是Download, 就显示Download对话框
            if (viewType == AliyunVodPlayerView.PlayViewType.Download) {
                showAddDownloadView(screenMode)
            }
        }
    }

    /**
     * 显示download 对话框
     *
     * @param screenMode
     */
    private fun showAddDownloadView(screenMode: AliyunScreenMode) {
        downloadDialog = DownloadChoiceDialog(this, screenMode)
        val contentView = AddDownloadView(this, screenMode)
        contentView.onPrepared(aliyunDownloadMediaInfoList)
        contentView.setOnViewClickListener(viewClickListener)
        val inflate = LayoutInflater.from(applicationContext).inflate(
            R.layout.alivc_dialog_download_video, null, false
        )
        dialogDownloadView = inflate.findViewById(R.id.download_view)
        downloadDialog!!.setContentView(contentView)
        downloadDialog!!.setOnDismissListener { }
        downloadDialog!!.show()
        downloadDialog!!.setCanceledOnTouchOutside(true)

        if (screenMode == AliyunScreenMode.Full) {
            contentView.setOnShowVideoListLisener {
                downloadViewSetting(dialogDownloadView!!)
                downloadDialog!!.setContentView(inflate)
            }
        }
    }

    private fun addNewInfo(info: AliyunDownloadMediaInfo?) {
        if (downloadManager != null) {
            downloadManager!!.addDownloadMedia(info)
            downloadManager!!.startDownloadMedia(info)
        }
        if (downloadView != null) {
            downloadView!!.addDownloadMediaInfo(info)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addNewInfo(aliyunDownloadMediaInfo)
            } else {
                // Permission Denied
                Toast.makeText(this@AliyunPlayerSkinActivity, "没有sd卡读写权限, 无法下载", Toast.LENGTH_SHORT).show()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private inner class MyDownloadInfoListener(private val downloadView: DownloadView) : AliyunDownloadInfoListener {

        override fun onPrepared(infos: List<AliyunDownloadMediaInfo>) {
            Collections.sort(infos, Comparator { mediaInfo1, mediaInfo2 ->
                if (mediaInfo1.size > mediaInfo2.size) {
                    return@Comparator 1
                }
                if (mediaInfo1.size < mediaInfo2.size) {
                    return@Comparator -1
                }

                if (mediaInfo1.size == mediaInfo2.size) {
                    0
                } else 0
            })
            onDownloadPrepared(infos)
        }

        override fun onStart(info: AliyunDownloadMediaInfo) {
            Toast.makeText(this@AliyunPlayerSkinActivity, "开始下载", Toast.LENGTH_SHORT).show()
            //downloadView.addDownloadMediaInfo(info);
            //dbHelper.insert(info, DownloadDBHelper.DownloadState.STATE_DOWNLOADING);
            if (!downloadDataProvider!!.hasAdded(info)) {
                updateDownloadTaskTip()
                downloadDataProvider!!.addDownloadMediaInfo(info)
            }

        }

        override fun onProgress(info: AliyunDownloadMediaInfo, percent: Int) {
            downloadView.updateInfo(info)
            if (dialogDownloadView != null) {
                dialogDownloadView!!.updateInfo(info)
            }
        }

        override fun onStop(info: AliyunDownloadMediaInfo) {
            Log.d("yds100", "onStop")
            downloadView.updateInfo(info)
            if (dialogDownloadView != null) {
                dialogDownloadView!!.updateInfo(info)
            }
            //dbHelper.update(info, DownloadDBHelper.DownloadState.STATE_PAUSE);
        }

        override fun onCompletion(info: AliyunDownloadMediaInfo) {
            Log.d("yds100", "onCompletion")
            downloadView.updateInfoByComplete(info)
            if (dialogDownloadView != null) {
                dialogDownloadView!!.updateInfoByComplete(info)
            }
            downloadDataProvider!!.addDownloadMediaInfo(info)
            //aliyunDownloadMediaInfoList.remove(info);
        }

        override fun onError(info: AliyunDownloadMediaInfo, code: Int, msg: String, requestId: String) {
            Log.d("yds100", "onError$msg")
            downloadView.updateInfoByError(info)
            if (dialogDownloadView != null) {
                dialogDownloadView!!.updateInfoByError(info)
            }
            val message = Message.obtain()
            val bundle = Bundle()
            bundle.putString(DOWNLOAD_ERROR_KEY, msg)
            message.data = bundle
            message.what = DOWNLOAD_ERROR
            playerHandler = PlayerHandler(this@AliyunPlayerSkinActivity)
            playerHandler!!.sendMessage(message)
        }

        override fun onWait(outMediaInfo: AliyunDownloadMediaInfo) {
            Log.d("yds100", "onWait")
        }

        override fun onM3u8IndexUpdate(outMediaInfo: AliyunDownloadMediaInfo, index: Int) {
            Log.d("yds100", "onM3u8IndexUpdate")
        }
    }

    private fun onDownloadPrepared(infos: List<AliyunDownloadMediaInfo>) {
        aliyunDownloadMediaInfoList = ArrayList()
        aliyunDownloadMediaInfoList!!.addAll(infos)
    }

    private class MyChangeQualityListener(skinActivity: AliyunPlayerSkinActivity) :
        IAliyunVodPlayer.OnChangeQualityListener {

        private val activityWeakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            activityWeakReference = WeakReference(skinActivity)
        }

        override fun onChangeQualitySuccess(finalQuality: String) {

            val activity = activityWeakReference.get()
            activity?.onChangeQualitySuccess(finalQuality)
        }

        override fun onChangeQualityFail(code: Int, msg: String) {
            val activity = activityWeakReference.get()
            activity?.onChangeQualityFail(code, msg)
        }
    }

    private fun onChangeQualitySuccess(finalQuality: String) {
        logStrs.add(format.format(Date()) + getString(R.string.log_change_quality_success))
        Toast.makeText(
            this@AliyunPlayerSkinActivity.applicationContext,
            getString(R.string.log_change_quality_success), Toast.LENGTH_SHORT
        ).show()
    }

    internal fun onChangeQualityFail(code: Int, msg: String) {
        logStrs.add(format.format(Date()) + getString(R.string.log_change_quality_fail) + " : " + msg)
        Toast.makeText(
            this@AliyunPlayerSkinActivity.applicationContext,
            getString(R.string.log_change_quality_fail), Toast.LENGTH_SHORT
        ).show()
    }

    private class MyStoppedListener(skinActivity: AliyunPlayerSkinActivity) : IAliyunVodPlayer.OnStoppedListener {

        private val activityWeakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            activityWeakReference = WeakReference(skinActivity)
        }

        override fun onStopped() {

            val activity = activityWeakReference.get()
            activity?.onStopped()
        }
    }

    private class MyRefreshStsCallback : AliyunRefreshStsCallback {

        override fun refreshSts(
            vid: String,
            quality: String,
            format: String,
            title: String,
            encript: Boolean
        ): AliyunVidSts? {
            VcPlayerLog.d("refreshSts ", "refreshSts , vid = $vid")
            //NOTE: 注意：这个不能启动线程去请求。因为这个方法已经在线程中调用了。
            val vidSts = VidStsUtil.getVidSts(vid)
            if (vidSts == null) {
                return null
            } else {
                vidSts.vid = vid
                vidSts.quality = quality
                vidSts.title = title
                return vidSts
            }
        }
    }

    private fun onStopped() {
        Toast.makeText(
            this@AliyunPlayerSkinActivity.applicationContext, R.string.log_play_stopped,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setPlaySource() {
        if ("localSource" == PlayParameter.PLAY_PARAM_TYPE) {
            val alsb = AliyunLocalSource.AliyunLocalSourceBuilder()
            alsb.setSource(PlayParameter.PLAY_PARAM_URL)
            val uri = Uri.parse(PlayParameter.PLAY_PARAM_URL)
            if ("rtmp" == uri.scheme) {
                alsb.setTitle("")
            }
            val localSource = alsb.build()
            if (mAliyunVodPlayerView != null) {
                mAliyunVodPlayerView!!.setLocalSource(localSource)
            }

        } else if ("vidsts" == PlayParameter.PLAY_PARAM_TYPE) {
            if (!inRequest) {
                val vidSts = AliyunVidSts()
                vidSts.vid = PlayParameter.PLAY_PARAM_VID
                vidSts.acId = PlayParameter.PLAY_PARAM_AK_ID
                vidSts.akSceret = PlayParameter.PLAY_PARAM_AK_SECRE
                vidSts.securityToken = PlayParameter.PLAY_PARAM_SCU_TOKEN
                if (mAliyunVodPlayerView != null) {
                    mAliyunVodPlayerView!!.setVidSts(vidSts)
                }
                downloadManager!!.prepareDownloadMedia(vidSts)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updatePlayerViewMode()
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView!!.onResume()
        }
    }

    override fun onStop() {
        super.onStop()

        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView!!.onStop()
        }

        if (downloadManager != null && downloadDataProvider != null) {
            downloadManager!!.stopDownloadMedias(downloadDataProvider!!.allDownloadMediaInfo)
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updatePlayerViewMode()
    }

    private fun updateDownloadTaskTip() {
        if (currentTab != TAB_DOWNLOAD_LIST) {

            val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.alivc_download_new_task)
            drawable!!.setBounds(0, 0, 20, 20)
            tvTabDownloadVideo!!.compoundDrawablePadding = -20
            tvTabDownloadVideo!!.setCompoundDrawables(null, null, drawable, null)
        } else {
            tvTabDownloadVideo!!.setCompoundDrawables(null, null, null, null)
        }
    }

    private fun updatePlayerViewMode() {
        if (mAliyunVodPlayerView != null) {
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                //转为竖屏了。
                //显示状态栏
                //                if (!isStrangePhone()) {
                //                    getSupportActionBar().show();
                //                }

                this.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                mAliyunVodPlayerView!!.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

                //设置view的布局，宽高之类
                val aliVcVideoViewLayoutParams = mAliyunVodPlayerView!!
                    .layoutParams as LinearLayout.LayoutParams
                aliVcVideoViewLayoutParams.height = (ScreenUtils.getWidth(this) * 9.0f / 16).toInt()
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                //                if (!isStrangePhone()) {
                //                    aliVcVideoViewLayoutParams.topMargin = getSupportActionBar().getHeight();
                //                }

            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //转到横屏了。
                //隐藏状态栏
                if (!isStrangePhone) {
                    this.window.setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                    )
                    mAliyunVodPlayerView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }

                //设置view的布局，宽高
                val aliVcVideoViewLayoutParams = mAliyunVodPlayerView!!
                    .layoutParams as LinearLayout.LayoutParams
                aliVcVideoViewLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                //                if (!isStrangePhone()) {
                //                    aliVcVideoViewLayoutParams.topMargin = 0;
                //                }
            }

        }
    }

    override fun onDestroy() {
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView!!.onDestroy()
            mAliyunVodPlayerView = null
        }

        if (playerHandler != null) {
            playerHandler!!.removeMessages(DOWNLOAD_ERROR)
            playerHandler = null
        }

        if (commenUtils != null) {
            commenUtils!!.onDestroy()
            commenUtils = null
        }

        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (mAliyunVodPlayerView != null) {
            val handler = mAliyunVodPlayerView!!.onKeyDown(keyCode, event)
            if (!handler) {
                return false
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //解决某些手机上锁屏之后会出现标题栏的问题。
        updatePlayerViewMode()
    }

    private class PlayerHandler(activity: AliyunPlayerSkinActivity) : Handler() {
        //持有弱引用AliyunPlayerSkinActivity,GC回收时会被回收掉.
        private val mActivty: WeakReference<AliyunPlayerSkinActivity>

        init {
            mActivty = WeakReference(activity)
        }

        override fun handleMessage(msg: Message) {
            val activity = mActivty.get()
            super.handleMessage(msg)
            if (activity != null) {
                when (msg.what) {
                    DOWNLOAD_ERROR ->
                        //Toast.makeText(mActivty.get(), msg.getData().getString(DOWNLOAD_ERROR_KEY), Toast.LENGTH_LONG)
                        //    .show();
                        Log.d("donwload", msg.data.getString(DOWNLOAD_ERROR_KEY))
                    else -> {
                    }
                }
            }
        }
    }

    private class MyStsListener internal constructor(act: AliyunPlayerSkinActivity) : VidStsUtil.OnStsResultListener {

        private val weakctivity: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakctivity = WeakReference(act)
        }

        override fun onSuccess(vid: String, akid: String, akSecret: String, token: String) {
            val activity = weakctivity.get()
            activity?.onStsSuccess(vid, akid, akSecret, token)
        }

        override fun onFail() {
            val activity = weakctivity.get()
            activity?.onStsFail()
        }
    }

    private fun onStsFail() {

        Toast.makeText(applicationContext, R.string.request_vidsts_fail, Toast.LENGTH_LONG).show()
        inRequest = false
        //finish();
    }

    private fun onStsSuccess(mVid: String, akid: String, akSecret: String, token: String) {
        PlayParameter.PLAY_PARAM_VID = mVid
        PlayParameter.PLAY_PARAM_AK_ID = akid
        PlayParameter.PLAY_PARAM_AK_SECRE = akSecret
        PlayParameter.PLAY_PARAM_SCU_TOKEN = token

        inRequest = false

        // 视频列表数据为0时, 加载列表
        if (alivcVideoInfos != null && alivcVideoInfos!!.size == 0) {
            alivcVideoInfos!!.clear()
            loadPlayList()
        }
    }

    private class MyOrientationChangeListener(activity: AliyunPlayerSkinActivity) :
        AliyunVodPlayerView.OnOrientationChangeListener {

        private val weakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun orientationChange(from: Boolean, currentMode: AliyunScreenMode) {
            val activity = weakReference.get()
            activity?.hideDownloadDialog(from, currentMode)
            activity?.hideShowMoreDialog(from, currentMode)
        }
    }

    private fun hideShowMoreDialog(from: Boolean, currentMode: AliyunScreenMode) {
        if (showMoreDialog != null) {
            if (currentMode == AliyunScreenMode.Small) {
                showMoreDialog!!.dismiss()
                currentScreenMode = currentMode
            }
        }
    }

    private fun hideDownloadDialog(from: Boolean, currentMode: AliyunScreenMode) {

        if (downloadDialog != null) {
            if (currentScreenMode != currentMode) {
                downloadDialog!!.dismiss()
                currentScreenMode = currentMode
            }
        }
    }

    /**
     * 判断是否有网络的监听
     */
    private inner class MyNetConnectedListener(activity: AliyunPlayerSkinActivity) :
        AliyunVodPlayerView.NetConnectedListener {
        internal var weakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun onReNetConnected(isReconnect: Boolean) {
            val activity = weakReference.get()
            activity?.onReNetConnected(isReconnect)
        }

        override fun onNetUnConnected() {
            val activity = weakReference.get()
            activity?.onNetUnConnected()
        }
    }

    private fun onNetUnConnected() {
        currentError = ErrorInfo.UnConnectInternet
        if (aliyunDownloadMediaInfoList != null && aliyunDownloadMediaInfoList!!.size > 0) {
            downloadManager!!.stopDownloadMedias(aliyunDownloadMediaInfoList!!)
        }
    }

    private fun onReNetConnected(isReconnect: Boolean) {
        currentError = ErrorInfo.Normal
        if (isReconnect) {
            if (aliyunDownloadMediaInfoList != null && aliyunDownloadMediaInfoList!!.size > 0) {
                var unCompleteDownload = 0
                for (info in aliyunDownloadMediaInfoList!!) {
                    if (info.status == AliyunDownloadMediaInfo.Status.Stop) {
                        unCompleteDownload++
                    }
                }

                if (unCompleteDownload > 0) {
                    Toast.makeText(this, "网络恢复, 请手动开启下载任务...", Toast.LENGTH_SHORT).show()
                }
            }
            // 如果当前播放列表为空, 网络重连后需要重新请求sts和播放列表, 其他情况不需要
            if (alivcVideoInfos != null && alivcVideoInfos!!.size == 0) {
                VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, MyStsListener(this))
            }
        }
    }

    private class MyOnUrlTimeExpiredListener(activity: AliyunPlayerSkinActivity) :
        IAliyunVodPlayer.OnUrlTimeExpiredListener {
        internal var weakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun onUrlTimeExpired(s: String, s1: String) {
            val activity = weakReference.get()
            activity?.onUrlTimeExpired(s, s1)
        }
    }

    private fun onUrlTimeExpired(oldVid: String, oldQuality: String) {
        //requestVidSts();
        val vidSts = VidStsUtil.getVidSts(oldVid)
        PlayParameter.PLAY_PARAM_VID = vidSts!!.vid
        PlayParameter.PLAY_PARAM_AK_SECRE = vidSts.akSceret
        PlayParameter.PLAY_PARAM_AK_ID = vidSts.acId
        PlayParameter.PLAY_PARAM_SCU_TOKEN = vidSts.securityToken

    }

    private class MyShowMoreClickLisener internal constructor(activity: AliyunPlayerSkinActivity) :
        ControlView.OnShowMoreClickListener {
        internal var weakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun showMore() {
            val activity = weakReference.get()
            activity?.showMore(activity)
        }
    }

    private fun showMore(activity: AliyunPlayerSkinActivity) {
        showMoreDialog = AlivcShowMoreDialog(activity)
        val moreValue = AliyunShowMoreValue()
        moreValue.speed = mAliyunVodPlayerView!!.currentSpeed
        moreValue.volume = mAliyunVodPlayerView!!.currentVolume
        moreValue.screenBrightness = mAliyunVodPlayerView!!.currentScreenBrigtness

        val showMoreView = ShowMoreView(activity, moreValue)
        showMoreDialog!!.setContentView(showMoreView)
        showMoreDialog!!.show()
        showMoreView.setOnDownloadButtonClickListener(ShowMoreView.OnDownloadButtonClickListener {
            // 点击下载
            showMoreDialog!!.dismiss()
            if ("vidsts" != PlayParameter.PLAY_PARAM_TYPE) {
                Toast.makeText(activity, "Url类型不支持下载", Toast.LENGTH_SHORT).show()
                return@OnDownloadButtonClickListener
            }
            showAddDownloadView(AliyunScreenMode.Full)
        })

        showMoreView.setOnScreenCastButtonClickListener {
            Toast.makeText(
                this@AliyunPlayerSkinActivity,
                "功能开发中, 敬请期待...",
                Toast.LENGTH_SHORT
            ).show()
        }

        showMoreView.setOnBarrageButtonClickListener {
            Toast.makeText(
                this@AliyunPlayerSkinActivity,
                "功能开发中, 敬请期待...",
                Toast.LENGTH_SHORT
            ).show()
        }

        showMoreView.setOnSpeedCheckedChangedListener { group, checkedId ->
            // 点击速度切换
            if (checkedId == R.id.rb_speed_normal) {
                mAliyunVodPlayerView!!.changeSpeed(SpeedValue.One)
            } else if (checkedId == R.id.rb_speed_onequartern) {
                mAliyunVodPlayerView!!.changeSpeed(SpeedValue.OneQuartern)
            } else if (checkedId == R.id.rb_speed_onehalf) {
                mAliyunVodPlayerView!!.changeSpeed(SpeedValue.OneHalf)
            } else if (checkedId == R.id.rb_speed_twice) {
                mAliyunVodPlayerView!!.changeSpeed(SpeedValue.Twice)
            }
        }

        // 亮度seek
        showMoreView.setOnLightSeekChangeListener(object : ShowMoreView.OnLightSeekChangeListener {
            override fun onStart(seekBar: SeekBar) {

            }

            override fun onProgress(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mAliyunVodPlayerView!!.currentScreenBrigtness = progress
            }

            override fun onStop(seekBar: SeekBar) {

            }
        })

        showMoreView.setOnVoiceSeekChangeListener(object : ShowMoreView.OnVoiceSeekChangeListener {
            override fun onStart(seekBar: SeekBar) {

            }

            override fun onProgress(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mAliyunVodPlayerView!!.currentVolume = progress
            }

            override fun onStop(seekBar: SeekBar) {

            }
        })

    }

    /**
     * 获取url的scheme
     *
     * @param url
     * @return
     */
    private fun getUrlScheme(url: String): String? {
        return Uri.parse(url).scheme
    }

    private class MyPlayStateBtnClickListener internal constructor(activity: AliyunPlayerSkinActivity) :
        AliyunVodPlayerView.OnPlayStateBtnClickListener {
        internal var weakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun onPlayBtnClick(playerState: IAliyunVodPlayer.PlayerState) {
            val activity = weakReference.get()
            activity?.onPlayStateSwitch(playerState)

        }
    }

    /**
     * 播放状态切换
     *
     * @param playerState
     */
    private fun onPlayStateSwitch(playerState: IAliyunVodPlayer.PlayerState) {
        if (playerState == IAliyunVodPlayer.PlayerState.Started) {
            tvLogs!!.append(format.format(Date()) + " 暂停 \n")
        } else if (playerState == IAliyunVodPlayer.PlayerState.Paused) {
            tvLogs!!.append(format.format(Date()) + " 开始 \n")
        }

    }

    private class MySeekCompleteListener internal constructor(activity: AliyunPlayerSkinActivity) :
        IAliyunVodPlayer.OnSeekCompleteListener {
        internal var weakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun onSeekComplete() {
            val activity = weakReference.get()
            activity?.onSeekComplete()
        }
    }

    private fun onSeekComplete() {
        tvLogs!!.append(format.format(Date()) + getString(R.string.log_seek_completed) + "\n")
    }

    private class MySeekStartListener internal constructor(activity: AliyunPlayerSkinActivity) :
        AliyunVodPlayerView.OnSeekStartListener {
        internal var weakReference: WeakReference<AliyunPlayerSkinActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun onSeekStart() {
            val activity = weakReference.get()
            activity?.onSeekStart()
        }
    }

    private fun onSeekStart() {
        tvLogs!!.append(format.format(Date()) + getString(R.string.log_seek_start) + "\n")
    }

    companion object {
        /**
         * 开启设置界面的请求码
         */
        private val CODE_REQUEST_SETTING = 1000
        /**
         * 设置界面返回的结果码, 100为vid类型, 200为url类型
         */
        private val CODE_RESULT_TYPE_VID = 100
        private val CODE_RESULT_TYPE_URL = 200
        private val DEFAULT_URL = "http://player.alicdn.com/video/aliyunmedia.mp4"
        private val DEFAULT_VID = "6e783360c811449d8692b2117acc9212"

        private val PERMISSIONS_STORAGE =
            arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")

        private val REQUEST_EXTERNAL_STORAGE = 1
        private val TAB_VIDEO_LIST = 1
        private val TAB_LOG = 2
        private val TAB_DOWNLOAD_LIST = 3

        private val DOWNLOAD_ERROR = 1
        private val DOWNLOAD_ERROR_KEY = "error_key"
    }
}
