package com.gsoft.inventory;


import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.gsoft.inventory.adapter.InventoryStatsAdapter;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.List;

import butterknife.BindArray;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 盘点统计：按盘点状态(ZT)统计资产，已盘 ZT=02 / 未盘 ZT!=02(含空)，默认查询已盘
 * @author 10904
 */
public class InventoryStatsActivity extends BaseCompatActivity {

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.tvInventoryFilter)
    TextView tvInventoryFilter;
    @BindArray(R.array.inventoryFilterFields)
    String[] inventoryFilterFields;
    @BindDrawable(R.drawable.icon_right)
    Drawable icon_right;
    @BindView(R.id.listStats)
    ListView listStats;
    @BindView(R.id.tvTotalCount)
    TextView tvTotalCount;

    private InventoryStatsAdapter statsAdapter;
    private int filterIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_stats);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("盘点统计");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        statsAdapter = new InventoryStatsAdapter(mContext);
        listStats.setAdapter(statsAdapter);
        listStats.setOnItemClickListener((parent, view, position, id) -> {
            Assets assets = statsAdapter.getItem(position);
            if (assets != null) {
                Intent intentView = new Intent(mContext, AssetsViewActivity.class);
                intentView.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                startActivity(intentView);
            }
        });

        icon_right.setBounds(0, 0, 60, 60);
        tvInventoryFilter.setOnClickListener(v ->
                new QMUIDialog.CheckableDialogBuilder(mContext)
                        .addItems(inventoryFilterFields, (dialog, which) -> {
                            filterIndex = which;
                            tvInventoryFilter.setText(inventoryFilterFields[which]);
                            queryData();
                            // 隐藏弹框
                            dialog.dismiss();
                        })
                        .show());
        queryData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        queryData();
    }

    private void queryData() {
        List<Assets> result;
        if (filterIndex == 0) {
            // 已盘
            result = Assets.find(Assets.class, "ZT = ?", new String[]{"02"}, null, "Id ASC", "500");
        } else {
            // 未盘（含空值）
            result = Assets.find(Assets.class, "(ZT IS NULL OR ZT != ?)", new String[]{"02"}, null, "Id ASC", "500");
        }
        statsAdapter.setData(result);
        statsAdapter.notifyDataSetChanged();
        tvTotalCount.setText("共 " + result.size() + " 条数据");
    }
}

