package com.gsoft.inventory;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;

import butterknife.BindView;
import com.gsoft.inventory.adapter.WaitingUploadPhotoListAdapter;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.service.photo.PhotoMigrationTask;
import com.gsoft.inventory.service.photo.ReclaimPendingPhotosTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.AssetsUtils;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.PhotoStorageHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 照片上传页
 * <p>
 * 标题固定显示“照片上传”，底部提供“清空记录”和“一键上传”；
 * 上传成功只把记录状态改为“已上传”，本地照片一律保留；
 * 清空记录会移除已上传记录和本地照片缺失的记录，同样不删除照片目录文件。
 *
 * @author 10904
 */
public class UploadPhotoImagesActivity extends BaseCompatActivity {

    /** 页面固定标题 */
    private static final String PAGE_TITLE = "照片上传";

    WaitingUploadPhotoListAdapter waitingUploadPhotoListAdapter;
    List<AssetPhoto> recordList;

    @BindView(R.id.topbar)
    QMUITopBar topBar;

    CustomProgressDialog progressDialog;
    ListView listView;
    QMUIRoundButton btnUploadAll;
    QMUIRoundButton btnClearRecords;

    /** 上传是否已结束：最后一张照片的进度消息可能晚于结束消息到达，用它拦截避免覆盖完成态 */
    private boolean uploadFinished = false;

    Handler handler = new Handler(Looper.getMainLooper()) {
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
                        // 上传已结束：忽略结束之后才到的进度消息，否则会把完成态覆盖回进度态
                        if (uploadFinished) {
                            return;
                        }
                        // 上传中置灰按钮，避免重复提交同一批照片
                        setActionButtonsEnabled(false);
                        // CustomProgressDialog.show()会重置确认按钮和可关闭状态，已显示时不再重复调用
                        if (!progressDialog.isShowing()) {
                            progressDialog.show();
                        }
                    } else if (show == 0) {
                        uploadFinished = true;
                        // show()内部会隐藏确认按钮，必须先确保遮罩显示，再展示完成确认按钮
                        if (!progressDialog.isShowing()) {
                            progressDialog.show();
                        }
                        // 上传结束：重新查询记录，成功项状态已是“已上传”，列表同步刷新
                        refreshRecordList();
                        setActionButtonsEnabled(true);
                        progressDialog.showConfirmButton("确定");
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
        // 标题固定，避免上传后数量变化让用户误以为照片被清空
        topBar.setTitle(PAGE_TITLE);
        progressDialog = new CustomProgressDialog(this);

        // 本地文件缺失的记录不自动删除：仅在卡片上用红字提示，由用户通过“清空记录”主动移除
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        listView = findViewById(R.id.listFiles);
        btnUploadAll = findViewById(R.id.btnUploadAll);
        btnUploadAll.setOnClickListener(v -> startUpload());
        btnClearRecords = findViewById(R.id.btnClearRecords);
        btnClearRecords.setOnClickListener(v -> showClearRecordsConfirm());

        // 最新的照片排最前（ID越大越新）
        recordList = AssetPhoto.listAll(AssetPhoto.class, "ID DESC");
        waitingUploadPhotoListAdapter = new WaitingUploadPhotoListAdapter(recordList, mContext);
        listView.setAdapter(waitingUploadPhotoListAdapter);
        // 后台尝试把待认领照片归位到当前账套（资产表能匹配到才挪动）
        reclaimPendingPhotos();
    }

    /**
     * 一键上传：迁移期间禁止上传，避免与迁移任务同时改动同一条照片记录
     */
    private void startUpload() {
        if (PhotoMigrationTask.isRunning()) {
            showShortText("照片迁移进行中，请稍候再上传");
            return;
        }
        // 按“待上传”状态判断：已上传/待认领的记录不参与一键上传
        long pendingCount = AssetPhoto.count(AssetPhoto.class, "STATUS=?",
                new String[]{PhotoStorageHelper.STATUS_WAIT_UPLOAD});
        if (pendingCount <= 0) {
            showShortText("没有需要上传的图片");
            return;
        }
        // 新一轮上传：重置结束标记，保证进度遮罩正常显示
        uploadFinished = false;
        AssetsUtils.uploadAssetPhotos(handler, mContext);
    }

