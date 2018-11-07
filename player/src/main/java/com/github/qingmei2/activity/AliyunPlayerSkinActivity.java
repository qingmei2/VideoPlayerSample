package com.github.qingmei2.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.alivc.player.VcPlayerLog;
import com.aliyun.vodplayer.downloader.*;
import com.aliyun.vodplayer.media.AliyunLocalSource;
import com.aliyun.vodplayer.media.AliyunVidSts;
import com.aliyun.vodplayer.media.IAliyunVodPlayer;
import com.github.qingmei2.R;
import com.github.qingmei2.constants.PlayParameter;
import com.github.qingmei2.playlist.AlivcPlayListAdapter;
import com.github.qingmei2.playlist.AlivcPlayListManager;
import com.github.qingmei2.playlist.AlivcVideoInfo;
import com.github.qingmei2.utils.Commen;
import com.github.qingmei2.utils.ScreenUtils;
import com.github.qingmei2.utils.VidStsUtil;
import com.github.qingmei2.utils.download.DownloadDBHelper;
import com.github.qingmei2.view.choice.AlivcShowMoreDialog;
import com.github.qingmei2.view.control.ControlView;
import com.github.qingmei2.view.download.*;
import com.github.qingmei2.view.more.AliyunShowMoreValue;
import com.github.qingmei2.view.more.ShowMoreView;
import com.github.qingmei2.view.more.SpeedValue;
import com.github.qingmei2.view.tipsview.ErrorInfo;
import com.github.qingmei2.widget.AliyunScreenMode;
import com.github.qingmei2.widget.AliyunVodPlayerView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 播放器和播放列表界面 Created by Mulberry on 2018/4/9.
 */
public class AliyunPlayerSkinActivity extends BaseActivity {

    private DownloadDBHelper dbHelper;
    private AliyunDownloadConfig config;
    private PlayerHandler playerHandler;
    private DownloadView dialogDownloadView;
    private AlivcShowMoreDialog showMoreDialog;

    private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SS");
    private List<String> logStrs = new ArrayList<>();

    private AliyunScreenMode currentScreenMode = AliyunScreenMode.Small;
    private TextView tvLogs;
    private TextView tvTabLogs;
    private TextView tvTabDownloadVideo;
    private ImageView ivLogs;
    private ImageView ivDownloadVideo;
    private LinearLayout llClearLogs;
    private RelativeLayout rlLogsContent;
    private RelativeLayout rlDownloadManagerContent;
    private TextView tvVideoList;
    private ImageView ivVideoList;
    private RecyclerView recyclerView;
    private LinearLayout llVideoList;
    private TextView tvStartSetting;

    private DownloadView downloadView;
    private AliyunVodPlayerView mAliyunVodPlayerView = null;

    private DownloadDataProvider downloadDataProvider;
    private AliyunDownloadManager downloadManager;
    private AlivcPlayListAdapter alivcPlayListAdapter;

    private ArrayList<AlivcVideoInfo.Video> alivcVideoInfos;
    private ErrorInfo currentError = ErrorInfo.Normal;
    /**
     * 开启设置界面的请求码
     */
    private static final int CODE_REQUEST_SETTING = 1000;
    /**
     * 设置界面返回的结果码, 100为vid类型, 200为url类型
     */
    private static final int CODE_RESULT_TYPE_VID = 100;
    private static final int CODE_RESULT_TYPE_URL = 200;
    private static final String DEFAULT_URL = "http://player.alicdn.com/video/aliyunmedia.mp4";
    private static final String DEFAULT_VID = "6e783360c811449d8692b2117acc9212";
    /**
     * get StsToken stats
     */
    private boolean inRequest;

    private static String[] PERMISSIONS_STORAGE = {
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"};

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    /**
     * 当前tab
     */
    private int currentTab = TAB_VIDEO_LIST;
    private static final int TAB_VIDEO_LIST = 1;
    private static final int TAB_LOG = 2;
    private static final int TAB_DOWNLOAD_LIST = 3;
    private Commen commenUtils;
    private long oldTime;
    /**
     * 当前点击的视频列表的下标
     */
    private int currentVidItemPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (isStrangePhone()) {
            //            setTheme(R.style.ActTheme);
        } else {
            setTheme(R.style.NoActionTheme);
        }
        copyAssets();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.alivc_player_layout_skin);

