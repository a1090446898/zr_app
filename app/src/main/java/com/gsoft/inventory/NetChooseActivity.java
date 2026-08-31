package com.gsoft.inventory;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

/**
 * @author 10904
 */
public class NetChooseActivity extends BaseCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_choose);

        // 检查是否是从通知栏进入
        Intent intent = getIntent();
        if (intent != null) {
            boolean isFromNotification = intent.getBooleanExtra("is_from_notification", false);
            String notificationSource = intent.getStringExtra("notification_source");

            // 将这些信息存储到应用全局状态，供后续使用
            SharedPreferences prefs = getSharedPreferences("app_state", MODE_PRIVATE);
            prefs.edit().putString("notification_source", notificationSource).apply();
        }

        initializeView();
    }


    @Override
    public void initializeView() {
        String offlineLogin = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_OFFLINE_LOGIN, "在线");

        if("在线".equals(offlineLogin)){

            SysConfig.runtimeModel = SysDefine.RuntimeModel.Online;
            Intent intent = new Intent(mContext, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        QMUITopBar qmuiTopBar = findViewById(R.id.topbar);
        qmuiTopBar.setTitle("选择版本环境");
        QMUIRoundButton buttonNetWork = findViewById(R.id.button_network);
        QMUIRoundButton buttonOffline = findViewById(R.id.button_offline);
        buttonNetWork.setOnClickListener(view -> {
            SysConfig.runtimeModel = SysDefine.RuntimeModel.Online;

            Intent intent = new Intent(mContext, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });


        buttonOffline.setOnClickListener(view -> {
            SysConfig.runtimeModel = SysDefine.RuntimeModel.Online;
            // 重定向跳转：清除现有任务栈并创建新任务栈
            Intent intent = new Intent(mContext, OfflineLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }


}