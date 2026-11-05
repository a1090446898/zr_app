
package com.gsoft.inventory.view.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.gsoft.inventory.*;
import com.gsoft.inventory.abs.BaseFragment;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.data.GridMenuItem;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.entities.BadgeNotificationPojo;
import com.gsoft.inventory.pojo.RoleMenu;
import com.gsoft.inventory.service.login.SyncGetBadgeAndNotification;
import com.gsoft.inventory.service.main.*;
import com.gsoft.inventory.service.photo.CleanPendingPhotosTask;
import com.gsoft.inventory.service.photo.PhotoMigrationTask;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.view.web.WebIndex;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import permissions.dispatcher.NeedsPermission;

import java.util.*;

import static com.gsoft.inventory.utils.SysDefine.PROCESS_RESULT_SUCCESS;

/**
 * @author 10904
 */
public class HomeFragment extends BaseFragment {

    MenuGridView menuGridView;
    List<GridMenuItem> menuItemList = new ArrayList<>();

    QMUITopBar topBar;
    TextView tv_bottom_copy;
    TextView tv_bottom_export_path;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;


    private List<RoleMenu> roleMenuList = new ArrayList<>();

    // 当前菜单层级栈（用于返回上级）
    private Deque<List<GridMenuItem>> menuStack = new ArrayDeque<>();
    // 根菜单数据（一级菜单）
    private List<GridMenuItem> rootMenus;

    private Map<String, String> badgeCountMap = new HashMap<>();

    private BadgeNotificationManager badgeNotificationManager;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 加载原MenuGridView布局
        return inflater.inflate(R.layout.activity_main, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        topBar = view.findViewById(R.id.topbar);
        menuGridView = (MenuGridView) view.findViewById(R.id.menuGridView);
        tv_bottom_copy = view.findViewById(R.id.tv_bottom_copy);
        tv_bottom_export_path = view.findViewById(R.id.tv_bottom_export_path);


        //  this.badgeNotificationManager = new BadgeNotificationManager(mContext);
        //  getSyncMenu();

        this.initHierarchicalMenusNew();
        this.showMenu(rootMenus);

        initializeView();

    }

