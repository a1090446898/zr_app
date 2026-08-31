package com.gsoft.inventory.view.web;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.gsoft.inventory.abs.WebCompatActivity;
import com.gsoft.inventory.entities.BadgeNotificationPojo;
import com.gsoft.inventory.service.login.SyncGetBadgeAndNotification;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BadgeNotificationManager;
import com.gsoft.inventory.utils.StringUtils;

import java.util.List;

import static com.gsoft.inventory.utils.SysDefine.WEB_URL;

/**
 * @author 10904
 */
public class WebIndex extends WebCompatActivity {

    private  String webUrl = WEB_URL + "/pages/404/index";

    private BadgeNotificationManager badgeNotificationManager;

    private Handler badgeHandler;
    private Runnable badgeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeView();
    }

    @Override
    public void initializeView() {

        String tempUrl = getIntent().getStringExtra("webUrl");
        if(!StringUtils.isNullOrEmpty(tempUrl)){
            webUrl = WEB_URL + tempUrl;
        }
        // 加载目标网页
        webView.loadUrl(webUrl);
    }

    /**
     * 扫码页返回的值
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止定时任务
        if (badgeHandler != null && badgeRunnable != null) {
            badgeHandler.removeCallbacks(badgeRunnable);
        }
    }



}
