package com.gsoft.inventory.abs;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gsoft.inventory.InventoryApplication;
import com.gsoft.inventory.utils.SyncAssetsFlow;

import static com.gsoft.inventory.InventoryApplication.getApplication;

/**
 * @author 10904
 */
public abstract class BaseFragment extends Fragment implements SyncAssetsFlow.Callback {

    protected Context mContext;

    private ProgressDialog progressDialog;

    protected InventoryApplication application;

    protected Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();

        application = ((InventoryApplication) getApplication());
    }

    @Override
    public void showShortText(CharSequence textMessage) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(mContext, textMessage, Toast.LENGTH_SHORT).show();
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, textMessage, Toast.LENGTH_SHORT).show();
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
