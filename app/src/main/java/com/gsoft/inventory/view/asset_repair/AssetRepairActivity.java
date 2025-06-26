package com.gsoft.inventory.view.asset_repair;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_repair.AssetRepairAdapter;
import com.gsoft.inventory.entities.AssetRepair;
import com.gsoft.inventory.service.asset_repair.AssetRepairTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * @author 10904
 * 资产报修
 */
public class AssetRepairActivity extends BaseCompatActivity {

    private QMUITopBar topBar;
    private RecyclerView rvAssetReceive;
    private AssetRepairAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_repair);

        bindViews();
        initializeView();
    }

    @Override
    public void initializeView() {
        rvAssetReceive.setLayoutManager(new LinearLayoutManager(this));
        // 标题栏
        topBar.setTitle("资产报修");
        topBar.addRightTextButton("报修", R.id.topbar_right_about_button).setOnClickListener(v -> {
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
        AssetRepairTask task = new AssetRepairTask(yhbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetRepair> data = result.getData();
                    adapter = new AssetRepairAdapter(this, data);
                    adapter.setOnItemClickListener(this::cardItemClick);
                    rvAssetReceive.setAdapter(adapter);
                } else {
                    showShortText(result.getError().getMessage());
                }
            });
        });
    }


    private void cardItemClick(AssetRepair item){
        Intent intent = new Intent(AssetRepairActivity.this, AssetRepairDetailActivity.class);
        intent.putExtra("DOCID", item.getFID());
        startActivity(intent);
    }

}
