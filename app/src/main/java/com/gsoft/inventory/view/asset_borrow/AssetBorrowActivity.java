package com.gsoft.inventory.view.asset_borrow;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_borrow.AssetBorrowAdapter;
import com.gsoft.inventory.entities.AssetBorrow;
import com.gsoft.inventory.service.asset_borrow.AssetBorrowTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * @author 10904
 * 资产借用
 */
public class AssetBorrowActivity  extends BaseCompatActivity {

    private QMUITopBar topBar;
    private RecyclerView rvAssetReceive;
    private AssetBorrowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_borrow);


        bindViews();
        initializeView();
    }
    @Override
    public void initializeView() {
        rvAssetReceive.setLayoutManager(new LinearLayoutManager(this));
        // 标题栏
        topBar.setTitle("资产借用");
        topBar.addRightTextButton("借用", R.id.topbar_right_about_button).setOnClickListener(v -> {
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
        AssetBorrowTask task = new AssetBorrowTask(yhbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetBorrow> data = result.getData();
                    adapter = new AssetBorrowAdapter(this, data);
                    adapter.setOnItemClickListener(this::cardItemClick);
                    rvAssetReceive.setAdapter(adapter);
                } else {
                    showShortText(result.getError().getMessage());
                }
            });
        });
    }


    private void cardItemClick(AssetBorrow item){
        Intent intent = new Intent(AssetBorrowActivity.this, AssetBorrowDetailActivity.class);
        intent.putExtra("DOCID", item.getDOCID());
        startActivity(intent);
    }

}
