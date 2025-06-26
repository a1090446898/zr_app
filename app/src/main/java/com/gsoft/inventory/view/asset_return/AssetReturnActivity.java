package com.gsoft.inventory.view.asset_return;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.asset_borrow.AssetBorrowDetailAdapter;
import com.gsoft.inventory.adapter.asset_return.AssetReturnAdapter;
import com.gsoft.inventory.entities.AssetBorrowDetailItem;
import com.gsoft.inventory.entities.AssetReturn;
import com.gsoft.inventory.service.asset_borrow.AssetBorrowDetailTask;
import com.gsoft.inventory.service.asset_return.AssetReturnTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * @author 10904
 * 资产归还
 */
public class AssetReturnActivity extends BaseCompatActivity {

    private QMUITopBar topBar;

    private boolean isCheckedAll = false;

    private RecyclerView rvAssetList;
    private AssetReturnAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_return);
        bindViews();
        initializeView();
        // 设置按钮事件
        setButtonListeners();
    }

    @Override
    public void initializeView() {

        // 标题栏
        topBar.setTitle("资产归还");
        getData();


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

        topBar.addRightTextButton("归还", R.id.topbar_right_query_button).setOnClickListener(v -> {
            adapter.removeSelected();
        });
    }

    private void getData () {
        String yhbh = application.getLoginAccount().getYHBH();
        // 获取数据
        showProgressDialog("", "正在获取数据");
        AssetReturnTask task = new AssetReturnTask(yhbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    List<AssetReturn> data = result.getData();
                    adapter = new AssetReturnAdapter(data);
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