    /**
     * 清空记录前二次确认：范围包含已上传记录和本地照片缺失的记录，只清记录不删文件
     */
    private void showClearRecordsConfirm() {
        if (PhotoMigrationTask.isRunning()) {
            showShortText("照片迁移进行中，请稍候再操作");
            return;
        }
        int clearableCount = findClearableRecords().size();
        if (clearableCount <= 0) {
            showShortText("暂无可清空的记录");
            return;
        }
        String message = "将清空已上传的记录或者本地照片缺失的记录，共 " + clearableCount + " 条。\n\n"
                + "仅清空本地记录，不会删除照片目录中的文件。";
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("清空记录")
                .setMessage(message)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "确定", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    dialog.dismiss();
                    clearRecords();
                })
                .show();
    }

    /**
     * 筛选可清空的记录：状态为已上传，或本地照片文件已缺失
     */
    private List<AssetPhoto> findClearableRecords() {
        List<AssetPhoto> clearableList = new ArrayList<>();
        List<AssetPhoto> allList = AssetPhoto.listAll(AssetPhoto.class, "ID DESC");
        if (allList == null) {
            return clearableList;
        }
        for (AssetPhoto photo : allList) {
            // 已上传记录直接命中，短路后不再做文件存在性检查
            if (PhotoStorageHelper.STATUS_UPLOADED.equals(photo.getStatus())
                    || PhotoStorageHelper.isPhotoFileMissing(photo)) {
                clearableList.add(photo);
            }
        }
        return clearableList;
    }

    /**
     * 删除可清空的记录并刷新列表：照片文件保持不动，用户可自行在文件目录中管理
     */
    private void clearRecords() {
        // 确认后按最新状态重新筛选，避免弹框期间记录被修复后误删
        List<AssetPhoto> clearableList = findClearableRecords();
        int removed = 0;
        for (AssetPhoto photo : clearableList) {
            // 只删数据库记录，绝不调用 PhotoStorageHelper.deleteLocalPhoto；文件缺失的记录同样只清记录
            photo.delete();
            removed++;
        }
        refreshRecordList();
        showShortText("已清空" + removed + "条记录，照片文件已保留");
    }

    /**
     * 重新查询照片记录并刷新列表
     */
    private void refreshRecordList() {
        // 最新的照片排最前，上传完成后刷新也保持该顺序
        List<AssetPhoto> list = AssetPhoto.listAll(AssetPhoto.class, "ID DESC");
        if (recordList == null) {
            recordList = list;
            waitingUploadPhotoListAdapter = new WaitingUploadPhotoListAdapter(recordList, mContext);
            listView.setAdapter(waitingUploadPhotoListAdapter);
            return;
        }
        recordList.clear();
        recordList.addAll(list);
        waitingUploadPhotoListAdapter.notifyDataSetChanged();
    }

    /**
     * 底部操作按钮可用状态：上传过程中置灰，防止重复点击
     */
    private void setActionButtonsEnabled(boolean enabled) {
        setButtonEnabled(btnUploadAll, enabled);
        setButtonEnabled(btnClearRecords, enabled);
    }

    private void setButtonEnabled(QMUIRoundButton button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.6f);
    }

    /**
     * 后台归位待认领照片：登录对应账套并同步资产后，打开本页即可自动认领
     */
    private void reclaimPendingPhotos() {
        if (PhotoMigrationTask.isRunning()) {
            return;
        }
        AsyncManager.getInstance().submitWithCallback(new ReclaimPendingPhotosTask(mContext), result -> {
            Integer count = result == null ? null : result.getData();
            if (count == null || count <= 0) {
                return;
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || recordList == null
                        || waitingUploadPhotoListAdapter == null) {
                    return;
                }
                refreshRecordList();
                showShortText("已自动归位" + count + "张待认领照片");
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 不删除文件缺失的记录：列表用红字提示，记录由用户通过“清空记录”主动移除
        if (recordList == null || waitingUploadPhotoListAdapter == null) {
            return;
        }
        refreshRecordList();
    }
}