package com.github.qingmei2.theme;

import com.github.qingmei2.widget.AliyunVodPlayerView;

/*
 * Copyright (C) 2010-2018 Alibaba Group Holding Limited.
 */

/**
 * 主题的接口。用于变换UI的主题。
 * {@link AliyunVodPlayerView}
 */

public interface ITheme {
    /**
     * 设置主题
     * @param theme 支持的主题
     */
    void setTheme(AliyunVodPlayerView.Theme theme);
}
