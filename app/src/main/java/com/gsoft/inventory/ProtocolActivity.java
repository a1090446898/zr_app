package com.gsoft.inventory;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProtocolActivity extends BaseCompatActivity {

    @BindView(R.id.topbar)
    QMUITopBar topBar;

    @BindView(R.id.textView)
    TextView textView;

    StringBuilder stringBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protocol);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {

        if (SharedPreferencesUtils.getBoolean(mContext, SharedPreferencesUtils.KEY_PROTOCAL, false)) {
            Intent mainIntent = new Intent(mContext, NetChooseActivity.class);
            startActivity(mainIntent);
            finish();
        }

        topBar.setTitle("许可协议");
        topBar.addRightTextButton("同意", R.id.topbar_right_change_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SharedPreferencesUtils.putBoolean(mContext, SharedPreferencesUtils.KEY_PROTOCAL, true);
                        Intent mainIntent = new Intent(mContext, NetChooseActivity.class);
                        startActivity(mainIntent);
                        finish();
                    }
                });
        InputStream inputStream = null;
        InputStreamReader inputreader = null;
        BufferedReader buffreader = null;
        try {
            inputStream = mContext.getResources().getAssets().open("protocal.txt");
            inputreader = new InputStreamReader(inputStream, "utf-8");
            buffreader = new BufferedReader(inputreader);
            String line;
            //分行读取
            while ((line = buffreader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    buffreader.close();
                    inputreader.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        textView.setText(stringBuilder.toString());
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
    }
}
