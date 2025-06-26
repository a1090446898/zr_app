package com.gsoft.inventory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.gsoft.inventory.data.GridMenuItem;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.service.main.*;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.view.asset_borrow.AssetBorrowActivity;
import com.gsoft.inventory.view.asset_purchase.AssetPurchaseActivity;
import com.gsoft.inventory.view.asset_receive.AssetReceiveActivity;
import com.gsoft.inventory.view.asset_repair.AssetRepairActivity;
import com.gsoft.inventory.view.asset_return.AssetReturnActivity;
import com.gsoft.inventory.view.asset_transfer.AssetTransferActivity;
import com.gsoft.inventory.view.dress_manage.DressManageActivity;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.xuexiang.xupdate.utils.UpdateUtils;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import java.util.*;

import static com.gsoft.inventory.utils.SysDefine.PROCESS_RESULT_SUCCESS;

/**
 * @author 10904
 */
@RuntimePermissions
public class MainActivity extends BaseCompatActivity {
    MenuGridView menuGridView;
    List<GridMenuItem> menuItemList = new ArrayList<>();
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.tv_bottom_copy)
    TextView tv_bottom_copy;

    @BindView(R.id.tv_bottom_export_path)
    TextView tv_bottom_export_path;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    boolean isExit = false;

    // 当前菜单层级栈（用于返回上级）
    private Deque<List<GridMenuItem>> menuStack = new ArrayDeque<>();
    // 根菜单数据（一级菜单）
    private List<GridMenuItem> rootMenus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (application.getLoginAccount() == null || StringUtils.isNullOrEmpty(application.getPrinterDevice())) {
            showShortText("登录超时，请退出APP重新登录！");
            return;
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        menuGridView = (MenuGridView) findViewById(R.id.menuGridView);

        initHierarchicalMenus();
        initializeView();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initializeView() {
        topBar.setTitle("资产大数据应用管理平台");
        topBar.addRightTextButton("签名", R.id.qmui_tab_segment_item_id).setOnClickListener(v -> {
            startActivity(new Intent(mContext, SignatureActivity.class));
        });
        tv_bottom_copy.setText("贵州兴财通科技有限公司  版权所有 v".concat(UpdateUtils.getVersionName(mContext)));
        tv_bottom_export_path.setText("导出路径：Android\\data\\com.gsoft.inventory\\files");

        showMenu(rootMenus);

        // 菜单点击事件
        menuGridView.setOnItemClickListener((parent, view, position, id) -> {
            GridMenuItem currentMenu = menuItemList.get(position);
            String menuTitle = currentMenu.getTitle();
            boolean isDirMenu = currentMenu.isDirectory();
            // 处理"返回上级"逻辑
            if ("返回上级".equals(menuTitle)) {
                // 显示上一级
                List<GridMenuItem> peek = menuStack.peek();
                if (!menuStack.isEmpty()) {
                    // 弹出当前层级
                    menuStack.pop();
                }
                showMenu(peek);

                return;
            }
            if (isDirMenu) {
                // 校验子菜单不为 null
                List<GridMenuItem> subMenus = currentMenu.getSubMenus();
                if (subMenus == null) {
                    showShortText("子菜单数据异常");
                    return;
                }
                // 保存当前层级
                menuStack.push(menuItemList);
                // 显示子菜单
                showMenu(subMenus);
                // 更新顶部栏标题
                topBar.setTitle(menuTitle);
            } else {
                // 菜单具体逻辑处理
                handleFunction(currentMenu.getFunctionType());
            }
        });
    }

    /**
     * 初始化层级菜单
     */
    private void initHierarchicalMenus(){
        // 二级菜单示例：资产操作子菜单
        List<GridMenuItem> assetsMenus = new ArrayList<>();
        assetsMenus.add(new GridMenuItem("资产列表", R.drawable.icon_list, "资产列表"));
        assetsMenus.add(new GridMenuItem("盘点资产", R.drawable.icon_scan_barcode, "盘点资产"));
        assetsMenus.add(new GridMenuItem("新增资产", R.drawable.icon_add2, "新增资产"));
        assetsMenus.add(new GridMenuItem("导出资产", R.drawable.icon_export, "导出资产"));
        assetsMenus.add(new GridMenuItem("导入资产", R.drawable.icon_import, "导入资产"));
        assetsMenus.add(new GridMenuItem("同步分类", R.drawable.icon_sync, "同步分类"));
        assetsMenus.add(new GridMenuItem("监督打印", R.drawable.icon_printer, "监督打印"));
        assetsMenus.add(new GridMenuItem("同步校验", R.drawable.icon_sync, "同步校验"));
        assetsMenus.add(new GridMenuItem("同步处置", R.drawable.icon_sync, "同步处置"));
        assetsMenus.add(new GridMenuItem("照片上传", R.drawable.icon_sync, "照片上传"));
        assetsMenus.add(new GridMenuItem("数字签名", R.drawable.icon_check, "数字签名"));
        assetsMenus.add(new GridMenuItem("批量复制", R.drawable.icon_check, "批量复制"));
        assetsMenus.add(new GridMenuItem("照片清除", R.drawable.icon_save, "照片清除"));
        assetsMenus.add(new GridMenuItem("", null, ""));




        // 根菜单
        rootMenus = new ArrayList<>();
        rootMenus.add(new GridMenuItem("资产盘点", true, R.drawable.icon_list, assetsMenus));

        rootMenus.add(new GridMenuItem("资产领用", R.drawable.icon_asset_receive, "资产领用"));
        rootMenus.add(new GridMenuItem("资产借用", R.drawable.icon_asset_borrow, "资产借用"));
        rootMenus.add(new GridMenuItem("采购申请", R.drawable.icon_asset_purchase, "采购申请"));
        rootMenus.add(new GridMenuItem("资产报修", R.drawable.icon_asset_repair, "资产报修"));
        rootMenus.add(new GridMenuItem("资产移交", R.drawable.icon_asset_transfer, "资产移交"));
        rootMenus.add(new GridMenuItem("资产归还", R.drawable.icon_asset_return, "资产归还"));
        rootMenus.add(new GridMenuItem("资产报废", R.drawable.icon_asset_dispose, "资产报废"));
        rootMenus.add(new GridMenuItem("服饰管理", R.drawable.icon_dress_manage, "服饰管理"));
    }

    // 显示指定层级的菜单
    private void showMenu(List<GridMenuItem> currentMenus) {
        // 添加空值校验
        if (currentMenus == null) {
            showShortText("菜单数据异常");
            return;
        }
        // 清空原菜单列表并填充当前层级数据
        menuItemList = currentMenus;
        // 移除所有包含返回上级的菜单，使用迭代器，避免重复
        Iterator<GridMenuItem> iterator = menuItemList.iterator();
        while (iterator.hasNext()) {
            GridMenuItem menuItem = iterator.next();
            if ("返回上级".equals(menuItem.getTitle())) {
                iterator.remove();
            }
        }

        // 如果不是根菜单，添加"返回上级"项
        if (!menuStack.isEmpty()) {
            menuItemList.add(new GridMenuItem("返回上级", R.drawable.icon_back,  "返回上级"));
        }
        MenuGridAdapter adapter = new MenuGridAdapter(mContext, menuItemList);
        menuGridView.setAdapter(adapter);
        // 当需要更新数据时，直接调用具体适配器的 notifyDataSetChanged()
        adapter.notifyDataSetChanged();
    }




    // 菜单处理逻辑
    private void handleFunction(String functionType) {
        switch (functionType) {
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
                    runOnUiThread(() -> {
                        showShortText("当前有未上传的资产数据，请先上传同步！");
                    });
                    break;
                }
                final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
                builder.setTitle("系统提示")
                        .setPlaceholder("输入确认码")
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .addAction("取消", (dialog, index) -> dialog.dismiss())
                        .addAction("确定", (dialog, index) -> {
                            String text = builder.getEditText().getText().toString();
                            if ("123456".equals(text)) {
                                dialog.dismiss();
                                Intent intentImport = new Intent(MainActivity.this, ImportNetAssetsActivity.class);
                                startActivity(intentImport);
                            }
                            dialog.dismiss();
                        })
                        .create(mCurrentDialogStyle).show();
                break;
            case "导入账套":
                syncServerAccountsFun();
                break;
            case "同步分类":
                syncAssetsFun();
                break;
            case "导出资产":
                MainActivityPermissionsDispatcher.requestExportFileWithPermissionCheck(MainActivity.this);
                break;
            case "新增资产":
                Intent intentNew = new Intent(mContext, NewAssetsActivity.class);
                startActivity(intentNew);
                break;
            case "监督打印":
                Uri uri = Uri.parse(SysDefine.SERVERHOST);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
            case "同步校验":
                syncAssetsTaskFun();
                break;
            case "同步处置":
                syncDisposeAssetsFun();
                break;
            case "照片上传":
                Intent intentPhoto = new Intent(mContext, UploadPhotoImagesActivity.class);
                startActivity(intentPhoto);
                break;
            case "数字签名":
                Intent intentSign = new Intent(mContext, SignatureActivity.class);
                startActivity(intentSign);
                break;
            case "批量复制":
                Intent intentCopy = new Intent(mContext, BatchCopyActivity.class);
                startActivity(intentCopy);
                break;
            case "照片清除":
                QMUIDialog.EditTextDialogBuilder builderCleanImg = new QMUIDialog.EditTextDialogBuilder(mContext);
                builderCleanImg.setTitle("系统提示")
                        .setPlaceholder("输入确认码")
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .addAction("取消", (dialog, index) -> dialog.dismiss())
                        .addAction("确定", (dialog, index) -> {
                            String text = builderCleanImg.getEditText().getText().toString();
                            if ("123456".equals(text)) {
                                dialog.dismiss();
                                // 清除图片上传的数据库
                                AssetPhoto.deleteAll(AssetPhoto.class, "STATUS=?", "待上传");
                                showShortText("待同步上传照片清除完成！");
                            }
                            dialog.dismiss();
                        })
                        .create(mCurrentDialogStyle).show();
                break;
            case "资产领用":
                Intent intentReceive = new Intent(mContext, AssetReceiveActivity.class);
                startActivity(intentReceive);
                break;
            case "资产借用":
                Intent intentBorrow = new Intent(mContext, AssetBorrowActivity.class);
                startActivity(intentBorrow);
                break;
            case "采购申请":
                Intent intentPurchase = new Intent(mContext, AssetPurchaseActivity.class);
                startActivity(intentPurchase);
                break;
            case "资产报修":
                Intent intentRepair = new Intent(mContext, AssetRepairActivity.class);
                startActivity(intentRepair);
                break;
            case "资产移交":
                Intent intentTransfer = new Intent(mContext, AssetTransferActivity.class);
                startActivity(intentTransfer);
                break;
            case "资产归还":
                Intent intentReturn = new Intent(mContext, AssetReturnActivity.class);
                startActivity(intentReturn);
                break;
            case "资产报废":
                Intent intentDispose = new Intent(mContext, AssetsDisposeActivity.class);
                intentDispose.putExtra("isOffline", false);
                startActivity(intentDispose);
                break;
            case "服饰管理":
                Intent intentDress = new Intent(mContext, DressManageActivity.class);
                startActivity(intentDress);
                break;

        }
    }

    private void syncAssetsTaskFun() {
        if (!NetworkUtils.checkNetConnect(mContext)) {
            showShortText("当前网络连接不正常，请稍后再试！");
            return;
        }
        long waitingSyncCount = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{"0"});
        if (waitingSyncCount == 0) {
            showShortText("当前资产数据已经全部同步，无需同步！");
            return;
        }
        String tips = "共有：".concat(String.valueOf(waitingSyncCount)).concat("条记录未同步，是否开始同步？");
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("系统提示")
                .setMessage(tips)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "开始同步", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    showProgressDialog("", "正在同步");
                    SyncAssetsTask task = new  SyncAssetsTask(mContext, application.getZcqcyCode());
                    AsyncManager.getInstance().submitWithCallback(task, result -> {
                        mainHandler.post(() -> {
                            hideProgressDialog();
                            String resultString = result.getData();
                            if ("存在图片同步失败，已停止数据同步！".equals(resultString)) {
                                showShortText(resultString);
                            } else if ("图片同步被中断，请重试".equals(resultString)) {
                                showShortText(resultString);
                            }
                            else {
                                showShortText(resultString);
                            }
                        });
                    });
                    dialog.dismiss();
                })
                .show();
    }

    private void syncServerAccountsFun() {
        showProgressDialog("", "正在同步");
        SyncServerAccountsTask task = new SyncServerAccountsTask();
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                String resultString = result.getData();
                showShortText(resultString);
            });
        });
    }

    private void syncDisposeAssetsFun() {
        showProgressDialog("", "正在同步");
        SyncDisposeAssetsTask task = new SyncDisposeAssetsTask();
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                String resultString = result.getData();
                showShortText(resultString);
            });
        });
    }

    private void syncAssetsFun() {
        showProgressDialog("", "正在同步");
        String ztbh = application.getLoginAccount().getZTBH();
        SyncAssetsCategoryTask task = new SyncAssetsCategoryTask(ztbh);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                String resultString = result.getData();
                if (StringUtils.isNullOrEmpty(resultString)) {
                    showShortText("同步成功!");
                    return;
                }
                showShortText(resultString);
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }


    @OnShowRationale({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    void showRationale(PermissionRequest request) {
        // 弹出一个对话框
        new AlertDialog.Builder(this)
                .setMessage("我们需要访问您的存储空间")
                .setPositiveButton("同意", (dialog, which) -> request.proceed())
                .setNegativeButton("拒绝", (dialog, which) -> request.cancel())
                .show();
    }

    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public void requestExportFile() {
        final QMUIDialog.EditTextDialogBuilder builderDC = new QMUIDialog.EditTextDialogBuilder(mContext);
        builderDC.setTitle("系统提示")
                .setPlaceholder("输入确认码")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String text = builderDC.getEditText().getText().toString();
                    if ("123456".equals(text)) {
                        dialog.dismiss();
                        showProgressDialog("", "正在生成文件……");
                        ExportAssetsFileTask task = new ExportAssetsFileTask(mContext);
                        AsyncManager.getInstance().submitWithCallback(task, result -> {
                            mainHandler.post(() -> {
                                hideProgressDialog();
                                String data = result.getData();
                                if(PROCESS_RESULT_SUCCESS.equals(data)){
                                    showShortText("导出成功！");
                                }
                                else{
                                    showShortText("导出失败！".concat(data));
                                }
                            });
                        });
//                        new ExportAssetsFileTask().execute();
                    }
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }



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
