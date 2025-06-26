package com.gsoft.inventory.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;



import com.gsoft.inventory.InventoryApplication;

public abstract class BaseCompatActivity extends AppCompatActivity {
    protected Context mContext;

    protected InventoryApplication application;

    protected final Handler mainHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        application = ((InventoryApplication) getApplication());
    }


    public abstract void initializeView();

    private ProgressDialog progressDialog;

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

    protected void showProgressDialog(String title, String message) {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(mContext);
        }
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    protected void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
