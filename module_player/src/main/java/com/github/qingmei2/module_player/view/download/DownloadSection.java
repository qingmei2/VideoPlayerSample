package com.github.qingmei2.module_player.view.download;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.RelativeLayout.LayoutParams;
import com.aliyun.vodplayer.downloader.AliyunDownloadMediaInfo;
import com.github.qingmei2.module_player.R;
import com.github.qingmei2.module_player.view.sectionlist.SectionParameters;
import com.github.qingmei2.module_player.view.sectionlist.StatelessSection;
import com.qingmei2.rhine.image.GlideApp;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * @author Mulberry
 *         create on 2018/4/12.
 */

public class DownloadSection extends StatelessSection {

    public static final String DOWNLOADED_TAG = "DownloadedTag";
    public static final String DOWNLOADING_TAG = "DownloadingTag";

    private ArrayList<AlivcDownloadMediaInfo> alivcDownloadMediaInfos;
    private final String title;
    private final String tag;
    private WeakReference<Context> context;

    public DownloadSection(Context context, String tag, String title,
                           ArrayList<AlivcDownloadMediaInfo> alivcDownloadMediaInfos) {
        super(SectionParameters.builder()
            .itemResourceId(R.layout.alivc_download_item)
            .headerResourceId(R.layout.alivc_download_section_item)
            .build());
        this.context = new WeakReference<Context>(context);
        this.tag = tag;
        this.title = title;
        this.alivcDownloadMediaInfos = alivcDownloadMediaInfos;
    }

    @Override
    public ViewHolder getHeaderViewHolder(View view) {
        return new SectionItemViewHolder(view);
    }

