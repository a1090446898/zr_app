package com.gsoft.inventory;


import android.content.Intent;
import android.os.Bundle;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

public class NetChooseActivity extends BaseCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_choose);

        initializeView();
    }


    @Override
    public void initializeView() {
        QMUITopBar qmuiTopBar = findViewById(R.id.topbar);
        qmuiTopBar.setTitle("选择版本环境");
        QMUIRoundButton buttonNetWork = findViewById(R.id.button_network);
        QMUIRoundButton buttonOffline = findViewById(R.id.button_offline);
        buttonNetWork.setOnClickListener(view -> {
            SysConfig.runtimeModel = SysDefine.RuntimeModel.Online;
            startActivity(new Intent(mContext, LoginActivity.class));
        });
        buttonOffline.setOnClickListener(view -> {
            SysConfig.runtimeModel = SysDefine.RuntimeModel.Offline;
            startActivity(new Intent(mContext, OfflineLoginActivity.class));
        });
    }
}