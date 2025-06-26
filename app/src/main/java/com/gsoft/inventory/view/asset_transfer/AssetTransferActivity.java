package com.gsoft.inventory.view.asset_transfer;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_borrow.AssetBorrowAdapter;
import com.gsoft.inventory.adapter.asset_borrow.AssetBorrowDetailAdapter;
import com.gsoft.inventory.adapter.asset_transfer.AssetTransferAdapter;
import com.gsoft.inventory.entities.AssetBorrow;
import com.gsoft.inventory.entities.AssetBorrowDetailItem;
import com.gsoft.inventory.entities.AssetTransfer;
import com.gsoft.inventory.service.asset_borrow.AssetBorrowDetailTask;
import com.gsoft.inventory.service.asset_borrow.AssetBorrowTask;
import com.gsoft.inventory.service.asset_transfer.AssetTransferTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.view.asset_borrow.AssetBorrowActivity;
import com.gsoft.inventory.view.asset_borrow.AssetBorrowDetailActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * @author 10904
 * 资产移交
 */
public class AssetTransferActivity extends BaseCompatActivity {

    private QMUITopBar topBar;
    private RecyclerView rvAssetReceive;
    private AssetTransferAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_transfer);

        bindViews();
        initializeView();
    }

    @Override
    public void initializeView() {
        rvAssetReceive.setLayoutManager(new LinearLayoutManager(this));
        // 标题栏
        topBar.setTitle("资产移交");
        topBar.addRightTextButton("移交", R.id.topbar_right_about_button).setOnClickListener(v -> {
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
        AssetTransferTask task = new AssetTransferTask(yhbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetTransfer> data = result.getData();
                    adapter = new AssetTransferAdapter(this, data);
                    adapter.setOnItemClickListener(this::cardItemClick);
                    rvAssetReceive.setAdapter(adapter);
                } else {
                    showShortText(result.getError().getMessage());
                }
            });
        });
    }


    private void cardItemClick(AssetTransfer item){
        Intent intent = new Intent(AssetTransferActivity.this, AssetTransferDetailActivity.class);
        intent.putExtra("DOCID", item.getDOCID());
        startActivity(intent);
    }

}
