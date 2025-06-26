package com.gsoft.inventory.view.dress_manage;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_borrow.AssetBorrowAdapter;
import com.gsoft.inventory.adapter.dress_manage.DressManageAdapter;
import com.gsoft.inventory.entities.DressManage;
import com.gsoft.inventory.entities.DressManage;
import com.gsoft.inventory.service.dress_manage.DressManageTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * @author 10904
 * 资产借用
 */
public class DressManageActivity extends BaseCompatActivity {

    private QMUITopBar topBar;
    private RecyclerView rvAssetReceive;
    private DressManageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dress_manage);

        bindViews();
        initializeView();
    }
    @Override
    public void initializeView() {
        rvAssetReceive.setLayoutManager(new LinearLayoutManager(this));
        // 标题栏
        topBar.setTitle("服饰管理");
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
        DressManageTask task = new DressManageTask(yhbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<DressManage> data = result.getData();
                    adapter = new DressManageAdapter(this, data);
                    adapter.setOnItemClickListener(this::cardItemClick);
                    rvAssetReceive.setAdapter(adapter);
                } else {
                    showShortText(result.getError().getMessage());
                }
            });
        });
    }


    private void cardItemClick(DressManage item){
        Intent intent = new Intent(DressManageActivity.this, DressManageDetailActivity.class);
        intent.putExtra("DOCID", item.getDocId());
        startActivity(intent);
    }

}
