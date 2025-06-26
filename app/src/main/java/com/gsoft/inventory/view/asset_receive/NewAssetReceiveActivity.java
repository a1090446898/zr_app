package com.gsoft.inventory.view.asset_receive;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;

/**
 * @author 10904
 */
public class NewAssetReceiveActivity extends BaseCompatActivity {
    private QMUITopBar topBar;
    private Button btnSave;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_receive_new);

        bindViews();
        initializeView();
    }

    @Override
    public void initializeView() {
        if(topBar != null){
            topBar.setTitle("资产领用新增");
        }
        btnSave.setOnClickListener(v -> {
            // 这里可以添加保存逻辑
            Toast.makeText(NewAssetReceiveActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        });
    }


    private void bindViews(){
        topBar = findViewById(R.id.topbar);
        btnSave = findViewById(R.id.btn_save);
    }


}
