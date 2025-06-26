package com.gsoft.inventory.view.asset_receive;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_receive.AssetReceiveAdapter;
import com.gsoft.inventory.adapter.asset_receive.AssetReceiveDetailAdapter;
import com.gsoft.inventory.entities.AssetReceive;
import com.gsoft.inventory.entities.AssetReceiveDetailItem;
import com.gsoft.inventory.service.asset_receive.AssetReceiveDetailTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 10904
 */
public class AssetReceiveDetailActivity extends BaseCompatActivity {

    private QMUITopBar topBar;

    private boolean isCheckedAll = false;

    private RecyclerView rvAssetList;
    private AssetReceiveDetailAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_receive_detail);
        bindViews();
        initializeView();
        // 设置按钮事件
        setButtonListeners();
    }

    @Override
    public void initializeView() {

        // 标题栏
        topBar.setTitle("资产领用详情");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String docId = extras.getString("DOCID");
            getData(docId);
        }
    }

    private void bindViews(){
        topBar = findViewById(R.id.topbar);
        rvAssetList = findViewById(R.id.rv_asset_list);
    }



    private void setButtonListeners() {
        topBar.addLeftTextButton("全选", R.id.qmuidemo).setOnClickListener(v -> {
            isCheckedAll = !isCheckedAll;
            adapter.selectAll(isCheckedAll);
        });

        topBar.addRightTextButton("删除", R.id.topbar_right_query_button).setOnClickListener(v -> {
            adapter.removeSelected();
        });
    }

    private void getData (String docId) {
        // 获取数据
        showProgressDialog("", "正在获取数据");
        AssetReceiveDetailTask task = new AssetReceiveDetailTask(docId);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetReceiveDetailItem> data = result.getData();
                    adapter = new AssetReceiveDetailAdapter(data);
                    // 通过findViewById获取的rvAssetList设置布局管理器和适配器
                    rvAssetList.setLayoutManager(new LinearLayoutManager(this));
                    rvAssetList.setAdapter(adapter);
                } else {
                    showShortText(result.getError().getMessage());
                }
            });
        });
    }
}