    @Override
    public ViewHolder getItemViewHolder(View view) {
        return new DownloadInfoItemViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(ViewHolder holder) {
        final SectionItemViewHolder headerHolder = (SectionItemViewHolder) holder;
        headerHolder.tvSectionItemTitle.setText(title);
    }

    @Override
    public void onBindItemViewHolder(ViewHolder holder, int position) {
        final DownloadInfoItemViewHolder itemViewHolder = (DownloadInfoItemViewHolder) holder;
        AliyunDownloadMediaInfo mediaInfo = alivcDownloadMediaInfos.get(position).getAliyunDownloadMediaInfo();
        GlideApp.with(this.context.get())
            .load(mediaInfo.getCoverUrl())
            .centerCrop()
            .placeholder(R.color.alivc_color_player_colorAccent)
            .into(itemViewHolder.ivVideoCover);
        //(new ImageLoader(itemViewHolder.ivVideoCover)).loadAsync(coverUrl);
        itemViewHolder.tvVideoTitle.setText(mediaInfo.getTitle());

        itemViewHolder.cbSelect.setVisibility(alivcDownloadMediaInfos.get(position).isEditState()?View.VISIBLE:View.GONE);
        itemViewHolder.cbSelect.setChecked(alivcDownloadMediaInfos.get(position).isCheckedState());
        AliyunDownloadMediaInfo.Status status = mediaInfo.getStatus();
        if (status == AliyunDownloadMediaInfo.Status.Prepare) {
            //prepare
            itemViewHolder.tvDownloadVideoStats.setText(context.get().getResources().getString(R.string.download_prepare));
        } else if (status == AliyunDownloadMediaInfo.Status.Wait) {
            //wait
            itemViewHolder.tvDownloadVideoStats.setText(context.get().getResources().getString(R.string.download_wait));
        } else if (status == AliyunDownloadMediaInfo.Status.Start) {
            //start
            itemViewHolder.tvDownloadVideoStats.setText(context.get().getResources().getString(R.string.download_downloading));
            itemViewHolder.ivVideoState.setBackgroundResource(R.drawable.alivc_download_pause);
            itemViewHolder.ivVideoState.setVisibility(View.VISIBLE);
        } else if (status == AliyunDownloadMediaInfo.Status.Stop) {
            //stop
            itemViewHolder.tvDownloadVideoStats.setText(context.get().getResources().getString(R.string.download_pause));
            itemViewHolder.ivVideoState.setBackgroundResource(R.drawable.alivc_download_downloading);
            itemViewHolder.ivVideoState.setVisibility(View.VISIBLE);
        } else if (status == AliyunDownloadMediaInfo.Status.Complete){
            //complete
            itemViewHolder.tvDownloadVideoStats.setVisibility(View.GONE);
            itemViewHolder.ivVideoState.setVisibility(View.GONE);
            itemViewHolder.progressDownloadVideo.setVisibility(View.GONE);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            itemViewHolder.tvDownloadVideoTotalSize.setLayoutParams(lp);
        } else if (status == AliyunDownloadMediaInfo.Status.Error) {
            // error
            itemViewHolder.tvDownloadVideoStats.setText(context.get().getResources().getString(R.string.download_error));
            itemViewHolder.ivVideoState.setVisibility(View.VISIBLE);
            itemViewHolder.ivVideoState.setBackgroundResource(R.drawable.alivc_download_downloading);
        }

        itemViewHolder.progressDownloadVideo.setProgress(mediaInfo.getProgress());
        itemViewHolder.tvDownloadVideoTotalSize.setText(formatSize(mediaInfo.getSize()));
        itemViewHolder.llDownloadItemRootView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int adapterPosition = itemViewHolder.getAdapterPosition();
                if (onSectionItemClickListener != null) {
                    onSectionItemClickListener.onItemClick(adapterPosition, tag);
                }
            }
        });
    }

    /**
     * 更新下载进度
     * @param progress
     */
    public void updateProgress(int progress) {

    }

    private String formatSize(long size) {
        int kb = (int) (size / 1024f);
        if (kb < 1024) {
            return kb + "KB";
        }

        int mb = (int) (kb / 1024f);
        return mb + "MB";

    }

    @Override
    public int getContentItemsTotal() {
        return alivcDownloadMediaInfos.size();
    }

    private static class SectionItemViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSectionItemTitle;

        SectionItemViewHolder(View view){
            super(view);
            tvSectionItemTitle = (TextView)view.findViewById(R.id.tv_section_item_title);
        }
    }

    private static class DownloadInfoItemViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout llDownloadItemRootView;
        private CheckBox cbSelect;
        private ImageView ivVideoCover;
        private ImageView ivVideoState;
        private TextView tvVideoTitle;
        private TextView tvDownloadVideoStats;
        private TextView tvDownloadVideoCurrentSpeed;
        private TextView tvDownloadVideoTotalSize;
        private ProgressBar progressDownloadVideo;

        DownloadInfoItemViewHolder(View view){
            super(view);
            llDownloadItemRootView = (LinearLayout)view.findViewById(R.id.ll_download_item_root_view);
            cbSelect = (CheckBox)view.findViewById(R.id.cb_select);
            ivVideoCover = (ImageView)view.findViewById(R.id.iv_video_cover);
            ivVideoState = (ImageView)view.findViewById(R.id.iv_video_state);
            tvVideoTitle = (TextView)view.findViewById(R.id.tv_video_title);
            tvDownloadVideoStats = (TextView)view.findViewById(R.id.tv_download_video_stats);
            tvDownloadVideoCurrentSpeed = (TextView)view.findViewById(R.id.tv_download_video_current_speed);
            tvDownloadVideoTotalSize = (TextView)view.findViewById(R.id.tv_video_total_size);
            progressDownloadVideo = (ProgressBar)view.findViewById(R.id.progress_download_video);
        }
    }


    private OnSectionItemClickListener onSectionItemClickListener;

    public void setOnSectionItemClickListener(
        OnSectionItemClickListener onSectionItemClickListener) {
        this.onSectionItemClickListener = onSectionItemClickListener;
    }
    public interface  OnSectionItemClickListener{
        void onItemClick(int posion, String tag);
    }
}
