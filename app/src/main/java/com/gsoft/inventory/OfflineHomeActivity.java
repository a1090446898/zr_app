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
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.entities.SyncResult;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.Accounts;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.AssetsCategory;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.DataTransmitHelper;
import com.gsoft.inventory.utils.FileSearchHelper;
import com.gsoft.inventory.data.GridMenuItem;
import com.gsoft.inventory.utils.MenuGridAdapter;
import com.gsoft.inventory.utils.MenuGridView;
import com.gsoft.inventory.utils.OkHttpUtils;
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
        /*topBar.addRightImageButton(R.drawable.ic_card, R.id.qmui_tab_segment_item_id).setOnClickListener(v -> {
            Uri uri = Uri.parse("http://47.110.252.199:8080/");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });*/
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
        menuItemList.add(new GridMenuItem("照片清除", R.drawable.icon_save));
        menuItemList.add(new GridMenuItem("资产处置", R.drawable.icon_check));


        menuGridView = (MenuGridView) findViewById(R.id.menuGridView);
        menuGridView.setAdapter(new MenuGridAdapter(mContext, menuItemList));
        menuGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
                        boolean hasLocalRecord = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{"0"}) > 0;
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
                    case "照片清除":
                        QMUIDialog.EditTextDialogBuilder builderCleanImg = new QMUIDialog.EditTextDialogBuilder(mContext);
                        builderCleanImg.setTitle("系统提示")
                                .setPlaceholder("输入确认码")
                                .setInputType(InputType.TYPE_CLASS_TEXT)
                                .addAction("取消", new QMUIDialogAction.ActionListener() {
                                    @Override
                                    public void onClick(QMUIDialog dialog, int index) {
                                        dialog.dismiss();
                                    }
                                })
                                .addAction("确定", new QMUIDialogAction.ActionListener() {
                                    @Override
                                    public void onClick(QMUIDialog dialog, int index) {
                                        String text = builderCleanImg.getEditText().getText().toString();
                                        if (text.equals("123456")) {
                                            dialog.dismiss();
                                            // 清除图片上传的数据库
                                            AssetPhoto.deleteAll(AssetPhoto.class, "STATUS=?", "待上传");
                                            showShortText("待同步上传照片清除完成！");
                                        }
                                        dialog.dismiss();
                                    }
                                })
                                .create(mCurrentDialogStyle).show();
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

    private void importAssetsFromFile() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("系统提示")
                .setPlaceholder("输入确认码")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String text = builder.getEditText().getText().toString();
                    if (text.equals("123456")) {
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
            List<Assets> assetsList = Assets.find(Assets.class, "ISUPLOAD=?", "0");
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
                                    ast.setISUPLOAD("1");
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
    }
}