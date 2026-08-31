package com.gsoft.inventory.view.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gsoft.inventory.R;
import com.gsoft.inventory.SignatureActivity;
import com.gsoft.inventory.abs.BaseFragment;
import com.gsoft.inventory.entities.BadgeNotificationPojo;
import com.gsoft.inventory.pojo.RoleMenu;
import com.gsoft.inventory.service.login.SyncGetBadgeAndNotification;
import com.gsoft.inventory.service.main.MenuRoleTask;
import com.gsoft.inventory.utils.*;
import com.xuexiang.xupdate.XUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author 10904
 */
public class MainActivity extends BaseCompatActivity {

    private ViewPager2 viewPager2;
    private TabLayout tabLayout;

    private List<BaseFragment> fragmentList;

    private List<RoleMenu> roleMenuList = new ArrayList<>();

    boolean isExit = false;

    boolean hasAssetRole = false;

    private BadgeNotificationManager badgeNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_tab);

        this.initializeView();
    }

    @Override
    public void initializeView() {
        if (application.getLoginAccount() == null || StringUtils.isNullOrEmpty(application.getPrinterDevice())) {
            showShortText("登录超时，请退出APP重新登录！");
            return;
        }

        // 初始化ViewPager2和TabLayout
        viewPager2 = findViewById(R.id.viewPager2);
        tabLayout = findViewById(R.id.tabLayout);


        this.syncMenu();

        // 添加页面切换监听器
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
            }
        });

        // 通知处理
        this.badgeNotificationManager = new BadgeNotificationManager(mContext);
        getSyncBadgeCount();


        // 检查版本更新
        String url = String.format(SysDefine.SERVER_APK_UPDATE(), application.getLoginAccount().getZTBH());
        XUpdate.newBuild(mContext)
                .updateUrl(url)
                .update();
    }

    private void initTab() {
        HomeFragment homeFragment =  new HomeFragment();
        WebHomeFragment webHomeFragment =  new WebHomeFragment();
        MyFragment myFragment = new MyFragment();

        Bundle webHomeFragmentArgs = new Bundle();
        webHomeFragmentArgs.putString("targetWebUrl", "/pages/home/index");
        webHomeFragment.setArguments(webHomeFragmentArgs);

        Bundle myFragmentArgs = new Bundle();
        myFragmentArgs.putString("targetWebUrl", "/pages/userSetting/detail");
        myFragment.setArguments(myFragmentArgs);

        fragmentList = new ArrayList<>();

        if(this.hasAssetRole){
            this.fragmentList.add(homeFragment);
        }
        this.fragmentList.add(webHomeFragment);
        this.fragmentList.add(myFragment);
        // 使用自定义适配器
        MainPagerAdapter mainPagerAdapter = new MainPagerAdapter(this, fragmentList);
        //
        viewPager2.setAdapter(mainPagerAdapter);

        // 禁止左右滑动切换
        viewPager2.setUserInputEnabled(false);
        // 设置保留Fragment实例，解决切换后菜单无法点击问题
        viewPager2.setOffscreenPageLimit(1);
        // 关联TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
            if(this.hasAssetRole){
                // 设置标签文本（与 XML 中 TabItem 对应）
                if (position == 0) {
                    tab.setText("资产盘点");
                    tab.setIcon(R.drawable.ic_asset_check);
                }
                else if(position == 1){
                    tab.setText("工作台");
                    tab.setIcon(R.drawable.ic_workbench);
                }
                else if(position == 2){
                    tab.setText("我的");
                    tab.setIcon(R.drawable.ic_my);
                }

            }else{
                if (position == 0) {
                    tab.setText("工作台");
                    tab.setIcon(R.drawable.ic_workbench);
                }
                else if(position == 1){
                    tab.setText("我的");
                    tab.setIcon(R.drawable.ic_my);
                }
            }
        }).attach();

        if(this.hasAssetRole){
            Objects.requireNonNull(tabLayout.getTabAt(1)).select();
        }
        else{
            Objects.requireNonNull(tabLayout.getTabAt(0)).select();
        }

    }


    private void getSyncBadgeCount() {
        SyncGetBadgeAndNotification syncTask = new SyncGetBadgeAndNotification(mContext);
        AsyncManager.getInstance().submitWithCallback(syncTask, result -> {
            mainHandler.post(() -> {

                if (result.isSuccess()) {
                    List<BadgeNotificationPojo> data = result.getData();

                    // 通知处理逻辑
                    processNotification(data);

                } else {
                    // 改进6: 添加错误处理
                    Log.e("HomeFragment", "Failed to sync badge count。");
                    showShortText("同步角标数据失败");
                }
            });
        });
    }


    // 改进9: 分离通知处理逻辑
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
        BaseFragment currentFragment = fragmentList.get(viewPager2.getCurrentItem());

        if (currentFragment instanceof WebHomeFragment) {
            WebHomeFragment webFragment = (WebHomeFragment) currentFragment;
            // 如果 WebView 还能返回上一页，就不退出应用
            if (webFragment.handleBackPress()) {
                return;
            }
        }

        // 其他情况下执行双击退出逻辑
        if (!isExit) {
            isExit = true;
            showShortText("再按一次退出程序");
            mainHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            finish();
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

                for (RoleMenu roleMenu : roleMenuList) {
                    String menuName = roleMenu.getMenuName();
                    Boolean hasRole = roleMenu.getHasRole();
                    if("资产盘点".equals(menuName)){
                        this.hasAssetRole = hasRole;
                        break;
                    }
                }
                this.initTab();
            });
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 将结果转发给当前显示的 Fragment
        BaseFragment currentFragment = fragmentList.get(viewPager2.getCurrentItem());
        if (currentFragment != null) {
            currentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
