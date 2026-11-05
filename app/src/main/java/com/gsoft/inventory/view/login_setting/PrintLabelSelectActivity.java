package com.gsoft.inventory.view.login_setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.print_router.PrintLabelCatalog;
import com.qmuiteam.qmui.widget.QMUITabSegment;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.List;

/**
 * 打印标签选择页：标题栏下方横向Tab（通用/宝威/得实），点击标签即选中并返回。
 * <p>
 * 替代设置页/离线登录页原有的弹框选择。进入时按当前标签自动定位Tab并高亮，
 * 选择结果通过 EXTRA_SELECTED_LABEL 返回，调用页继续处理字段重置、警示内容编辑等联动。
 */
public class PrintLabelSelectActivity extends BaseCompatActivity {

    /** 进入时携带的当前标签名称（用于定位Tab与高亮） */
    public static final String EXTRA_CURRENT_LABEL = "EXTRA_CURRENT_LABEL";
    /** 返回时携带的选中标签名称 */
    public static final String EXTRA_SELECTED_LABEL = "EXTRA_SELECTED_LABEL";

    private static final int TEXT_COLOR_NORMAL = 0xFF333333;

    private QMUITabSegment tabSegment;
    private LinearLayout labelContainer;
    private TextView tvEmpty;

    /** 当前标签名称 */
    private String currentLabel;
    /** 当前Tab下标 */
    private int currentGroupIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_label_select);
        currentLabel = getIntent().getStringExtra(EXTRA_CURRENT_LABEL);
        currentGroupIndex = PrintLabelCatalog.getGroupIndex(currentLabel);
        initializeView();
    }

    @Override
    public void initializeView() {
        QMUITopBar topBar = findViewById(R.id.topbar);
        topBar.setTitle("打印标签选择");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        tabSegment = findViewById(R.id.tabSegment);
        labelContainer = findViewById(R.id.labelContainer);
        tvEmpty = findViewById(R.id.tvEmpty);

        initTabs();
    }

    private void initTabs() {
        for (String group : PrintLabelCatalog.GROUPS) {
            tabSegment.addTab(new QMUITabSegment.Tab(group));
        }
        tabSegment.notifyDataChanged();
        tabSegment.addOnTabSelectedListener(new QMUITabSegment.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int index) {
                currentGroupIndex = index;
                renderLabels();
            }

            @Override
            public void onTabUnselected(int index) {
            }

            @Override
            public void onTabReselected(int index) {
            }

            @Override
            public void onDoubleTap(int index) {
            }
        });
        tabSegment.selectTab(currentGroupIndex);
        renderLabels();
    }

    /** 渲染当前Tab的标签列表；得实Tab暂无标签时显示空态 */
    private void renderLabels() {
        labelContainer.removeAllViews();
        String group = PrintLabelCatalog.GROUPS[currentGroupIndex];
        List<String> labels = PrintLabelCatalog.getLabels(this, group);
        if (labels.isEmpty()) {
            tvEmpty.setText("暂无" + group + "标签");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (String label : labels) {
            View item = inflater.inflate(R.layout.item_print_label, labelContainer, false);
            TextView tvName = item.findViewById(R.id.tvLabelName);
            boolean selected = label.equals(currentLabel);
            tvName.setText(label);
            tvName.setTextColor(selected ? getResources().getColor(R.color.primary_color) : TEXT_COLOR_NORMAL);
            item.setOnClickListener(v -> {
                Intent data = new Intent();
                data.putExtra(EXTRA_SELECTED_LABEL, label);
                setResult(RESULT_OK, data);
                finish();
            });
            labelContainer.addView(item);
        }
    }
}