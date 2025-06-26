package com.gsoft.inventory;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;

/**
 * @author 10904
 */
public class CustomProgressDialog extends Dialog {
    @BindView(R.id.dialogProgressBar)
    ProgressBar dialogProgressBar;
    @BindView(R.id.dialogMessageTextView)
    TextView dialogMessageTextView;
    public CustomProgressDialog(Context context) {
        super(context);
        this.init();
    }
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       this.init();
    }

    private void init() {
        setContentView(R.layout.dialog_progress);
        dialogProgressBar = findViewById(R.id.dialogProgressBar);
        dialogProgressBar.setMax(100);
        dialogMessageTextView = findViewById(R.id.dialogMessageTextView);
        if (dialogMessageTextView == null) {
            Log.e("CustomProgressDialog", "messageTextView is null");
        }
    }
    public void setMessage(String message) {
        if (dialogMessageTextView != null) {
            dialogMessageTextView.setText(message);
        } else {
            Log.e("CustomProgressDialog", "messageTextView is still null");
        }
    }
    @Override
    public void show() {
        super.show();
        // 防止用户点击外部区域关闭对话框
        setCancelable(false);
    }


    public void setProgress(int i) {
        if (dialogProgressBar != null) {
            dialogProgressBar.setProgress(i);
            dialogProgressBar.invalidate();
        } else {
            Log.e("CustomProgressDialog", "dialogProgressBar is still null");
        }
    }
}