    @SuppressLint("SetTextI18n")
    public void initializeView() {
        topBar.setTitle(getString(R.string.app_name));

        // 菜单点击事件
        menuGridView.setOnItemClickListener((parent, view, position, id) -> {
            // 迁移进行中任何菜单都不响应，并恢复遮罩，避免用户以为程序卡死
            if (PhotoMigrationTask.isRunning()) {
                showMigrationProgress();
                showShortText("照片迁移进行中，请稍候");
                return;
            }
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
                handleWebFun(currentMenu);
            }
        });


    }


    private void getSyncMenu(){
        SyncGetBadgeAndNotification syncTask = new SyncGetBadgeAndNotification(mContext);
        AsyncManager.getInstance().submitWithCallback(syncTask, result -> {
            mainHandler.post(() -> {
                if (result.isSuccess()) {
                    initHierarchicalMenus();
                }
            });
        });
    }

    private void getSyncBadgeCount() {
        SyncGetBadgeAndNotification syncTask = new SyncGetBadgeAndNotification(mContext);
        AsyncManager.getInstance().submitWithCallback(syncTask, result -> {
            mainHandler.post(() -> {
                // 增加Fragment生命周期检查，防止内存泄漏
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                if (result.isSuccess()) {
                    List<BadgeNotificationPojo> data = result.getData();

                    // 重构角标数据处理逻辑，使意图更清晰
                    processBadgeCounts(data);

                    // 重构通知处理逻辑
                    processNotification(data);

                    // 增加空值检查
                    if (rootMenus != null) {
                        // 更新所有菜单的角标数量并刷新当前菜单显示
                        updateMenuBadgeCounts(rootMenus);
                    }

                    // 检查当前菜单列表是否有效
                    if (menuItemList != null) {
                        showMenu(menuItemList);
                    }
                } else {
                    // 添加错误处理
                    Log.e("HomeFragment", "Failed to sync badge count。");
                    showShortText("同步角标数据失败");
                }
            });
        });
    }

    // 分离角标处理逻辑
    private void processBadgeCounts(List<BadgeNotificationPojo> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        // 处理除了最后一个元素外的所有数据（假设最后一个是通知数据）
        int badgeDataSize = data.size() - 1;
        for (int i = 0; i < badgeDataSize; i++) {
            BadgeNotificationPojo badgeNotificationPojo = data.get(i);
            String flowTag = badgeNotificationPojo.getFlowTag();
            String badgeCount = badgeNotificationPojo.getBadgeCount();

            if (StringUtils.isNullOrEmpty(badgeCount)) {
                badgeCount = "0";
            }

            try {
                int badgeInt = Integer.parseInt(badgeCount);
                if (badgeInt > 99) {
                    badgeCount = "99+";
                }
            } catch (NumberFormatException e) {
                // 添加异常处理
                Log.w("HomeFragment", "Invalid badge count format: " + badgeCount);
                badgeCount = "0";
            }

            badgeCountMap.put(flowTag, badgeCount);
        }
    }

    // 分离通知处理逻辑
    private void processNotification(List<BadgeNotificationPojo> data) {
        SharedPreferences prefs = mContext.getSharedPreferences("app_state", Context.MODE_PRIVATE);
        String notificationSource = prefs.getString("notification_source", "");

        // 如果是从通知栏进入的应用，则不再发送新通知以避免循环
        if ("from_app".equals(notificationSource)) {
            return;
        }

        if (data != null && !data.isEmpty()) {
            BadgeNotificationPojo badgeLastPojo = data.get(data.size() - 1);
            String allCount = badgeLastPojo.getBadgeCount();

            if (StringUtils.isNullOrEmpty(allCount)) {
                allCount = "0";
            }

            String noticeContent = badgeLastPojo.getNoticeContent();
            int count = 0;
            try {
                count = Integer.parseInt(allCount);
            } catch (NumberFormatException e) {
                Log.w("HomeFragment", "Invalid notification count format: " + allCount);
            }

            this.badgeNotificationManager.sendNotice(noticeContent, count, "from_app");
        } else {
            this.badgeNotificationManager.sendNotice("", 0, "from_app");
        }
    }

    /**
     * 递归更新所有菜单的角标数量
     */
    private void updateMenuBadgeCounts(List<GridMenuItem> menus) {
        // 添加空值检查和早期返回
        if (menus == null || menus.isEmpty()) {
            return;
        }

        for (GridMenuItem item : menus) {
            // 添加item空值检查
            if (item == null) {
                continue;
            }

            String badgeCode = item.getBadgeCode();
            if (!StringUtils.isNullOrEmpty(badgeCode)) {
                // 从badgeCountMap获取最新角标数量，默认0（兼容低版本Android）
                String badgeCount = badgeCountMap.get(badgeCode);
                if (badgeCount == null) {
                    badgeCount = "0";
                }
                // 更新菜单项的角标
                item.setBadgeCount(badgeCount);
            }

            // 如果是目录菜单，递归更新子菜单
            if (item.isDirectory()) {
                // 在递归调用前检查子菜单列表
                if (item.getSubMenus() != null) {
                    updateMenuBadgeCounts(item.getSubMenus());
                }
                // 再更新自己的角标
                item.calculateBadgeCount();
            }
        }
    }


    private void initHierarchicalMenus(){

        // this.initHierarchicalMenusOld();

        this.initHierarchicalMenusNew();

        // 同步菜单权限
        // syncMenu();

        // 同步菜单数量
        // getSyncBadgeCount();
    }

    /**
     * 新版本菜单
     */
    private void initHierarchicalMenusNew (){
        // 新版本根菜单
        rootMenus = new ArrayList<>();
        rootMenus.add(new GridMenuItem("资产列表", R.drawable.icon_list_colorful, "资产列表", ""));
        rootMenus.add(new GridMenuItem("盘点资产", R.drawable.icon_scan_barcode_colorful, "盘点资产", ""));
        rootMenus.add(new GridMenuItem("新增资产", R.drawable.icon_export_colorful, "新增资产", ""));
        rootMenus.add(new GridMenuItem("导出资产", R.drawable.icon_add2_colorful, "导出资产", ""));
        rootMenus.add(new GridMenuItem("导入资产", R.drawable.icon_import_colorful, "导入资产", ""));
        rootMenus.add(new GridMenuItem("同步分类", R.drawable.icon_sync_1_colorful, "同步分类", ""));
        rootMenus.add(new GridMenuItem("监督打印", R.drawable.icon_printer_colorful, "监督打印", ""));
        rootMenus.add(new GridMenuItem("同步上传", R.drawable.icon_sync_2_colorful, "同步上传", ""));
        rootMenus.add(new GridMenuItem("同步处置", R.drawable.icon_sync_3_colorful, "同步处置", ""));
        rootMenus.add(new GridMenuItem("照片上传", R.drawable.icon_sync_4_colorful, "照片上传", ""));
        rootMenus.add(new GridMenuItem("数字签名", R.drawable.icon_check_1_colorful, "数字签名", ""));
        rootMenus.add(new GridMenuItem("批量复制", R.drawable.icon_check_2_colorful, "批量复制", ""));
        rootMenus.add(new GridMenuItem("照片迁移", R.drawable.icon_convert, "照片迁移", ""));
        rootMenus.add(new GridMenuItem("照片清除", R.drawable.icon_save_colorful, "照片清除", ""));
        rootMenus.add(new GridMenuItem("资产盘点", R.drawable.icon_barcode, "资产盘点", ""));

        topBar.addRightTextButton("签名", R.id.qmui_tab_segment_item_id).setOnClickListener(v -> {
            startActivity(new Intent(mContext, SignatureActivity.class));
        });
    }

    /**
     * 老版本菜单
     */
    private void initHierarchicalMenusOld() {
        // 二级菜单示例：资产操作子菜单
        List<GridMenuItem> assetsMenus = new ArrayList<>();
        assetsMenus.add(new GridMenuItem("资产列表", R.drawable.icon_list, "资产列表", ""));
        assetsMenus.add(new GridMenuItem("盘点资产", R.drawable.icon_scan_barcode, "盘点资产", ""));
        assetsMenus.add(new GridMenuItem("新增资产", R.drawable.icon_add2, "新增资产", ""));
        assetsMenus.add(new GridMenuItem("导出资产", R.drawable.icon_export, "导出资产", ""));
        assetsMenus.add(new GridMenuItem("导入资产", R.drawable.icon_import, "导入资产", ""));
        assetsMenus.add(new GridMenuItem("同步分类", R.drawable.icon_sync, "同步分类", ""));
        assetsMenus.add(new GridMenuItem("监督打印", R.drawable.icon_printer, "监督打印", ""));
        assetsMenus.add(new GridMenuItem("同步上传", R.drawable.icon_sync, "同步上传", ""));
        assetsMenus.add(new GridMenuItem("同步处置", R.drawable.icon_sync, "同步处置", ""));
        assetsMenus.add(new GridMenuItem("照片上传", R.drawable.icon_sync, "照片上传", ""));
        assetsMenus.add(new GridMenuItem("数字签名", R.drawable.icon_check, "数字签名", ""));
        assetsMenus.add(new GridMenuItem("批量复制", R.drawable.icon_check, "批量复制", ""));
        assetsMenus.add(new GridMenuItem("照片清除", R.drawable.icon_save, "照片清除", ""));

        List<GridMenuItem> dressMenus = new ArrayList<>();
        dressMenus.add(new GridMenuItem("服装品目", R.drawable.icon_dress_category, "服装品目", "/pages/dress_category/index"));
        dressMenus.add(new GridMenuItem("尺码管理", R.drawable.icon_dress_size, "尺码管理", "/pages/dress_size/index"));
        dressMenus.add(new GridMenuItem("库存管理", R.drawable.icon_office_1, "库存管理-服装", "/pages/dress_stock/index"));
        dressMenus.add(new GridMenuItem("服装领用", R.drawable.icon_office_2, "服装领用", "/pages/dress_receive/index", badgeCountMap.get("WF00000071"), "WF00000071"));
        dressMenus.add(new GridMenuItem("供应商管理", R.drawable.icon_dress_supplier, "供应商管理", "/pages/dress_supplier/index"));


        List<GridMenuItem> officeMenus = new ArrayList<>();
        officeMenus.add(new GridMenuItem("审核入库", R.drawable.icon_office_4, "审核入库", "/pages/office_procurement/examAssetList", badgeCountMap.get("OFFICESUPP"), "OFFICESUPP"));
        officeMenus.add(new GridMenuItem("库存管理", R.drawable.icon_office_1, "库存管理", "/pages/office_add/index"));
        officeMenus.add(new GridMenuItem("领用申请", R.drawable.icon_office_2, "耗材领用", "/pages/office_receive/index", badgeCountMap.get("WF00000035"), "WF00000035"));
        officeMenus.add(new GridMenuItem("领用申请", R.drawable.icon_office_2, "耗材领用二", "/pages-gy-rmjcy/office_receive/index", badgeCountMap.get("WF00000068"), "WF00000068"));

        officeMenus.add(new GridMenuItem("物品入库", R.drawable.icon_stock_in, "物品入库", "/pages/office_warehouse/index", badgeCountMap.get(""), ""));
        officeMenus.add(new GridMenuItem("入库记录", R.drawable.icon_stock_in_record, "入库记录", "/pages/office_stock_record/index", badgeCountMap.get(""), ""));
        officeMenus.add(new GridMenuItem("物品出库", R.drawable.icon_stock_out, "物品出库", "/pages/office_outbound/index", badgeCountMap.get(""), ""));
        officeMenus.add(new GridMenuItem("出库记录", R.drawable.icon_stock_out_record, "出库记录", "/pages/office_stock_out_record/index", badgeCountMap.get(""), ""));


        List<GridMenuItem> contractMenus = new ArrayList<>();
        contractMenus.add(new GridMenuItem("合同登记", R.drawable.icon_contract_1, "合同登记", "/pages/contract/index"));
        contractMenus.add(new GridMenuItem("合同终止", R.drawable.icon_contract_2, "合同终止", "/pages/contract/terminate_index"));
        contractMenus.add(new GridMenuItem("合同续签", R.drawable.icon_contract_3, "合同续签", "/pages/contract/renew_index"));
        contractMenus.add(new GridMenuItem("到期预警", R.drawable.icon_contract_4, "到期预警", "/pages/contract/warning_index"));
        contractMenus.add(new GridMenuItem("付款预警", R.drawable.icon_contract_5, "付款预警", "/pages/contract/warning_pay_index"));

        List<GridMenuItem> vehicleMenus = new ArrayList<>();
        vehicleMenus.add(new GridMenuItem("年审预警", R.drawable.icon_vehicle_1, "年审预警", "/pages/vehicle_insurance_warning/index?state=01"));
        vehicleMenus.add(new GridMenuItem("保养预警", R.drawable.icon_vehicle_2, "保养预警", "/pages/vehicle_insurance_warning/index?state=02"));
        vehicleMenus.add(new GridMenuItem("保险预警", R.drawable.icon_vehicle_3, "保险预警", "/pages/vehicle_insurance_warning/index?state=03"));
        vehicleMenus.add(new GridMenuItem("车辆查询", R.drawable.icon_vehicle_4, "车辆查询", "/pages/vehicle_query/index"));
        vehicleMenus.add(new GridMenuItem("用车申请", R.drawable.icon_vehicle_5, "用车申请", "/pages/vehicle_request/index", badgeCountMap.get("WF00000011"), "WF00000011"));

        List<GridMenuItem> assetMenus = new ArrayList<>();
        assetMenus.add(new GridMenuItem("资产盘点", true, R.drawable.icon_check_1, assetsMenus));
        assetMenus.add(new GridMenuItem("资产领用", R.drawable.icon_asset_receive, "资产领用", "/pages/asset_receive/index", badgeCountMap.get("WF00000022"), "WF00000022"));

        // 通用资产采购
        assetMenus.add(new GridMenuItem("资产采购", R.drawable.icon_asset_purchase, "资产采购", "/pages/asset_procurement/index", badgeCountMap.get("WF00000044"), "WF00000044"));
        assetMenus.add(new GridMenuItem("资产采购", R.drawable.icon_asset_purchase, "资产采购九", "/pages-gy-rmjcy/asset_procurement/index", badgeCountMap.get("WF00000044"), "WF00000044"));

        assetMenus.add(new GridMenuItem("资产移交", R.drawable.icon_asset_transfer, "资产移交", "/pages/asset_transfer/index", badgeCountMap.get("WF00000042"), "WF00000042"));
        assetMenus.add(new GridMenuItem("资产移交", R.drawable.icon_asset_transfer, "资产移交四", "/pages-gy-rmjcy/asset_transfer/index", badgeCountMap.get("WF00000042"), "WF00000042"));


        assetMenus.add(new GridMenuItem("资产处置", R.drawable.icon_asset_dispose, "资产处置", "/pages/asset_scrap/index", badgeCountMap.get("WF00000043"), "WF00000043"));
        assetMenus.add(new GridMenuItem("资产报修", R.drawable.icon_repair, "资产报修", "/pages/asset_repair/index"));

        assetMenus.add(new GridMenuItem("设备借用", R.drawable.icon_asset_borrow, "设备借用", "/pages/asset_borrow/index", badgeCountMap.get("WF00000059"), "WF00000059"));
        assetMenus.add(new GridMenuItem("设备借用", R.drawable.icon_asset_borrow, "设备借用二", "/pages-gy-rmjcy/asset_borrow/index", badgeCountMap.get("WF00000059"), "WF00000059"));
        assetMenus.add(new GridMenuItem("场所申请", R.drawable.icon_asset_borrow, "场所管理二", "/pages-gy-rmjcy/room_borrow/index", badgeCountMap.get("WF00000059"), "WF00000059"));


        assetMenus.add(new GridMenuItem("设备归还", R.drawable.icon_asset_return, "设备归还", "/pages/asset_return_new/index", badgeCountMap.get("WF00000069"), "WF00000069"));
        assetMenus.add(new GridMenuItem("设备归还", R.drawable.icon_asset_return, "设备归还二", "/pages/asset_return_new/index", badgeCountMap.get("WF00000069"), "WF00000069"));
        assetMenus.add(new GridMenuItem("场所归还", R.drawable.icon_asset_return, "场所归还二", "/pages-gy-rmjcy/room_return_new/index", badgeCountMap.get("WF00000070"), "WF00000070"));


        assetMenus.add(new GridMenuItem("变动日志", R.drawable.icon_log, "变动日志", "/pages/log_list/index"));

        List<GridMenuItem> testMenus = new ArrayList<>();
        //  testMenus.add(new GridMenuItem("用户信息", R.drawable.icon_vehicle_1, "PDF预览", ""));


        // 根菜单
        rootMenus = new ArrayList<>();
        rootMenus.add(new GridMenuItem("资产管理", true, R.drawable.icon_asset_manager, "资产管理",assetMenus));
        rootMenus.add(new GridMenuItem("服装管理", true, R.drawable.icon_dress, "服装管理",dressMenus));
        rootMenus.add(new GridMenuItem("办公用品", true, R.drawable.icon_office, "办公用品",officeMenus));
        rootMenus.add(new GridMenuItem("车辆管理", true, R.drawable.icon_vehicle_insurance_warning, "车辆管理", vehicleMenus));
        rootMenus.add(new GridMenuItem("合同管理", true, R.drawable.icon_contract, "合同管理", contractMenus));

        rootMenus.add(new GridMenuItem("办公采购", R.drawable.icon_office_3, "办公采购", "/pages/office_procurement/index", badgeCountMap.get("WF00000012"), "WF00000012"));
        rootMenus.add(new GridMenuItem("办公采购", R.drawable.icon_office_3, "办公采购二", "/pages-gy-rmjcy/office_procurement/index", badgeCountMap.get("WF00000067"), "WF00000067"));

        rootMenus.add(new GridMenuItem("值班调休", R.drawable.icon_leave_shift_rotation, "值班调休", "/pages/leave_shift_rotation/index", badgeCountMap.get("WF00000072"), "WF00000072"));
        rootMenus.add(new GridMenuItem("请假管理", R.drawable.icon_leave_manage, "请假管理", "/pages/leave_management/index", badgeCountMap.get("WF00000074"), "WF00000074"));
        rootMenus.add(new GridMenuItem("打卡报备", R.drawable.icon_clock_in_report, "打卡报备", "/pages/clock_in_report/index", badgeCountMap.get("WF00000073"), "WF00000073"));

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


        // 过滤出有权限的菜单
        Iterator<GridMenuItem> iterator1 = menuItemList.iterator();
        while (iterator1.hasNext()) {
            GridMenuItem menuItem = iterator1.next();
            String functionType = menuItem.getFunctionType();
            boolean hasPermission = true;

            // 检查当前菜单是否有权限
            for (RoleMenu roleMenu : roleMenuList) {
                String menuName = roleMenu.getMenuName();
                Boolean hasRole = roleMenu.getHasRole();
                if(StringUtils.isNullOrEmpty(functionType)){
                    continue;
                }
                if (functionType.equals(menuName)) {
                    hasPermission = hasRole;
                    break;
                }
            }

            // 无权限则从迭代器中安全移除
            if (!hasPermission) {
                iterator1.remove();
            }
        }

        // 如果不是根菜单，添加"返回上级"项
        if (!menuStack.isEmpty()) {
            menuItemList.add(new GridMenuItem("返回上级", R.drawable.icon_back, "返回上级", ""));
        }
        MenuGridAdapter adapter = new MenuGridAdapter(mContext, menuItemList);
        menuGridView.setAdapter(adapter);
        // 当需要更新数据时，直接调用具体适配器的 notifyDataSetChanged()
        adapter.notifyDataSetChanged();
    }

    private void handleWebFun (GridMenuItem  menuItem) {
        String webUrl = menuItem.getWebUrl();
        if(StringUtils.isNullOrEmpty(webUrl)){
            handleFunction(menuItem.getFunctionType());
            return;
        }
        Intent intentWeb = new Intent(mContext, WebIndex.class);
        intentWeb.putExtra("webUrl",  webUrl);
        startActivity(intentWeb);
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
                boolean hasLocalRecord = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{Const.NOT_UPLOAD}) > 0;
                if (hasLocalRecord) {
                    showShortText("当前有未上传的资产数据，请先同步上传！");
                    break;
                }
                Intent intentImport = new Intent(mContext, ImportNetAssetsActivity.class);
                startActivity(intentImport);
                break;
            case "导入账套":
                syncServerAccountsFun();
                break;
            case "同步分类":
                syncAssetsFun();
                break;
            case "导出资产":
                this.handleExportData();
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
            case "同步上传":
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
            case "照片迁移":
                this.confirmPhotoMigration();
                break;
            case "照片清除":
                this.cleanPhoto();
                break;
            case "资产盘点":
                Intent intentRfid = new Intent(mContext, RfidStatsActivity.class);
                startActivity(intentRfid);
                break;
            default:
                break;
        }
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
     * 启动迁移任务：任务只写静态进度，页面轮询刷新，Fragment重建不会丢进度
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
     * Fragment是否可安全操作UI
     */
    private boolean isFragmentUsable() {
        return isAdded() && getActivity() != null
                && !getActivity().isFinishing() && !getActivity().isDestroyed();
    }

    /**
     * 显示迁移进度遮罩
     */
    private void showMigrationProgress() {
        if (!isFragmentUsable()) {
            return;
        }
        if (migrationProgressDialog == null) {
            migrationProgressDialog = new CustomProgressDialog(getActivity());
        }
        refreshMigrationProgress();
        if (!migrationProgressDialog.isShowing()) {
            migrationProgressDialog.show();
        }
        // 迁移期间保持屏幕常亮，降低息屏后进程被回收导致任务中断的概率
        if (getActivity().getWindow() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
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
                if (!isFragmentUsable()) {
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
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

    private void syncAssetsTaskFun() {

        long waitingSyncCount = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{Const.NOT_UPLOAD});
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
                    if (!NetworkUtils.checkNetConnect(mContext)) {
                        showShortText("当前网络连接不正常，请稍后再试！");
                        return;
                    }

                    showProgressDialog("", "正在同步");
                    SyncAssetsTask task = new SyncAssetsTask(mContext, application.getZcqcyCode());
                    AsyncManager.getInstance().submitWithCallback(task, result -> {
                        mainHandler.post(() -> {
                            hideProgressDialog();
                            String resultString = result.getData();
                            if ("存在图片同步失败，已停止数据同步！".equals(resultString)) {
                                showShortText(resultString);
                            } else if ("图片同步被中断，请重试".equals(resultString)) {
                                showShortText(resultString);
                            } else {
                                showShortText(resultString);
                            }
                        });
                    });
                    dialog.dismiss();
                }).show();
    }

    private void syncMenu() {
        showProgressDialog("", "正在同步菜单");
        String userId = application.getLoginAccount().getYHBH();
        if(userId == null){
            showShortText("当前用户信息异常，请退出APP重新登录！");
            return;
        }
        MenuRoleTask task = new MenuRoleTask(userId);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                List<RoleMenu> menuRoleList = result.getData();
                roleMenuList.clear();
                roleMenuList.addAll(menuRoleList);
                showMenu(rootMenus);

                for (RoleMenu roleMenu : roleMenuList) {
                    String menuName = roleMenu.getMenuName();
                    Boolean hasRole = roleMenu.getHasRole();
                    if("短信签名".equals(menuName) && hasRole){
                        topBar.addRightTextButton("签名", R.id.qmui_tab_segment_item_id).setOnClickListener(v -> {
                            startActivity(new Intent(mContext, SignatureActivity.class));
                        });
                    }
                }
            });
        });
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


    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private void handleExportData () {
        QMUIDialog.EditTextDialogBuilder builderDC = new QMUIDialog.EditTextDialogBuilder(mContext);
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
                                if (PROCESS_RESULT_SUCCESS.equals(data)) {
                                    showShortText("导出成功！");
                                } else {
                                    showShortText("导出失败！".concat(data));
                                }
                            });
                        });
                    }
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show();
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
        String message =  "该功能会删除当前设备上所有“待上传”的资产照片记录。\n删除对应的本地照片文件，用于释放手机存储空间。\n\n"
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
        if (!isAdded() || getActivity() == null) {
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
                    if (!isAdded() || getActivity() == null) {
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

    @Override
    public void onResume() {
        super.onResume();
        // 页面重建或从后台切回时恢复进度遮罩
        if (PhotoMigrationTask.isRunning()) {
            showMigrationProgress();
        }
    }

    @Override
    public void onDestroyView() {
        // 只关闭弹窗，迁移任务在后台继续执行，回到首页可继续查看进度
        hideMigrationProgress();
        super.onDestroyView();
    }
}