        requestVidSts();
        dbHelper = DownloadDBHelper.getDownloadHelper(getApplicationContext(), 1);
        initAliyunPlayerView();
        initLogView();
        initDownloadView();
        initVideoListView();

    }

    private void copyAssets() {
        commenUtils = Commen.getInstance(getApplicationContext()).copyAssetsToSD("encrypt", "aliyun");
        commenUtils.setFileOperateCallback(

            new Commen.FileOperateCallback() {
                @Override
                public void onSuccess() {
                    config = new AliyunDownloadConfig();
                    config.setSecretImagePath(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/aliyun/encryptedApp.dat");
                    //        config.setDownloadPassword("123456789");
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test_save/");
                    if (!file.exists()) {
                        file.mkdir();
                    }
                    config.setDownloadDir(file.getAbsolutePath());
                    //设置同时下载个数
                    config.setMaxNums(2);
                    // 获取AliyunDownloadManager对象
                    downloadManager = AliyunDownloadManager.getInstance(getApplicationContext());
                    downloadManager.setDownloadConfig(config);

                    downloadDataProvider = DownloadDataProvider.getSingleton(getApplicationContext());
                    // 更新sts回调
                    downloadManager.setRefreshStsCallback(new MyRefreshStsCallback());
                    // 视频下载的回调
                    downloadManager.setDownloadInfoListener(new MyDownloadInfoListener(downloadView));
                    //
                    downloadViewSetting(downloadView);
                }

                @Override
                public void onFailed(String error) {
                }
            });
    }

    private void initAliyunPlayerView() {
        mAliyunVodPlayerView = (AliyunVodPlayerView)findViewById(R.id.video_view);
        //保持屏幕敞亮
        mAliyunVodPlayerView.setKeepScreenOn(true);
        PlayParameter.PLAY_PARAM_URL = DEFAULT_URL;
        String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test_save_cache";
        mAliyunVodPlayerView.setPlayingCache(false, sdDir, 60 * 60 /*时长, s */, 300 /*大小，MB*/);
        mAliyunVodPlayerView.setTheme(AliyunVodPlayerView.Theme.Blue);
        //mAliyunVodPlayerView.setCirclePlay(true);
        mAliyunVodPlayerView.setAutoPlay(true);

        mAliyunVodPlayerView.setOnPreparedListener(new MyPrepareListener(this));
        mAliyunVodPlayerView.setNetConnectedListener(new MyNetConnectedListener(this));
        mAliyunVodPlayerView.setOnCompletionListener(new MyCompletionListener(this));
        mAliyunVodPlayerView.setOnFirstFrameStartListener(new MyFrameInfoListener(this));
        mAliyunVodPlayerView.setOnChangeQualityListener(new MyChangeQualityListener(this));
        mAliyunVodPlayerView.setOnStoppedListener(new MyStoppedListener(this));
        mAliyunVodPlayerView.setmOnPlayerViewClickListener(new MyPlayViewClickListener());
        mAliyunVodPlayerView.setOrientationChangeListener(new MyOrientationChangeListener(this));
        mAliyunVodPlayerView.setOnUrlTimeExpiredListener(new MyOnUrlTimeExpiredListener(this));
        mAliyunVodPlayerView.setOnShowMoreClickListener(new MyShowMoreClickLisener(this));
        mAliyunVodPlayerView.setOnPlayStateBtnClickListener(new MyPlayStateBtnClickListener(this));
        mAliyunVodPlayerView.setOnSeekCompleteListener(new MySeekCompleteListener(this));
        mAliyunVodPlayerView.setOnSeekStartListener(new MySeekStartListener(this));
        mAliyunVodPlayerView.enableNativeLog();

    }

    /**
     * 请求sts
     */
    private void requestVidSts() {
        if (inRequest) {
            return;
        }
        inRequest = true;
        PlayParameter.PLAY_PARAM_VID = DEFAULT_VID;
        VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, new MyStsListener(this));
    }

    /**
     * 获取播放列表数据
     */
    private void loadPlayList() {

        AlivcPlayListManager.getInstance().fetchPlayList(PlayParameter.PLAY_PARAM_AK_ID,
            PlayParameter.PLAY_PARAM_AK_SECRE,
            PlayParameter.PLAY_PARAM_SCU_TOKEN, new AlivcPlayListManager.PlayListListener() {
                @Override
                public void onPlayList(int code, final ArrayList<AlivcVideoInfo.Video> videos) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (alivcVideoInfos != null && alivcVideoInfos.size() == 0) {
                                alivcVideoInfos.clear();
                                alivcVideoInfos.addAll(videos);
                                alivcPlayListAdapter.notifyDataSetChanged();
                                // 请求sts成功后, 加载播放资源,和视频列表
                                AlivcVideoInfo.Video video = alivcVideoInfos.get(0);
                                PlayParameter.PLAY_PARAM_VID = video.getVideoId();
                                setPlaySource();
                            }

                        }
                    });
                }

            });
    }

    /**
     * init视频列表tab
     */
    private void initVideoListView() {
        tvVideoList = findViewById(R.id.tv_tab_video_list);
        ivVideoList = findViewById(R.id.iv_video_list);
        recyclerView = findViewById(R.id.video_list);
        llVideoList = findViewById(R.id.ll_video_list);
        tvStartSetting = findViewById(R.id.tv_start_player);
        alivcVideoInfos = new ArrayList<AlivcVideoInfo.Video>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        alivcPlayListAdapter = new AlivcPlayListAdapter(this, alivcVideoInfos);

        ivVideoList.setActivated(true);
        llVideoList.setVisibility(View.VISIBLE);
        rlDownloadManagerContent.setVisibility(View.GONE);
        rlLogsContent.setVisibility(View.GONE);

        tvVideoList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTab = TAB_VIDEO_LIST;
                ivVideoList.setActivated(true);
                ivLogs.setActivated(false);
                ivDownloadVideo.setActivated(false);
                downloadView.changeDownloadEditState(false);
                llVideoList.setVisibility(View.VISIBLE);
                rlDownloadManagerContent.setVisibility(View.GONE);
                rlLogsContent.setVisibility(View.GONE);
            }
        });

        recyclerView.setAdapter(alivcPlayListAdapter);

        alivcPlayListAdapter.setOnVideoListItemClick(new AlivcPlayListAdapter.OnVideoListItemClick() {
            @Override
            public void onItemClick(int position) {
                long currentClickTime = System.currentTimeMillis();
                // 防止快速点击
                if (currentVidItemPosition == position && currentClickTime - oldTime <= 2000) {
                    return;
                }
                PlayParameter.PLAY_PARAM_TYPE = "vidsts";
                // 点击视频列表, 切换播放的视频
                changePlaySource(position);
                oldTime = currentClickTime;
                currentVidItemPosition = position;

            }
        });

        // 开启vid和url设置界面
        tvStartSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AliyunPlayerSkinActivity.this, AliyunPlayerSettingActivity.class);
                // 开启时, 默认为vid
                startActivityForResult(intent, CODE_REQUEST_SETTING);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setPlaySource();
        loadPlayList();
    }

    private int currentVideoPosition;

    /**
     * 切换播放资源
     *
     * @param position 需要播放的数据在集合中的下标
     */
    private void changePlaySource(int position) {

        currentVideoPosition = position;

        AlivcVideoInfo.Video video = alivcVideoInfos.get(position);

        changePlayVidSource(video.getVideoId(), video.getTitle());
    }

    /**
     * 播放本地资源
     *
     * @param url
     * @param title
     */
    private void changePlayLocalSource(String url, String title) {
        AliyunLocalSource.AliyunLocalSourceBuilder alsb = new AliyunLocalSource.AliyunLocalSourceBuilder();
        alsb.setSource(url);
        alsb.setTitle(title);
        AliyunLocalSource localSource = alsb.build();
        mAliyunVodPlayerView.setLocalSource(localSource);
    }

    /**
     * 切换播放vid资源
     *
     * @param vid   切换视频的vid
     * @param title 切换视频的title
     */
    private void changePlayVidSource(String vid, String title) {
        AliyunVidSts vidSts = new AliyunVidSts();
        vidSts.setVid(vid);
        vidSts.setAcId(PlayParameter.PLAY_PARAM_AK_ID);
        vidSts.setAkSceret(PlayParameter.PLAY_PARAM_AK_SECRE);
        vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
        vidSts.setTitle(title);
        mAliyunVodPlayerView.setVidSts(vidSts);
        downloadManager.prepareDownloadMedia(vidSts);
    }

    /**
     * init 日志tab
     */
    private void initLogView() {
        tvLogs = (TextView)findViewById(R.id.tv_logs);
        tvTabLogs = (TextView)findViewById(R.id.tv_tab_logs);
        ivLogs = (ImageView)findViewById(R.id.iv_logs);
        llClearLogs = (LinearLayout)findViewById(R.id.ll_clear_logs);
        rlLogsContent = (RelativeLayout)findViewById(R.id.rl_logs_content);

        //日志Tab默认不选择
        ivLogs.setActivated(false);

        //日志清除
        llClearLogs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                logStrs.clear();
                tvLogs.setText("");
            }
        });

        tvTabLogs.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                currentTab = TAB_LOG;
                // TODO: 2018/4/10 show logs contents view
                ivLogs.setActivated(true);
                ivDownloadVideo.setActivated(false);
                ivVideoList.setActivated(false);
                rlLogsContent.setVisibility(View.VISIBLE);
                downloadView.changeDownloadEditState(false);
                rlDownloadManagerContent.setVisibility(View.GONE);
                llVideoList.setVisibility(View.GONE);
            }
        });
    }

    /**
     * init下载(离线视频)tab
     */
    private void initDownloadView() {
        tvTabDownloadVideo = (TextView)findViewById(R.id.tv_tab_download_video);
        ivDownloadVideo = (ImageView)findViewById(R.id.iv_download_video);
        rlDownloadManagerContent = (RelativeLayout)findViewById(R.id.rl_download_manager_content);
        downloadView = (DownloadView)findViewById(R.id.download_view);
        //离线下载Tab默认不选择
        ivDownloadVideo.setActivated(false);
        tvTabDownloadVideo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTab = TAB_DOWNLOAD_LIST;
                // TODO: 2018/4/10 show download content
                ivDownloadVideo.setActivated(true);
                ivLogs.setActivated(false);
                ivVideoList.setActivated(false);
                rlLogsContent.setVisibility(View.GONE);
                llVideoList.setVisibility(View.GONE);
                rlDownloadManagerContent.setVisibility(View.VISIBLE);
                //Drawable drawable = getResources().getDrawable(R.drawable.alivc_new_download);
                //drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());

                updateDownloadTaskTip();
            }
        });
    }

    /**
     * downloadView的配置 里面配置了需要下载的视频的信息, 事件监听等 抽取该方法的主要目的是, 横屏下download dialog的离线视频列表中也用到了downloadView, 而两者显示内容和数据是同步的,
     * 所以在此进行抽取 AliyunPlayerSkinActivity.class#showAddDownloadView(DownloadVie view)中使用
     *
     * @param downloadView
     */
    private void downloadViewSetting(final DownloadView downloadView) {
        downloadView.addAllDownloadMediaInfo(downloadDataProvider.getAllDownloadMediaInfo());

        downloadView.setOnDownloadViewListener(new DownloadView.OnDownloadViewListener() {
            @Override
            public void onStop(AliyunDownloadMediaInfo downloadMediaInfo) {
                downloadManager.stopDownloadMedia(downloadMediaInfo);
            }

            @Override
            public void onStart(AliyunDownloadMediaInfo downloadMediaInfo) {
                downloadManager.startDownloadMedia(downloadMediaInfo);
            }

            @Override
            public void onDeleteDownloadInfo(final ArrayList<AlivcDownloadMediaInfo> alivcDownloadMediaInfos) {
                // 视频删除的dialog
                final AlivcDialog alivcDialog = new AlivcDialog(AliyunPlayerSkinActivity.this);
                alivcDialog.setDialogIcon(R.drawable.icon_delete_tips);
                alivcDialog.setMessage(getResources().getString(R.string.alivc_delete_confirm));
                alivcDialog.setOnConfirmclickListener(getResources().getString(R.string.alivc_dialog_sure),
                    new AlivcDialog.onConfirmClickListener() {
                        @Override
                        public void onConfirm() {
                            alivcDialog.dismiss();
                            if (alivcDownloadMediaInfos != null && alivcDownloadMediaInfos.size() > 0) {
                                downloadView.deleteDownloadInfo();
                                if (dialogDownloadView != null) {

                                    dialogDownloadView.deleteDownloadInfo();
                                }
                                downloadDataProvider.deleteAllDownloadInfo(alivcDownloadMediaInfos);
                            } else {
                                Toast.makeText(AliyunPlayerSkinActivity.this, "没有删除的视频选项...", Toast.LENGTH_SHORT)
                                    .show();
                            }
                        }
                    });
                alivcDialog.setOnCancelOnclickListener(getResources().getString(R.string.alivc_dialog_cancle),
                    new AlivcDialog.onCancelOnclickListener() {
                        @Override
                        public void onCancel() {
                            alivcDialog.dismiss();
                        }
                    });
                alivcDialog.show();
            }
        });

        downloadView.setOnDownloadedItemClickListener(new DownloadView.OnDownloadItemClickListener() {
            @Override
            public void onDownloadedItemClick(int positin) {

                ArrayList<AliyunDownloadMediaInfo> downloadedList = downloadDataProvider.getAllDownloadMediaInfo();
                //// 存入顺序和显示顺序相反,  所以进行倒序

                ArrayList<AliyunDownloadMediaInfo> tempList = new ArrayList<>();
                int size = downloadedList.size();
                for (int i = 0; i < size; i++) {
                    if (downloadedList.get(i).getProgress() == 100) {
                        tempList.add(downloadedList.get(i));
                    }
                }

                Collections.reverse(tempList);
                tempList.add(downloadedList.get(downloadedList.size() - 1));
                for (int i = 0; i < size; i++) {
                    AliyunDownloadMediaInfo downloadMediaInfo = downloadedList.get(i);
                    if (!tempList.contains(downloadMediaInfo)) {
                        tempList.add(downloadMediaInfo);
                    }
                }

                if (positin < 0) {
                    Toast.makeText(AliyunPlayerSkinActivity.this, "视频资源不存在", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 如果点击列表中的视频, 需要将类型改为vid
                AliyunDownloadMediaInfo aliyunDownloadMediaInfo = tempList.get(positin);
                PlayParameter.PLAY_PARAM_TYPE = "localSource";
                if (aliyunDownloadMediaInfo != null) {
                    PlayParameter.PLAY_PARAM_URL = aliyunDownloadMediaInfo.getSavePath();
                    mAliyunVodPlayerView.updateScreenShow();
                    changePlayLocalSource(PlayParameter.PLAY_PARAM_URL, aliyunDownloadMediaInfo.getTitle());
                }
            }

            @Override
            public void onDownloadingItemClick(ArrayList<AlivcDownloadMediaInfo> infos, int position) {
                AlivcDownloadMediaInfo alivcInfo = infos.get(position);
                AliyunDownloadMediaInfo aliyunDownloadInfo = alivcInfo.getAliyunDownloadMediaInfo();
                AliyunDownloadMediaInfo.Status status = aliyunDownloadInfo.getStatus();
                if (status == AliyunDownloadMediaInfo.Status.Error || status == AliyunDownloadMediaInfo.Status.Wait) {
                    //downloadManager.removeDownloadMedia(aliyunDownloadInfo);
                    downloadManager.startDownloadMedia(aliyunDownloadInfo);
                }
            }

        });
    }

    private static class MyPrepareListener implements IAliyunVodPlayer.OnPreparedListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyPrepareListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onPrepared() {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onPrepared();
            }
        }
    }

    private void onPrepared() {
        logStrs.add(format.format(new Date()) + getString(R.string.log_prepare_success));

        for (String log : logStrs) {
            tvLogs.append(log + "\n");
        }
        Toast.makeText(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.toast_prepare_success,
            Toast.LENGTH_SHORT).show();
    }

    private static class MyCompletionListener implements IAliyunVodPlayer.OnCompletionListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyCompletionListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onCompletion() {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onCompletion();
            }
        }
    }

    private void onCompletion() {
        logStrs.add(format.format(new Date()) + getString(R.string.log_play_completion));
        for (String log : logStrs) {
            tvLogs.append(log + "\n");
        }
        Toast.makeText(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.toast_play_compleion,
            Toast.LENGTH_SHORT).show();

        // 当前视频播放结束, 播放下一个视频
        onNext();
    }

    private void onNext() {
        if (currentError == ErrorInfo.UnConnectInternet) {
            // 此处需要判断网络和播放类型
            // 网络资源, 播放完自动波下一个, 无网状态提示ErrorTipsView
            // 本地资源, 播放完需要重播, 显示Replay, 此处不需要处理
            if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
                mAliyunVodPlayerView.showErrorTipView(4014, -1, "当前网络不可用");
            }
            return;
        }

        currentVideoPosition++;
        if (currentVideoPosition >= alivcVideoInfos.size() - 1) {
            //列表循环播放，如发现播放完成了从列表的第一个开始重新播放
            currentVideoPosition = 0;
        }

        if (alivcVideoInfos.size() > 0) {
            AlivcVideoInfo.Video video = alivcVideoInfos.get(currentVideoPosition);
            if (video != null) {
                changePlayVidSource(video.getVideoId(), video.getTitle());
            }
        }

    }

    private static class MyFrameInfoListener implements IAliyunVodPlayer.OnFirstFrameStartListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyFrameInfoListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onFirstFrameStart() {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onFirstFrameStart();
            }
        }
    }

    private void onFirstFrameStart() {
        Map<String, String> debugInfo = mAliyunVodPlayerView.getAllDebugInfo();
        long createPts = 0;
        if (debugInfo.get("create_player") != null) {
            String time = debugInfo.get("create_player");
            createPts = (long)Double.parseDouble(time);
            logStrs.add(format.format(new Date(createPts)) + getString(R.string.log_player_create_success));
        }
        if (debugInfo.get("open-url") != null) {
            String time = debugInfo.get("open-url");
            long openPts = (long)Double.parseDouble(time) + createPts;
            logStrs.add(format.format(new Date(openPts)) + getString(R.string.log_open_url_success));
        }
        if (debugInfo.get("find-stream") != null) {
            String time = debugInfo.get("find-stream");
            long findPts = (long)Double.parseDouble(time) + createPts;
            logStrs.add(format.format(new Date(findPts)) + getString(R.string.log_request_stream_success));
        }
        if (debugInfo.get("open-stream") != null) {
            String time = debugInfo.get("open-stream");
            long openPts = (long)Double.parseDouble(time) + createPts;
            logStrs.add(format.format(new Date(openPts)) + getString(R.string.log_start_open_stream));
        }
        logStrs.add(format.format(new Date()) + getString(R.string.log_first_frame_played));
        for (String log : logStrs) {
            tvLogs.append(log + "\n");
        }
    }

    private class MyPlayViewClickListener implements AliyunVodPlayerView.OnPlayerViewClickListener {
        @Override
        public void onClick(AliyunScreenMode screenMode, AliyunVodPlayerView.PlayViewType viewType) {
            // 如果当前的Type是Download, 就显示Download对话框
            if (viewType == AliyunVodPlayerView.PlayViewType.Download) {
                showAddDownloadView(screenMode);
            }
        }
    }

    /**
     * 显示download 对话框
     *
     * @param screenMode
     */
    private void showAddDownloadView(AliyunScreenMode screenMode) {
        downloadDialog = new DownloadChoiceDialog(this, screenMode);
        final AddDownloadView contentView = new AddDownloadView(this, screenMode);
        contentView.onPrepared(aliyunDownloadMediaInfoList);
        contentView.setOnViewClickListener(viewClickListener);
        final View inflate = LayoutInflater.from(getApplicationContext()).inflate(
            R.layout.alivc_dialog_download_video, null, false);
        dialogDownloadView = inflate.findViewById(R.id.download_view);
        downloadDialog.setContentView(contentView);
        downloadDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
            }
        });
        downloadDialog.show();
        downloadDialog.setCanceledOnTouchOutside(true);

        if (screenMode == AliyunScreenMode.Full) {
            contentView.setOnShowVideoListLisener(new AddDownloadView.OnShowNativeVideoBtnClickListener() {
                @Override
                public void onShowVideo() {
                    downloadViewSetting(dialogDownloadView);
                    downloadDialog.setContentView(inflate);
                }
            });
        }
    }

    private Dialog downloadDialog = null;

    private AliyunDownloadMediaInfo aliyunDownloadMediaInfo;
    private long currentDownloadIndex = 0;
    /**
     * 开始下载的事件监听
     */
    private AddDownloadView.OnViewClickListener viewClickListener = new AddDownloadView.OnViewClickListener() {
        @Override
        public void onCancel() {
            if (downloadDialog != null) {
                downloadDialog.dismiss();
            }
        }

        @Override
        public void onDownload(AliyunDownloadMediaInfo info) {
            if (downloadDialog != null) {
                downloadDialog.dismiss();
            }

            aliyunDownloadMediaInfo = info;

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                int permission = ContextCompat.checkSelfPermission(AliyunPlayerSkinActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permission != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(AliyunPlayerSkinActivity.this, PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE);

                } else {
                    addNewInfo(info);
                }
            } else {
                addNewInfo(info);
            }

        }
    };

    private void addNewInfo(AliyunDownloadMediaInfo info) {
        if (downloadManager != null) {
            downloadManager.addDownloadMedia(info);
            downloadManager.startDownloadMedia(info);
        }
        if (downloadView != null) {
            downloadView.addDownloadMediaInfo(info);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addNewInfo(aliyunDownloadMediaInfo);
            } else {
                // Permission Denied
                Toast.makeText(AliyunPlayerSkinActivity.this, "没有sd卡读写权限, 无法下载", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private class MyDownloadInfoListener implements AliyunDownloadInfoListener {

        private DownloadView downloadView;

        public MyDownloadInfoListener(DownloadView downloadView) {
            this.downloadView = downloadView;
        }

        @Override
        public void onPrepared(List<AliyunDownloadMediaInfo> infos) {
            Collections.sort(infos, new Comparator<AliyunDownloadMediaInfo>() {
                @Override
                public int compare(AliyunDownloadMediaInfo mediaInfo1, AliyunDownloadMediaInfo mediaInfo2) {
                    if (mediaInfo1.getSize() > mediaInfo2.getSize()) {
                        return 1;
                    }
                    if (mediaInfo1.getSize() < mediaInfo2.getSize()) {
                        return -1;
                    }

                    if (mediaInfo1.getSize() == mediaInfo2.getSize()) {
                        return 0;
                    }
                    return 0;
                }
            });
            onDownloadPrepared(infos);
        }

        @Override
        public void onStart(AliyunDownloadMediaInfo info) {
            Toast.makeText(AliyunPlayerSkinActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
            //downloadView.addDownloadMediaInfo(info);
            //dbHelper.insert(info, DownloadDBHelper.DownloadState.STATE_DOWNLOADING);
            if (!downloadDataProvider.hasAdded(info)) {
                updateDownloadTaskTip();
                downloadDataProvider.addDownloadMediaInfo(info);
            }

        }

        @Override
        public void onProgress(AliyunDownloadMediaInfo info, int percent) {
            downloadView.updateInfo(info);
            if (dialogDownloadView != null) {
                dialogDownloadView.updateInfo(info);
            }
        }

        @Override
        public void onStop(AliyunDownloadMediaInfo info) {
            Log.d("yds100", "onStop");
            downloadView.updateInfo(info);
            if (dialogDownloadView != null) {
                dialogDownloadView.updateInfo(info);
            }
            //dbHelper.update(info, DownloadDBHelper.DownloadState.STATE_PAUSE);
        }

        @Override
        public void onCompletion(AliyunDownloadMediaInfo info) {
            Log.d("yds100", "onCompletion");
            downloadView.updateInfoByComplete(info);
            if (dialogDownloadView != null) {
                dialogDownloadView.updateInfoByComplete(info);
            }
            downloadDataProvider.addDownloadMediaInfo(info);
            //aliyunDownloadMediaInfoList.remove(info);
        }

        @Override
        public void onError(AliyunDownloadMediaInfo info, int code, String msg, String requestId) {
            Log.d("yds100", "onError" + msg);
            downloadView.updateInfoByError(info);
            if (dialogDownloadView != null) {
                dialogDownloadView.updateInfoByError(info);
            }
            Message message = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString(DOWNLOAD_ERROR_KEY, msg);
            message.setData(bundle);
            message.what = DOWNLOAD_ERROR;
            playerHandler = new PlayerHandler(AliyunPlayerSkinActivity.this);
            playerHandler.sendMessage(message);
        }

        @Override
        public void onWait(AliyunDownloadMediaInfo outMediaInfo) {
            Log.d("yds100", "onWait");
        }

        @Override
        public void onM3u8IndexUpdate(AliyunDownloadMediaInfo outMediaInfo, int index) {
            Log.d("yds100", "onM3u8IndexUpdate");
        }
    }

    List<AliyunDownloadMediaInfo> aliyunDownloadMediaInfoList;

    private void onDownloadPrepared(List<AliyunDownloadMediaInfo> infos) {
        aliyunDownloadMediaInfoList = new ArrayList<>();
        aliyunDownloadMediaInfoList.addAll(infos);
    }

    private static class MyChangeQualityListener implements IAliyunVodPlayer.OnChangeQualityListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyChangeQualityListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onChangeQualitySuccess(String finalQuality) {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onChangeQualitySuccess(finalQuality);
            }
        }

        @Override
        public void onChangeQualityFail(int code, String msg) {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onChangeQualityFail(code, msg);
            }
        }
    }

    private void onChangeQualitySuccess(String finalQuality) {
        logStrs.add(format.format(new Date()) + getString(R.string.log_change_quality_success));
        Toast.makeText(AliyunPlayerSkinActivity.this.getApplicationContext(),
            getString(R.string.log_change_quality_success), Toast.LENGTH_SHORT).show();
    }

    void onChangeQualityFail(int code, String msg) {
        logStrs.add(format.format(new Date()) + getString(R.string.log_change_quality_fail) + " : " + msg);
        Toast.makeText(AliyunPlayerSkinActivity.this.getApplicationContext(),
            getString(R.string.log_change_quality_fail), Toast.LENGTH_SHORT).show();
    }

    private static class MyStoppedListener implements IAliyunVodPlayer.OnStoppedListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyStoppedListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onStopped() {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onStopped();
            }
        }
    }

    private static class MyRefreshStsCallback implements AliyunRefreshStsCallback {

        @Override
        public AliyunVidSts refreshSts(String vid, String quality, String format, String title, boolean encript) {
            VcPlayerLog.d("refreshSts ", "refreshSts , vid = " + vid);
            //NOTE: 注意：这个不能启动线程去请求。因为这个方法已经在线程中调用了。
            AliyunVidSts vidSts = VidStsUtil.getVidSts(vid);
            if (vidSts == null) {
                return null;
            } else {
                vidSts.setVid(vid);
                vidSts.setQuality(quality);
                vidSts.setTitle(title);
                return vidSts;
            }
        }
    }

    private void onStopped() {
        Toast.makeText(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.log_play_stopped,
            Toast.LENGTH_SHORT).show();
    }

    private void setPlaySource() {
        if ("localSource".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            AliyunLocalSource.AliyunLocalSourceBuilder alsb = new AliyunLocalSource.AliyunLocalSourceBuilder();
            alsb.setSource(PlayParameter.PLAY_PARAM_URL);
            Uri uri = Uri.parse(PlayParameter.PLAY_PARAM_URL);
            if ("rtmp".equals(uri.getScheme())) {
                alsb.setTitle("");
            }
            AliyunLocalSource localSource = alsb.build();
            if (mAliyunVodPlayerView != null) {
                mAliyunVodPlayerView.setLocalSource(localSource);
            }

        } else if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            if (!inRequest) {
                AliyunVidSts vidSts = new AliyunVidSts();
                vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
                vidSts.setAcId(PlayParameter.PLAY_PARAM_AK_ID);
                vidSts.setAkSceret(PlayParameter.PLAY_PARAM_AK_SECRE);
                vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
                if (mAliyunVodPlayerView != null) {
                    mAliyunVodPlayerView.setVidSts(vidSts);
                }
                downloadManager.prepareDownloadMedia(vidSts);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePlayerViewMode();
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.onStop();
        }

        if (downloadManager != null && downloadDataProvider != null) {
            downloadManager.stopDownloadMedias(downloadDataProvider.getAllDownloadMediaInfo());
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updatePlayerViewMode();
    }

    private void updateDownloadTaskTip() {
        if (currentTab != TAB_DOWNLOAD_LIST) {

            Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.alivc_download_new_task);
            drawable.setBounds(0, 0, 20, 20);
            tvTabDownloadVideo.setCompoundDrawablePadding(-20);
            tvTabDownloadVideo.setCompoundDrawables(null, null, drawable, null);
        } else {
            tvTabDownloadVideo.setCompoundDrawables(null, null, null, null);
        }
    }

    private void updatePlayerViewMode() {
        if (mAliyunVodPlayerView != null) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                //转为竖屏了。
                //显示状态栏
                //                if (!isStrangePhone()) {
                //                    getSupportActionBar().show();
                //                }

                this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                mAliyunVodPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                //设置view的布局，宽高之类
                LinearLayout.LayoutParams aliVcVideoViewLayoutParams = (LinearLayout.LayoutParams)mAliyunVodPlayerView
                    .getLayoutParams();
                aliVcVideoViewLayoutParams.height = (int)(ScreenUtils.getWidth(this) * 9.0f / 16);
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                //                if (!isStrangePhone()) {
                //                    aliVcVideoViewLayoutParams.topMargin = getSupportActionBar().getHeight();
                //                }

            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //转到横屏了。
                //隐藏状态栏
                if (!isStrangePhone()) {
                    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    mAliyunVodPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }

                //设置view的布局，宽高
                LinearLayout.LayoutParams aliVcVideoViewLayoutParams = (LinearLayout.LayoutParams)mAliyunVodPlayerView
                    .getLayoutParams();
                aliVcVideoViewLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                //                if (!isStrangePhone()) {
                //                    aliVcVideoViewLayoutParams.topMargin = 0;
                //                }
            }

        }
    }

    @Override
    protected void onDestroy() {
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.onDestroy();
            mAliyunVodPlayerView = null;
        }

        if (playerHandler != null) {
            playerHandler.removeMessages(DOWNLOAD_ERROR);
            playerHandler = null;
        }

        if (commenUtils != null) {
            commenUtils.onDestroy();
            commenUtils = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAliyunVodPlayerView != null) {
            boolean handler = mAliyunVodPlayerView.onKeyDown(keyCode, event);
            if (!handler) {
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //解决某些手机上锁屏之后会出现标题栏的问题。
        updatePlayerViewMode();
    }

    private static final int DOWNLOAD_ERROR = 1;
    private static final String DOWNLOAD_ERROR_KEY = "error_key";

    private static class PlayerHandler extends Handler {
        //持有弱引用AliyunPlayerSkinActivity,GC回收时会被回收掉.
        private final WeakReference<AliyunPlayerSkinActivity> mActivty;

        public PlayerHandler(AliyunPlayerSkinActivity activity) {
            mActivty = new WeakReference<AliyunPlayerSkinActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AliyunPlayerSkinActivity activity = mActivty.get();
            super.handleMessage(msg);
            if (activity != null) {
                switch (msg.what) {
                    case DOWNLOAD_ERROR:
                        //Toast.makeText(mActivty.get(), msg.getData().getString(DOWNLOAD_ERROR_KEY), Toast.LENGTH_LONG)
                        //    .show();
                        Log.d("donwload", msg.getData().getString(DOWNLOAD_ERROR_KEY));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static class MyStsListener implements VidStsUtil.OnStsResultListener {

        private WeakReference<AliyunPlayerSkinActivity> weakctivity;

        MyStsListener(AliyunPlayerSkinActivity act) {
            weakctivity = new WeakReference<AliyunPlayerSkinActivity>(act);
        }

        @Override
        public void onSuccess(String vid, String akid, String akSecret, String token) {
            AliyunPlayerSkinActivity activity = weakctivity.get();
            if (activity != null) {
                activity.onStsSuccess(vid, akid, akSecret, token);
            }
        }

        @Override
        public void onFail() {
            AliyunPlayerSkinActivity activity = weakctivity.get();
            if (activity != null) {
                activity.onStsFail();
            }
        }
    }

    private void onStsFail() {

        Toast.makeText(getApplicationContext(), R.string.request_vidsts_fail, Toast.LENGTH_LONG).show();
        inRequest = false;
        //finish();
    }

    private void onStsSuccess(String mVid, String akid, String akSecret, String token) {
        PlayParameter.PLAY_PARAM_VID = mVid;
        PlayParameter.PLAY_PARAM_AK_ID = akid;
        PlayParameter.PLAY_PARAM_AK_SECRE = akSecret;
        PlayParameter.PLAY_PARAM_SCU_TOKEN = token;

        inRequest = false;

        // 视频列表数据为0时, 加载列表
        if (alivcVideoInfos != null && alivcVideoInfos.size() == 0) {
            alivcVideoInfos.clear();
            loadPlayList();
        }
    }

    private static class MyOrientationChangeListener implements AliyunVodPlayerView.OnOrientationChangeListener {

        private final WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOrientationChangeListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void orientationChange(boolean from, AliyunScreenMode currentMode) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            activity.hideDownloadDialog(from, currentMode);
            activity.hideShowMoreDialog(from, currentMode);
        }
    }

    private void hideShowMoreDialog(boolean from, AliyunScreenMode currentMode) {
        if (showMoreDialog != null) {
            if (currentMode == AliyunScreenMode.Small) {
                showMoreDialog.dismiss();
                currentScreenMode = currentMode;
            }
        }
    }

    private void hideDownloadDialog(boolean from, AliyunScreenMode currentMode) {

        if (downloadDialog != null) {
            if (currentScreenMode != currentMode) {
                downloadDialog.dismiss();
                currentScreenMode = currentMode;
            }
        }
    }

    /**
     * 判断是否有网络的监听
     */
    private class MyNetConnectedListener implements AliyunVodPlayerView.NetConnectedListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyNetConnectedListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onReNetConnected(boolean isReconnect) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onReNetConnected(isReconnect);
            }
        }

        @Override
        public void onNetUnConnected() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onNetUnConnected();
            }
        }
    }

    private void onNetUnConnected() {
        currentError = ErrorInfo.UnConnectInternet;
        if (aliyunDownloadMediaInfoList != null && aliyunDownloadMediaInfoList.size() > 0) {
            downloadManager.stopDownloadMedias(aliyunDownloadMediaInfoList);
        }
    }

    private void onReNetConnected(boolean isReconnect) {
        currentError = ErrorInfo.Normal;
        if (isReconnect) {
            if (aliyunDownloadMediaInfoList != null && aliyunDownloadMediaInfoList.size() > 0) {
                int unCompleteDownload = 0;
                for (AliyunDownloadMediaInfo info : aliyunDownloadMediaInfoList) {
                    if (info.getStatus() == AliyunDownloadMediaInfo.Status.Stop) {
                        unCompleteDownload++;
                    }
                }

                if (unCompleteDownload > 0) {
                    Toast.makeText(this, "网络恢复, 请手动开启下载任务...", Toast.LENGTH_SHORT).show();
                }
            }
            // 如果当前播放列表为空, 网络重连后需要重新请求sts和播放列表, 其他情况不需要
            if (alivcVideoInfos != null && alivcVideoInfos.size() == 0) {
                VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, new MyStsListener(this));
            }
        }
    }

    private static class MyOnUrlTimeExpiredListener implements IAliyunVodPlayer.OnUrlTimeExpiredListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnUrlTimeExpiredListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<AliyunPlayerSkinActivity>(activity);
        }

        @Override
        public void onUrlTimeExpired(String s, String s1) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            activity.onUrlTimeExpired(s, s1);
        }
    }

    private void onUrlTimeExpired(String oldVid, String oldQuality) {
        //requestVidSts();
        AliyunVidSts vidSts = VidStsUtil.getVidSts(oldVid);
        PlayParameter.PLAY_PARAM_VID = vidSts.getVid();
        PlayParameter.PLAY_PARAM_AK_SECRE = vidSts.getAkSceret();
        PlayParameter.PLAY_PARAM_AK_ID = vidSts.getAcId();
        PlayParameter.PLAY_PARAM_SCU_TOKEN = vidSts.getSecurityToken();

    }

    private static class MyShowMoreClickLisener implements ControlView.OnShowMoreClickListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MyShowMoreClickLisener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void showMore() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            activity.showMore(activity);
        }
    }

    private void showMore(final AliyunPlayerSkinActivity activity) {
        showMoreDialog = new AlivcShowMoreDialog(activity);
        AliyunShowMoreValue moreValue = new AliyunShowMoreValue();
        moreValue.setSpeed(mAliyunVodPlayerView.getCurrentSpeed());
        moreValue.setVolume(mAliyunVodPlayerView.getCurrentVolume());
        moreValue.setScreenBrightness(mAliyunVodPlayerView.getCurrentScreenBrigtness());

        ShowMoreView showMoreView = new ShowMoreView(activity, moreValue);
        showMoreDialog.setContentView(showMoreView);
        showMoreDialog.show();
        showMoreView.setOnDownloadButtonClickListener(new ShowMoreView.OnDownloadButtonClickListener() {
            @Override
            public void onDownloadClick() {
                // 点击下载
                showMoreDialog.dismiss();
                if (!"vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
                    Toast.makeText(activity, "Url类型不支持下载", Toast.LENGTH_SHORT).show();
                    return;
                }
                showAddDownloadView(AliyunScreenMode.Full);
            }
        });

        showMoreView.setOnScreenCastButtonClickListener(new ShowMoreView.OnScreenCastButtonClickListener() {
            @Override
            public void onScreenCastClick() {
                Toast.makeText(AliyunPlayerSkinActivity.this, "功能开发中, 敬请期待...", Toast.LENGTH_SHORT).show();
            }
        });

        showMoreView.setOnBarrageButtonClickListener(new ShowMoreView.OnBarrageButtonClickListener() {
            @Override
            public void onBarrageClick() {
                Toast.makeText(AliyunPlayerSkinActivity.this, "功能开发中, 敬请期待...", Toast.LENGTH_SHORT).show();
            }
        });

        showMoreView.setOnSpeedCheckedChangedListener(new ShowMoreView.OnSpeedCheckedChangedListener() {
            @Override
            public void onSpeedChanged(RadioGroup group, int checkedId) {
                // 点击速度切换
                if (checkedId == R.id.rb_speed_normal) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.One);
                } else if (checkedId == R.id.rb_speed_onequartern) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.OneQuartern);
                } else if (checkedId == R.id.rb_speed_onehalf) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.OneHalf);
                } else if (checkedId == R.id.rb_speed_twice) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.Twice);
                }

            }
        });

        // 亮度seek
        showMoreView.setOnLightSeekChangeListener(new ShowMoreView.OnLightSeekChangeListener() {
            @Override
            public void onStart(SeekBar seekBar) {

            }

            @Override
            public void onProgress(SeekBar seekBar, int progress, boolean fromUser) {
                mAliyunVodPlayerView.setCurrentScreenBrigtness(progress);
            }

            @Override
            public void onStop(SeekBar seekBar) {

            }
        });

        showMoreView.setOnVoiceSeekChangeListener(new ShowMoreView.OnVoiceSeekChangeListener() {
            @Override
            public void onStart(SeekBar seekBar) {

            }

            @Override
            public void onProgress(SeekBar seekBar, int progress, boolean fromUser) {
                mAliyunVodPlayerView.setCurrentVolume(progress);
            }

            @Override
            public void onStop(SeekBar seekBar) {

            }
        });

    }

    /**
     * 获取url的scheme
     *
     * @param url
     * @return
     */
    private String getUrlScheme(String url) {
        return Uri.parse(url).getScheme();
    }

    private static class MyPlayStateBtnClickListener implements AliyunVodPlayerView.OnPlayStateBtnClickListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MyPlayStateBtnClickListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onPlayBtnClick(IAliyunVodPlayer.PlayerState playerState) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onPlayStateSwitch(playerState);
            }

        }
    }

    /**
     * 播放状态切换
     *
     * @param playerState
     */
    private void onPlayStateSwitch(IAliyunVodPlayer.PlayerState playerState) {
        if (playerState == IAliyunVodPlayer.PlayerState.Started) {
            tvLogs.append(format.format(new Date()) + " 暂停 \n");
        } else if (playerState == IAliyunVodPlayer.PlayerState.Paused) {
            tvLogs.append(format.format(new Date()) + " 开始 \n");
        }

    }

    private static class MySeekCompleteListener implements IAliyunVodPlayer.OnSeekCompleteListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MySeekCompleteListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSeekComplete() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onSeekComplete();
            }
        }
    }

    private void onSeekComplete() {
        tvLogs.append(format.format(new Date()) + getString(R.string.log_seek_completed) + "\n");
    }

    private static class MySeekStartListener implements AliyunVodPlayerView.OnSeekStartListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MySeekStartListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSeekStart() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onSeekStart();
            }
        }
    }

    private void onSeekStart() {
        tvLogs.append(format.format(new Date()) + getString(R.string.log_seek_start) + "\n");
    }
}
