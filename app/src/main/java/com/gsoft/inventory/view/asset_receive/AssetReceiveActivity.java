package com.gsoft.inventory.view.asset_receive;

import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_receive.AssetReceiveAdapter;
import com.gsoft.inventory.entities.AssetReceive;
import com.gsoft.inventory.service.asset_receive.AssetReceiveTask;
import com.gsoft.inventory.service.import_net_assets.SyncServerAssetsTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 10904
 * 资产领用
 */
public class AssetReceiveActivity extends BaseCompatActivity {

    private QMUITopBar topBar;
    private RecyclerView rvAssetReceive;
    private AssetReceiveAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_receive);

        bindViews();
        initializeView();
    }

    @Override
    public void initializeView() {
        rvAssetReceive.setLayoutManager(new LinearLayoutManager(this));
        // 标题栏
        topBar.setTitle("资产领用");
        topBar.addRightTextButton("领用", R.id.topbar_right_about_button).setOnClickListener(v -> {
            /*Intent intent = new Intent(AssetReceiveActivity.this, NewAssetReceiveActivity.class);
            startActivity(intent);*/
        });

        getData();
    }

    private void bindViews()
    {
        rvAssetReceive = findViewById(R.id.rv_asset_receive);
        topBar = findViewById(R.id.topbar);
    }


    private void getData () {
        String yhbh = application.getLoginAccount().getYHBH();
        // 获取数据
        showProgressDialog("", "正在获取数据");
        AssetReceiveTask task = new AssetReceiveTask(yhbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetReceive> data = result.getData();
                    adapter = new AssetReceiveAdapter(this, data);
                    adapter.setOnItemClickListener(this::cardItemClick);
                    rvAssetReceive.setAdapter(adapter);
                } else {
                    showShortText(result.getError().getMessage());
                }
            });
        });
    }


    private void cardItemClick(AssetReceive item){
        Intent intent = new Intent(AssetReceiveActivity.this, AssetReceiveDetailActivity.class);
        intent.putExtra("DOCID", item.getDOCID());
        startActivity(intent);
    }

}
