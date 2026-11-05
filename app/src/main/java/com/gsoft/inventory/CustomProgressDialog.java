package com.gsoft.inventory;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
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
    /** 完成确认按钮：同步/迁移结束后才显示，点击关闭遮罩 */
    private Button dialogConfirmButton;

    public CustomProgressDialog(Context context) {
        super(context);
        this.init();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 构造时已初始化，避免重复setContentView
        if (findViewById(R.id.dialogProgressBar) == null) {
            this.init();
        }
    }

    private void init() {
        setContentView(R.layout.dialog_progress);
        // 窗口背景置透明，让布局自身的圆角背景生效
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        // 返回键：进度阶段只消费不关闭；任务结束显示确认按钮后允许关闭
        setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_BACK) {
                return false;
            }
            return dialogConfirmButton == null || dialogConfirmButton.getVisibility() != View.VISIBLE;
        });
        dialogProgressBar = findViewById(R.id.dialogProgressBar);
        dialogProgressBar.setMax(100);
        dialogMessageTextView = findViewById(R.id.dialogMessageTextView);
        if (dialogMessageTextView == null) {
            Log.e("CustomProgressDialog", "messageTextView is null");
        }
        dialogConfirmButton = findViewById(R.id.dialogConfirmButton);
        if (dialogConfirmButton != null) {
            dialogConfirmButton.setVisibility(View.GONE);
            dialogConfirmButton.setOnClickListener(v -> dismiss());
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
        Window window = getWindow();
        if (window != null) {
            // FLAG_DIM_BEHIND 让弹窗外部区域变暗，形成遮罩
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            // 必须在show之前设置，show时才会随窗口属性一起生效
            window.setDimAmount(0.6f);
        }
        super.show();
        // 进度阶段禁止返回键关闭、禁止点击外部关闭
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        // 每次重新显示都回到进度状态，隐藏上次遗留的确认按钮
        hideConfirmButton();
    }

    /**
     * 显示完成确认按钮：任务结束后调用，用户点击后关闭遮罩
     *
     * @param text 按钮文案，为空时保留默认文案
     */
    public void showConfirmButton(String text) {
        if (dialogConfirmButton == null) {
            return;
        }
        if (text != null) {
            dialogConfirmButton.setText(text);
        }
        dialogConfirmButton.setVisibility(View.VISIBLE);
        // 任务已结束，允许返回键和点击外部关闭
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    /**
     * 隐藏完成确认按钮，恢复为不可关闭的进度遮罩
     */
    public void hideConfirmButton() {
        if (dialogConfirmButton != null) {
            dialogConfirmButton.setVisibility(View.GONE);
        }
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