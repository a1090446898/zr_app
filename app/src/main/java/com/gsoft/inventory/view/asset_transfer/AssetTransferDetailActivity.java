package com.gsoft.inventory.view.asset_transfer;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_borrow.AssetBorrowDetailAdapter;
import com.gsoft.inventory.adapter.asset_transfer.AssetTransferDetailAdapter;
import com.gsoft.inventory.entities.AssetBorrowDetailItem;
import com.gsoft.inventory.entities.AssetTransferDetailItem;
import com.gsoft.inventory.service.asset_borrow.AssetBorrowDetailTask;
import com.gsoft.inventory.service.asset_transfer.AssetTransferDetailTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * @author 10904
 */
public class AssetTransferDetailActivity extends BaseCompatActivity {

    private QMUITopBar topBar;

    private boolean isCheckedAll = false;

    private RecyclerView rvAssetList;
    private AssetTransferDetailAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_transfer_detail);
        bindViews();
        initializeView();
        // 设置按钮事件
        setButtonListeners();
    }

    @Override
    public void initializeView() {

        // 标题栏
        topBar.setTitle("资产移交详情");

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
        AssetTransferDetailTask task = new AssetTransferDetailTask(docId);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetTransferDetailItem> data = result.getData();
                    adapter = new AssetTransferDetailAdapter(data);
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
