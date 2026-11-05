
package com.gsoft.inventory;

import static com.gsoft.inventory.utils.SysDefine.PROCESS_RESULT_SUCCESS;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.entities.SyncResult;
import com.gsoft.inventory.service.photo.CleanPendingPhotosTask;
import com.gsoft.inventory.service.photo.PhotoMigrationTask;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.domain.AssetsCategory;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.DataTransmitHelper;
import com.gsoft.inventory.utils.FileSearchHelper;
import com.gsoft.inventory.data.GridMenuItem;
import com.gsoft.inventory.utils.MenuGridAdapter;
import com.gsoft.inventory.utils.MenuGridView;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.PhotoStorageHelper;
import com.gsoft.inventory.utils.SecurityUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.xuexiang.xupdate.utils.UpdateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OfflineHomeActivity extends BaseCompatActivity {

    MenuGridView menuGridView;
    List<GridMenuItem> menuItemList = new ArrayList<>();
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.tv_bottom_copy)
    TextView tv_bottom_copy;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;
    private Gson gson = new Gson();

    /** 进度轮询间隔（毫秒） */
    private static final long MIGRATION_PROGRESS_INTERVAL = 300L;
    /** 照片迁移进度遮罩弹窗 */
    private CustomProgressDialog migrationProgressDialog;
    /** 照片迁移进度轮询任务 */
    private Runnable migrationProgressWatcher;
    /** 本次迁移结果只展示一次 */
    private boolean migrationResultShown;
    /** 任务提交时间：避开线程池排队的瞬间误判任务已结束 */
    private long migrationSubmittedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_home);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("资产大数据应用管理平台");
        topBar.addRightImageButton(R.drawable.ic_card, R.id.qmui_tab_segment_item_id).setOnClickListener(v -> {
  /*          Uri uri = Uri.parse("http://47.110.252.199:8080/");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);*/
        });
        tv_bottom_copy.setText("贵州兴财通科技有限公司  版权所有 v".concat(UpdateUtils.getVersionName(mContext)));
        menuItemList.add(new GridMenuItem("资产列表", R.drawable.icon_list));
        menuItemList.add(new GridMenuItem("盘点资产", R.drawable.icon_scan_barcode));
        menuItemList.add(new GridMenuItem("新增资产", R.drawable.icon_add2));
        menuItemList.add(new GridMenuItem("导入资产", R.drawable.icon_import));
        /*menuItemList.add(new GridMenuItem("导入账套", R.drawable.icon_import));*/
        menuItemList.add(new GridMenuItem("导出全部资产", R.drawable.icon_export));
        menuItemList.add(new GridMenuItem("导出变动资产", R.drawable.icon_export));
        menuItemList.add(new GridMenuItem("导入分类", R.drawable.icon_import));
        menuItemList.add(new GridMenuItem("导入清查人员", R.drawable.icon_import));
        menuItemList.add(new GridMenuItem("照片迁移", R.drawable.icon_convert));
        menuItemList.add(new GridMenuItem("照片清除", R.drawable.icon_save));
        menuItemList.add(new GridMenuItem("资产处置", R.drawable.icon_check));


        menuGridView = (MenuGridView) findViewById(R.id.menuGridView);
        menuGridView.setAdapter(new MenuGridAdapter(mContext, menuItemList));
        menuGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 迁移进行中任何菜单都不响应，并恢复遮罩，避免用户以为程序卡死
                if (PhotoMigrationTask.isRunning()) {
                    showMigrationProgress();
                    showShortText("照片迁移进行中，请稍候");
                    return;
                }
                GridMenuItem gridMenuItem = menuItemList.get(position);
                String menuTitle = gridMenuItem.getTitle();
                if (application.getLoginAccount() == null || StringUtils.isNullOrEmpty(application.getPrinterDevice())) {
                    showShortText("登录超时，请退出APP重新登录！");
                    return;
                }
                switch (menuTitle) {
                    case "资产列表":
                        Intent intentMap = new Intent(mContext, AssetsQueryActivity.class);
                        startActivity(intentMap);
                        break;
                    case "盘点资产":
                        Intent intentScan = new Intent(mContext, AssetsCodeScanActivity.class);
                        startActivity(intentScan);
                        break;
                    case "导入资产":
                        boolean hasLocalRecord = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{Const.NOT_UPLOAD}) > 0;
                        if (hasLocalRecord) {
                            QMUIDialog.MessageDialogBuilder builder = new QMUIDialog.MessageDialogBuilder(mContext);
                            builder.setTitle("系统提示")
                                    .setMessage("当前有未同步的资产数据，请确认是否继续导入？")
                                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                                    .addAction("确定", (dialog, index) -> {importAssetsFromFile();})
                                    .create(mCurrentDialogStyle).show();
                            break;
                        } else {
                            importAssetsFromFile();
                        }
                        break;
                    case "新增资产":
                        Intent intentNew = new Intent(mContext, NewAssetsActivity.class);
                        startActivity(intentNew);
                        break;
                    case "导出全部资产":
                        final QMUIDialog.EditTextDialogBuilder builderDC = new QMUIDialog.EditTextDialogBuilder(mContext);
                        builderDC.setTitle("系统提示")
                                .setPlaceholder("输入确认码")
                                .setInputType(InputType.TYPE_CLASS_TEXT)
                                .addAction("取消", (dialog, index) -> dialog.dismiss())
                                .addAction("确定", (dialog, index) -> {
                                    String text = builderDC.getEditText().getText().toString();
                                    if (text != null && text.equals("123456")) {dialog.dismiss();
                                        new ExportAssetsFileTask().execute(true);
                                    }
                                    dialog.dismiss();
                                })
                                .create(mCurrentDialogStyle).show();
                        break;
                    case "导出变动资产":
                        final QMUIDialog.EditTextDialogBuilder builderDCBD = new QMUIDialog.EditTextDialogBuilder(mContext);
                        builderDCBD.setTitle("系统提示")
                                .setPlaceholder("输入确认码")
                                .setInputType(InputType.TYPE_CLASS_TEXT)
                                .addAction("取消", (dialog, index) -> dialog.dismiss())
                                .addAction("确定", (dialog, index) -> {
                                    String text = builderDCBD.getEditText().getText().toString();
                                    if (text != null && text.equals("123456")) {
                                        dialog.dismiss();
                                        new ExportAssetsFileTask().execute(false);
                                    }
                                    dialog.dismiss();
                                })
                                .create(mCurrentDialogStyle).show();
                        break;
                    case "导入分类":
                        Intent intentImportFL = new Intent(mContext, ImportAssetsCategoryActivity.class);
                        startActivity(intentImportFL);
                        break;
                    case "导入清查员":
                        Intent intentQCY = new Intent(mContext, ImportQCYActivity.class);
                        startActivity(intentQCY);
                        break;
                    case "照片迁移":
                        confirmPhotoMigration();
                        break;
                    case "照片清除":
                        cleanPhoto();
                        break;
                    case "资产处置":
                        Intent intentDispose = new Intent(mContext, AssetsDisposeActivity.class);
                        intentDispose.putExtra("isOffline", true);
                        startActivity(intentDispose);
                        break;
                }
            }
        });
    }

    /**
     * 照片迁移：先提示影响范围，确认后在后台把缓存照片迁移到统一照片目录
     */
    private void confirmPhotoMigration() {
        if (PhotoMigrationTask.isRunning()) {
            showMigrationProgress();
            showShortText("照片迁移进行中，请稍候");
            return;
        }
        long cacheCount = AssetPhoto.count(AssetPhoto.class, "PATH LIKE ?", new String[]{"%/cache/images/%"});
        if (cacheCount <= 0) {
            showShortText("当前没有需要迁移的缓存照片");
            return;
        }
        String message = "该功能会把手机缓存目录中的资产照片迁移到统一照片目录。\n\n"
                + "注意：\n"
                + "1. 迁移只挪动文件位置，不会删除照片记录；\n"
                + "2. 无法确定账套的照片会放入“_待认领”目录，不会强行归到当前账套；\n"
                + "3. 共检测到 " + cacheCount + " 条缓存照片记录，数据较多时耗时较长；\n"
                + "4. 迁移过程中请不要退出APP、不要拔线，请保持手机电量充足。\n\n"
                + "确认开始迁移吗？";
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("照片迁移")
                .setMessage(message)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("开始迁移", (dialog, index) -> {
                    dialog.dismiss();
                    startPhotoMigration();
                })
                .create(mCurrentDialogStyle)
                .show();
    }

    /**
     * 启动迁移任务：任务只写静态进度，页面轮询刷新，Activity重建不会丢进度
     */
    private void startPhotoMigration() {
        if (PhotoMigrationTask.isRunning()) {
            showShortText("照片迁移正在进行，请稍候");
            return;
        }
        migrationResultShown = false;
        migrationSubmittedAt = System.currentTimeMillis();
        showMigrationProgress();
        AsyncManager.getInstance().submit(new PhotoMigrationTask(mContext));
    }

    /**
     * 显示迁移进度遮罩
     */
    private void showMigrationProgress() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (migrationProgressDialog == null) {
            migrationProgressDialog = new CustomProgressDialog(this);
        }
        refreshMigrationProgress();
        if (!migrationProgressDialog.isShowing()) {
            migrationProgressDialog.show();
        }
        // 迁移期间保持屏幕常亮，降低息屏后进程被回收导致任务中断的概率
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startMigrationProgressWatcher();
    }

    private void refreshMigrationProgress() {
        if (migrationProgressDialog == null) {
            return;
        }
        int progress = PhotoMigrationTask.getProgress();
        String message = PhotoMigrationTask.getProgressMessage();
        if (StringUtils.isNullOrEmpty(message)) {
            message = "正在准备迁移...";
        }
        migrationProgressDialog.setProgress(progress);
        migrationProgressDialog.setMessage(message + "（" + progress + "%）");
    }

    private void startMigrationProgressWatcher() {
        if (migrationProgressWatcher != null) {
            return;
        }
        migrationProgressWatcher = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                // 提交后1秒内任务可能还在线程池排队，不能误判为已结束
                boolean justSubmitted = System.currentTimeMillis() - migrationSubmittedAt < 1000L;
                if (PhotoMigrationTask.isRunning() || justSubmitted) {
                    refreshMigrationProgress();
                    mainHandler.postDelayed(this, MIGRATION_PROGRESS_INTERVAL);
                    return;
                }
                // 任务结束：先撤掉遮罩，再展示结果
                hideMigrationProgress();
                if (migrationResultShown) {
                    return;
                }
                String errorMessage = PhotoMigrationTask.getErrorMessage();
                if (!StringUtils.isNullOrEmpty(errorMessage)) {
                    migrationResultShown = true;
                    showShortText("照片迁移失败：" + errorMessage);
                    return;
                }
                PhotoMigrationTask.Result result = PhotoMigrationTask.consumeLastResult();
                if (result != null) {
                    migrationResultShown = true;
                    showMigrationResult(result);
                }
            }
        };
        mainHandler.postDelayed(migrationProgressWatcher, MIGRATION_PROGRESS_INTERVAL);
    }

    /**
     * 关闭进度遮罩并恢复系统屏幕常亮设置
     */
    private void hideMigrationProgress() {
        if (migrationProgressWatcher != null) {
            mainHandler.removeCallbacks(migrationProgressWatcher);
            migrationProgressWatcher = null;
        }
        if (migrationProgressDialog != null && migrationProgressDialog.isShowing()) {
            migrationProgressDialog.dismiss();
        }
        if (getWindow() != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * 迁移结果统计
     * <p>
     * 账套待认领：账套无法确定的照片已存放到“_待认领”目录，不算迁移成功；
     * 源文件缺失：记录还在但缓存文件已被系统清理，记录保留不删除；
     * 迁移失败：文件复制或数据库更新失败，源文件保留，可重跑。
     */
    private void showMigrationResult(PhotoMigrationTask.Result result) {
        String message = "共处理 " + result.total + " 条缓存照片记录\n"
                + "迁移成功：" + result.success + " 张\n"
                + "账套待认领：" + result.unassigned + " 张（已存到“_待认领”目录）\n"
                + "源文件缺失：" + result.missing + " 张（记录已保留）\n"
                + "空文件跳过：" + result.empty + " 张\n"
                + "迁移失败：" + result.failed + " 张\n\n"
                + "待认领照片保留了原单据号和条码，不属于当前账套；登录对应账套并同步资产后会自动归位，"
                + "也可插线到电脑，在“_待认领”目录查看（明细见“待认领清单.txt”）。";
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("照片迁移完成")
                .setMessage(message)
                .addAction("知道了", (dialog, index) -> dialog.dismiss())
                .create(mCurrentDialogStyle)
                .show();
    }

    /**
     * 照片清除：先展示功能说明和待清除数量，再输入登录密码验证身份，
     * 验证通过后在后台删除待上传照片记录及本地照片文件。
     */
    private void cleanPhoto() {
        if (PhotoMigrationTask.isRunning()) {
            showShortText("照片迁移进行中，请稍候");
            return;
        }
        long pendingCount = AssetPhoto.count(AssetPhoto.class, "STATUS=?", new String[]{"待上传"});
        if (pendingCount == 0) {
            showShortText("当前没有需要清除的待上传照片");
            return;
        }
        String message = "该功能会删除当前设备上所有“待上传”的资产照片记录。\n删除对应的本地照片文件，用于释放手机存储空间。\n\n"
                + "注意：\n"
                + "1. 使用该功能前确定照片已经备份或者上传；\n"
                + "2. 已上传到服务器的照片不会删除；\n"
                + "3. 清除后本地照片无法恢复；\n"
                + "4. 当前共有 " + pendingCount + " 张待上传照片。\n\n"
                + "确认继续清除吗？";
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("照片清除")
                .setMessage(message)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("下一步", (dialog, index) -> {
                    dialog.dismiss();
                    showCleanPhotoPasswordDialog();
                })
                .create(mCurrentDialogStyle)
                .show();
    }

    /**
     * 输入当前登录密码，验证通过后执行照片清除
     */
    private void showCleanPhotoPasswordDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("重要验证")
                .setPlaceholder("请输入密码")
                .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "确定", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    String password = builder.getEditText().getText().toString().trim();
                    if (StringUtils.isNullOrEmpty(password)) {
                        showShortText("请输入密码");
                        dialog.dismiss();
                        mainHandler.postDelayed(this::showCleanPhotoPasswordDialog, 200);
                        return;
                    }
                    if (!verifyLoginPassword(password)) {
                        showShortText("密码错误，请重新输入");
                        dialog.dismiss();
                        mainHandler.postDelayed(this::showCleanPhotoPasswordDialog, 200);
                        return;
                    }
                    dialog.dismiss();
                    executeCleanPhoto();
                })
                .create(mCurrentDialogStyle)
                .show();
    }

    /**
     * 校验当前登录用户密码
     *
     * @param inputPassword 用户输入的明文密码
     * @return true 表示密码正确
     */
    private boolean verifyLoginPassword(String inputPassword) {
        if (application == null || application.getLoginAccount() == null
                || StringUtils.isNullOrEmpty(inputPassword)) {
            return false;
        }
        String storedPassword = application.getLoginAccount().getDLMM();
        if (StringUtils.isNullOrEmpty(storedPassword)) {
            return false;
        }
        String encryptedPassword = SecurityUtils.md5(inputPassword.trim()).toUpperCase();
        return encryptedPassword.equals(storedPassword.toUpperCase());
    }

    /**
     * 后台清除待上传照片记录和本地照片文件
     */
    private void executeCleanPhoto() {
        showProgressDialog("", "正在清除照片");
        AsyncManager.getInstance().submitWithCallback(
                new CleanPendingPhotosTask(mContext),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    hideProgressDialog();
                    if (!result.isSuccess()) {
                        showShortText("照片清除失败，请重试");
                        return;
                    }
                    Integer count = result.getData();
                    int total = count == null ? 0 : count;
                    if (total == 0) {
                        showShortText("当前没有需要清除的待上传照片");
                    } else {
                        showShortText("照片清除完成，共清除" + total + "张待上传照片");
                    }
                }));
    }

    private void importAssetsFromFile() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("系统提示")
                .setPlaceholder("输入确认码")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String text = builder.getEditText().getText().toString();
                    if ("123456".equals(text)) {
                        dialog.dismiss();
                        Intent intentImport = new Intent(OfflineHomeActivity.this, ImportAssetsActivity.class);
                        startActivity(intentImport);
                    }
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show();
    }

    private void chooseSyncDepart() {
        String[] bmArray = new String[AccountHelper.getDepartList(application.getLoginAccount().getZTBH()).size()];
        AccountHelper.getDepartList(application.getLoginAccount().getZTBH()).toArray(bmArray);
        int[] checkedIndexArray = new int[bmArray.length];
        for (int i = 0; i < bmArray.length; i++) {
            checkedIndexArray[i] = i;
        }
        final QMUIDialog.MultiCheckableDialogBuilder builder = new QMUIDialog.MultiCheckableDialogBuilder(mContext)
                .setCheckedItems(checkedIndexArray)
                .addItems(bmArray, (dialogInterface, i) -> {
                });
        builder.addAction("取消", (dialog, index) -> dialog.dismiss());
        builder.addAction("确认", (dialog, index) -> {
            String deptids = "";
            for (int i = 0; i < builder.getCheckedItemIndexes().length; i++) {
                String bmmc = bmArray[builder.getCheckedItemIndexes()[i]];
                String bmbh = AccountHelper.getDepartCode(application.getLoginAccount().getZTBH(), bmmc);
                deptids = deptids.concat(bmbh).concat(",");
            }
            if (!StringUtils.isNullOrEmpty(deptids)) {
                new OfflineHomeActivity.SyncServerAssetsTask().execute(deptids.substring(0, deptids.length() - 1));
            }
            dialog.dismiss();
        });
        builder.create(mCurrentDialogStyle).show();
    }

    class SyncDisposeAssetsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步");
        }

        @Override
        protected String doInBackground(Void... voids) {
            List<DisposeAsset> disposeAdding = DisposeAsset.find(DisposeAsset.class, "STATUS=?", "1");
            List<DisposeAsset> disposeDeleting = DisposeAsset.find(DisposeAsset.class, "STATUS=?", "2");
            if (disposeAdding.size() == 0 && disposeDeleting.size() == 0) return "没有需要同步的资产处置记录";
            int addCount = 0;
            int deleteCount = 0;
            for (DisposeAsset asset : disposeAdding) {
                String result = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_SCAN_UPLOAD(), asset.getDispose(), asset.getCode()));
                if (result.contains("\"result\":\"OK\"") || result.contains("OK")) {
                    asset.setStatus(0);
                    asset.save();
                    addCount++;
                }
            }
            for (DisposeAsset asset : disposeDeleting) {
                String result = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_SCAN_UPLOAD(), asset.getDispose(), asset.getCode()));
                if (result.contains("\"result\":\"OK\"") || result.contains("OK")) {
                    asset.setStatus(0);
                    asset.save();
                    deleteCount++;
                }
            }
            return String.format("同步新增记录：%1$s条，删除记录：%2$s", addCount, deleteCount);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            showShortText(s);
        }
    }

    class SyncAssetsCategoryTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步");
        }

        @Override
        protected String doInBackground(Void... voids) {
            String s = OkHttpUtils.getInstance().get(SysDefine.SERVERHOST_SYNC_ASSETCATEGORY());
            if (StringUtils.isNullOrEmpty(s) || s.startsWith("ERROR")) {
                showShortText("同步出错，".concat(s));
            }
            StringBuilder stringBuilder = new StringBuilder();
            try {
                List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {
                }.getType());
                if (results == null || results.size() == 0) {
                    showShortText("同步出错，".concat(s));
                    return "ERROR:";
                }
                AssetsCategory.deleteAll(AssetsCategory.class);
                int lineIndex = 0;
                for (String dataLine : results) {
                    long newID = DataTransmitHelper.insertAssetsCategory(dataLine);
                    if (newID == -1) {
                        stringBuilder.append("第".concat(String.valueOf(lineIndex)).concat("行导入失败；"));
                    }
                    lineIndex++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                stringBuilder.append("同步失败，请联系技术人员\n");
            }
            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();

            if (StringUtils.isNullOrEmpty(s)) {
                showShortText("同步成功!");
            } else {
                showShortText("同步出错，".concat(s));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }
    }

    class SyncServerAssetsTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步");
        }

        @Override
        protected String doInBackground(String... params) {
            String bmbh = params[0];
            String s = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVERHOST_SYNC_ASSETS(), application.getLoginAccount().getZTBH(), bmbh));
            if (StringUtils.isNullOrEmpty(s) || s.startsWith("ERROR")) {
                showShortText("同步出错，".concat(s));
                return s;
            }
            StringBuilder stringBuilder = new StringBuilder();
            try {
                List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {
                }.getType());
                if (results == null || results.isEmpty()) {
                    showShortText("同步出错，".concat(s));
                    return "ERROR:";
                }
                Assets.deleteAll(Assets.class);
                int lineIndex = 0;
                for (String dataLine : results) {
                    long newID = DataTransmitHelper.insertAssets(dataLine);
                    if (newID == -1) {
                        stringBuilder.append("第".concat(String.valueOf(lineIndex)).concat("行导入失败；"));
                    }
                    lineIndex++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                stringBuilder.append("同步失败，请联系技术人员\n");
            }
            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();

            if (StringUtils.isNullOrEmpty(s)) {
                showShortText("同步成功!");
            } else {
                showShortText("同步出错，".concat(s));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }
    }

    class SyncServerAccountsTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步");
        }

        @Override
        protected String doInBackground(Void... voids) {
            String s = OkHttpUtils.getInstance().get(SysDefine.SERVERHOST_SYNC_ACCOUNTS());
            if (StringUtils.isNullOrEmpty(s) || s.startsWith("ERROR")) {
                showShortText("同步出错，".concat(s));
            }
            StringBuilder stringBuilder = new StringBuilder();
            try {
                List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {
                }.getType());
                if (results == null || results.size() == 0) {
                    showShortText("同步出错，".concat(s));
                    return "ERROR:";
                }
                Accounts.deleteAll(Accounts.class);
                int lineIndex = 0;
                for (String dataLine : results) {
                    long newID = DataTransmitHelper.insertAccounts(dataLine);
                    if (newID == -1) {
                        stringBuilder.append("第".concat(String.valueOf(lineIndex)).concat("行导入失败；"));
                    }
                    lineIndex++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                stringBuilder.append("同步失败，请联系技术人员\n");
            }
            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();

            if (StringUtils.isNullOrEmpty(s)) {
                showShortText("同步成功，请退出系统重新登陆！");
            } else {
                showShortText("同步出错，".concat(s));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }
    }

    class SyncAssetsTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步");
        }

        @Override
        protected String doInBackground(Void... voids) {
            List<Assets> assetsList = Assets.find(Assets.class, "ISUPLOAD=?", Const.NOT_UPLOAD);
            StringBuilder stringBuilder = new StringBuilder();
            if (assetsList.size() > 0) {
                JsonArray jsonArray = new JsonArray();
                for (Assets assets : assetsList) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("data", assets.toLineString());
                    jsonArray.add(jsonObject);
                }
                String responseString = OkHttpUtils.getInstance().postAssetsJson(SysDefine.SERVERHOST_ASSETS(), gson.toJson(jsonArray));
                try {
                    List<SyncResult> results = gson.fromJson(responseString, new TypeToken<List<SyncResult>>() {
                    }.getType());
                    if (results == null || results.size() == 0) {
                        stringBuilder.append("有记录同步失败，请联系技术人员\n");
                    }
                    for (SyncResult sr : results) {
                        String barcode = sr.barcodeid;
                        if (sr.result.equals("1")) {
                            List<Assets> assets = Assets.find(Assets.class, "BARCODEID=?", barcode);
                            for (Assets ast : assets) {
                                if (ast.getREMARKS().equals("待删除")) {
                                    ast.delete();
                                } else {
                                    ast.setISUPLOAD(Const.UPLOADED);
                                    ast.save();
                                }
                            }
                        } else {
                            stringBuilder.append("条码：".concat(barcode).concat("同步失败\n"));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stringBuilder.append("同步失败，请联系技术人员\n");
                }
            }

            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (StringUtils.isNullOrEmpty(s)) {
                showShortText("同步成功!");
            } else {
                showShortText("同步出错，".concat(s));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }
    }

    class ExportAssetsFileTask extends AsyncTask<Boolean, Void, String> {
        private boolean allData;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在生成文件");
        }

        @Override
        protected String doInBackground(Boolean... exportParams) {
            if (exportParams.length == 0) {
                return "导出参数为空";
            }

            allData = exportParams[0];
            String fileDirPath =  mContext.getExternalFilesDir(null) + "/export";
            String filePath = SysConfig.getExportFilePath(fileDirPath);
            // 高版本不能导出到外置路径
            Log.d("导出数据路径： ", filePath);
            try {
                String resultMessage = DataTransmitHelper.exportAssets(filePath, allData);

                if (new File(filePath).exists() && TextUtils.isEmpty(resultMessage)) {
                    FileSearchHelper.notifySystemToScan(filePath);
                    return PROCESS_RESULT_SUCCESS;
                }
                return resultMessage;
            } catch (Exception e) {
                e.printStackTrace();
                return "导出过程中发生异常: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (!allData) DataTransmitHelper.onExportAssetsOver();
            showShortText("导出成功！");
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            hideProgressDialog();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 迁移过程中禁止返回退出，避免任务被中断
            if (PhotoMigrationTask.isRunning()) {
                showShortText("照片迁移进行中，请稍候");
                return true;
            }
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    boolean isExit = false;

    /**
     * 点击两次退出程序
     */
    private void exit() {
        if (!isExit) {
            isExit = true;
            showShortText("再按一次退出程序");
            mainHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            finish();
            //参数用作状态码；根据惯例，非 0 的状态码表示异常终止。
            if (application.lifecycleCallbacks != null) {
                application.lifecycleCallbacks.removeAllActivities();
            }
            System.exit(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isExit = false;
        // 页面重建或从后台切回时恢复进度遮罩
        if (PhotoMigrationTask.isRunning()) {
            showMigrationProgress();
        }
    }

    @Override
    protected void onDestroy() {
        // 只关闭弹窗，迁移任务在后台继续执行，重进首页可继续查看进度
        hideMigrationProgress();
        super.onDestroy();
    }
}