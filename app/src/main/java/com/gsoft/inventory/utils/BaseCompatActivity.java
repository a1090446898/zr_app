package com.gsoft.inventory.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;



import com.gsoft.inventory.InventoryApplication;

/**
 * @author 10904
 */
public abstract class BaseCompatActivity extends AppCompatActivity implements SyncAssetsFlow.Callback {
    protected Context mContext;

    protected InventoryApplication application;

    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ProgressDialog progressDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        application = ((InventoryApplication) getApplication());
    }


    public abstract void initializeView();


    @Override
    public void showShortText(CharSequence textMessage) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, textMessage, Toast.LENGTH_SHORT).show();
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BaseCompatActivity.this, textMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void showProgressDialog(String title, String message) {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(mContext);
        }
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * 资产同步（上传）公共流程：统计本地未同步数量，确认后异步上传（与首页"同步上传"一致）
     */
    public void startSyncAssetsTask() {
        SyncAssetsFlow.startSync(mContext, application.getZcqcyCode(), mainHandler, this);
    }
}
