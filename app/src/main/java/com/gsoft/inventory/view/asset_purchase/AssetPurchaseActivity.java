package com.gsoft.inventory.view.asset_purchase;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_borrow.AssetBorrowAdapter;
import com.gsoft.inventory.adapter.asset_purchase.AssetPurchaseAdapter;
import com.gsoft.inventory.entities.AssetBorrow;
import com.gsoft.inventory.entities.AssetPurchase;
import com.gsoft.inventory.service.asset_borrow.AssetBorrowTask;
import com.gsoft.inventory.service.asset_purchase.AssetPurchaseTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.view.asset_borrow.AssetBorrowActivity;
import com.gsoft.inventory.view.asset_borrow.AssetBorrowDetailActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * @author 10904
 * 采购申请
 */
public class AssetPurchaseActivity extends BaseCompatActivity {

    private QMUITopBar topBar;
    private RecyclerView rvAssetReceive;
    private AssetPurchaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_purchase);


        bindViews();
        initializeView();
    }
    @Override
    public void initializeView() {
        rvAssetReceive.setLayoutManager(new LinearLayoutManager(this));
        // 标题栏
        topBar.setTitle("资产采购");
        topBar.addRightTextButton("采购", R.id.topbar_right_about_button).setOnClickListener(v -> {
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
        AssetPurchaseTask task = new AssetPurchaseTask(yhbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetPurchase> data = result.getData();
                    adapter = new AssetPurchaseAdapter(this, data);
                    adapter.setOnItemClickListener(this::cardItemClick);
                    rvAssetReceive.setAdapter(adapter);
                } else {
                    showShortText(result.getError().getMessage());
                }
            });
        });
    }


    private void cardItemClick(AssetPurchase item){
        Intent intent = new Intent(AssetPurchaseActivity.this, AssetPurchaseDetailActivity.class);
        intent.putExtra("DOCID", item.getDOCID());
        startActivity(intent);
    }
}
