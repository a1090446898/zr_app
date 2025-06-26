package com.gsoft.inventory;

import android.annotation.SuppressLint;
import android.os.*;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;

import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import com.google.gson.Gson;
import com.gsoft.inventory.adapter.WaitingUploadPhotoListAdapter;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.inter.OnUploadFinishListener;
import com.gsoft.inventory.utils.AssetsUtils;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.SysConfig;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author 10904
 */
public class UploadPhotoImagesActivity extends BaseCompatActivity {

  /*  @BindView(R.id.textViewInfo)
    TextView textViewInfo;*/

    WaitingUploadPhotoListAdapter waitingUploadPhotoListAdapter;
    List<AssetPhoto> recordList;

    @BindView(R.id.topbar)
    QMUITopBar topBar;

    CustomProgressDialog progressDialog;
    ListView listView;
    private Gson gson = new Gson();
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle b = msg.getData();
            int progress = b.getInt("progress");
            String message = b.getString("message");
            int show = b.getInt("show", 1);

            Log.d("UploadPhotoImagesActivity", "progress:" + progress + ",message:" + message + ",show:" + show);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 更新UI操作
                    progressDialog.setMessage(message);
                    progressDialog.setProgress(progress);
                    if (show == 1) {
                        progressDialog.show();
                    } else if (show == 0) {
                        // 关闭进度条
                        progressDialog.setCancelable(true);
                        recordList = AssetPhoto.listAll(AssetPhoto.class, "ID ASC");
                        topBar.setTitle("图片上传(" + recordList.size() + ")");
                        waitingUploadPhotoListAdapter = new WaitingUploadPhotoListAdapter(recordList, mContext);
                        listView.setAdapter(waitingUploadPhotoListAdapter);
                    }
                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo_images);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar = findViewById(R.id.topbar);
        progressDialog = new CustomProgressDialog(this);

        List<AssetPhoto> list = AssetPhoto.listAll(AssetPhoto.class, "ID ASC");
        topBar.setTitle("图片上传(" + list.size() + ")");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        topBar.addRightTextButton("一键上传", R.id.topbar_right_query_button).setOnClickListener(v -> {
            AssetsUtils.uploadAssetPhotos(handler, mContext);
        });

        listView = findViewById(R.id.listFiles);
        recordList = AssetPhoto.listAll(AssetPhoto.class, "ID ASC");
        waitingUploadPhotoListAdapter = new WaitingUploadPhotoListAdapter(recordList, mContext);
        listView.setAdapter(waitingUploadPhotoListAdapter);

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        List<AssetPhoto> list = AssetPhoto.listAll(AssetPhoto.class, "ID ASC");
        recordList.clear();
        recordList.addAll(list);
        topBar.setTitle("图片上传(" + list.size() + ")");
        waitingUploadPhotoListAdapter.notifyDataSetChanged();
    }

// 在主线程中接收消息并更新UI


    public class OnUpload implements OnUploadFinishListener {
        // 计数锁
        private CountDownLatch countDownLatch;

        public OnUpload() {
            showProgressDialog("", "正在同步");
        }

        @Override
        public void setCountDownLatch(int size) {
            countDownLatch = new CountDownLatch(size);
        }

        @Override
        public CountDownLatch getCountDownLatch() {
            return countDownLatch;
        }

        @Override
        public void countDown() {
            countDownLatch.countDown();
        }

        @Override
        public long getCount() {
            return countDownLatch.getCount();
        }

        @Override
        public void onUploadSuccess(AssetPhoto photo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        recordList.clear();
                        List<AssetPhoto> list = AssetPhoto.listAll(AssetPhoto.class, "ID ASC");
                        recordList.addAll(list);
                        topBar.setTitle("图片上传(" + list.size() + ")");
                        waitingUploadPhotoListAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.d("UploadPhotoImagesActivity", e.getMessage());
                    } finally {
                        hideProgressDialog();
                    }
                }
            });
        }

        @Override
        public void onUploadFailed(AssetPhoto photo, Exception e) {

        }
    }
}