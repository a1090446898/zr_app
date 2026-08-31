package com.gsoft.inventory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindArray;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.JCPrinter;
import com.gsoft.inventory.service.login.SyncPrintLogTask;
import com.gsoft.inventory.service.login.SyncServerAccountsTask;
import com.gsoft.inventory.service.login.SyncZCQCYTask;
import com.gsoft.inventory.update.OkHttpUtilsUpdateService;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.view.home.MainActivity;
import com.gsoft.inventory.view.login_setting.ActivityLoginSetting;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.entity.UpdateError;
import com.xuexiang.xupdate.listener.OnUpdateFailureListener;
import com.xuexiang.xupdate.utils.UpdateUtils;

import java.util.List;

import static com.gsoft.inventory.const_data.Log_file_name.gson;
import static com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NEW_VERSION;

/**
 * 联网登录页面
 * @author 10904
 */
public class LoginActivity extends BaseCompatActivity {
  /*  @BindView(R.id.topbar)
    QMUITopBar topBar;*/
    @BindView(R.id.tv_bottom_copy)
    TextView tv_bottom_copy;


    @BindView(R.id.btn_settings)
    View btn_settings;

    @BindView(R.id.unitName)
    TextView unitName;
    @BindView(R.id.editAccount)
    EditText editAccount;
    @BindView(R.id.editPass)
    EditText editPass;

    @BindView(R.id.tv_register)
    TextView tv_register;
    @BindView(R.id.tv_forget)
    TextView tv_forget;
    @BindView(R.id.editUser)
    EditText editUser;

    @BindView(R.id.editAccountNum)
    EditText editAccountNum;


    @BindView(R.id.buttonLogin)
    QMUIButton buttonLogin;

    @BindDrawable(R.drawable.icon_user)
    Drawable icon_user;
    @BindDrawable(R.drawable.icon_pass)
    Drawable icon_pass;
    @BindDrawable(R.drawable.icon_list)
    Drawable icon_list;
    @BindDrawable(R.drawable.icon_device)
    Drawable icon_device;
    @BindDrawable(R.drawable.icon_printer)
    Drawable icon_printer;

    @BindArray(R.array.inventory_setting)
    String[] inventorySettingArray;

    String[] ztArray = null;

    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;
    private Context context;

    private String deviceId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        context = this;

        initializeView();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initializeView() {
       /* topBar.setTitle("系统登录");
        // 设置操作
        topBar.addRightImageButton(R.drawable.ic_set, R.id.qmui_tab_segment_item_id).setOnClickListener(v -> {
            // 跳转页面
            Intent intent = new Intent(mContext, ActivityLoginSetting.class);
            startActivity(intent);
        });*/

        btn_settings.setOnClickListener(view -> {
            // 跳转页面
            Intent intent = new Intent(mContext, ActivityLoginSetting.class);
            startActivity(intent);
        });

        BadgeNotificationManager badgeNotificationManager = new BadgeNotificationManager(mContext);
        // 使用新的徽章通知管理类
        badgeNotificationManager.requestNotificationAndBadgePermissions();

        String serverHost = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.SP_KEY_SERVERHOST, SysDefine.SERVERHOST);
        String ztmc = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZTMC, "");
        String yhbh = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_YHBH, "");
        String accountNum = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ACCOUNT_NUM, "");
        String yhmm = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_YHMM, "");
        deviceId = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_DEVICENO, "");
        // 获取当前手机的设备号
        if(StringUtils.isNullOrEmpty(deviceId)){
            deviceId = CommonUtils.getRandomTwoString();
        }

        if(!StringUtils.isNullOrEmpty(serverHost)){
            if (!serverHost.startsWith("http://")) {
                serverHost = "http://".concat(serverHost);
            }
            if (!serverHost.endsWith("/")) {
                serverHost = serverHost.concat("/");
            }
            SysDefine.SERVERHOST = serverHost;
        }

        if (!StringUtils.isNullOrEmpty(ztmc)) {
            editAccount.setText(ztmc);
            unitName.setText(ztmc);
        }
        if (!StringUtils.isNullOrEmpty(yhbh)) {
            editUser.setText(yhbh);
        }
        if (!StringUtils.isNullOrEmpty(accountNum)) {
            editAccountNum.setText(accountNum);
        }
        if (!StringUtils.isNullOrEmpty(yhmm)) {
            editPass.setText(yhmm);
        }

        tv_register.setOnClickListener((view) -> {
            showShortText("功能暂未开通");
        });

        tv_forget.setOnClickListener((view) -> {
            showShortText("功能暂未开通");
        });

        editAccount.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(ztArray, (dialog, which) -> {
                    editAccount.setText(ztArray[which]);
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        buttonLogin.setOnClickListener((view) -> {

            if (StringUtils.isNullOrEmpty(editAccountNum.getText().toString())) {
                showShortText("请输入账号/手机号");
                return;
            }

            // 检查是否有网络连接
            if (NetworkUtils.checkNetConnect(mContext)) {
                // 进行同步打印信息
                SyncPrintLogTask syncPrintLogTask = new SyncPrintLogTask(mContext);
                AsyncManager.getInstance().submit(syncPrintLogTask);
                // 在线登录
                String account = editAccountNum.getText().toString();
                String password = editPass.getText().toString();
                showProgressDialog("", "正在登陆");
                SyncServerAccountsTask task = new SyncServerAccountsTask(account, password);
                AsyncManager.getInstance().submitWithCallback(task, result -> {
                    mainHandler.post(() -> {
                        hideProgressDialog();
                        if (result.isSuccess()) {
                            String data = result.getData();
                            if (StringUtils.isNullOrEmpty(data)) {
                                onLoginAccess();
                            } else {
                                showShortText(data);
                            }
                        }
                    });
                });
            } else {
                onLoginAccess();
            }
        });
        showProgressDialog("", "正在同步人员");
        SyncZCQCYTask syncUserTask = new SyncZCQCYTask(mContext);
        AsyncManager.getInstance().submitWithCallback(syncUserTask, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                if (result.isSuccess()) {
                    String data = result.getData();
                    if (!StringUtils.isNullOrEmpty(data)) {
                        showShortText(data);
                    }
                }
            });
        });

        refreshPrinters();



        // 初始化更新
        initUpdateService();

        String companyName = CommonUtils.getCompanyNameByFlavor();
        tv_bottom_copy.setText(companyName + " 版权所有 v".concat(UpdateUtils.getVersionName(context)));
    }



    @Override
    protected void onResume() {
        super.onResume();
        ztArray = new String[SysConfig.getZTList().size()];
        SysConfig.getZTList().toArray(ztArray);
    }

    private void onLoginAccess() {
        // 账号可能是手机号也可能是账套编号
        String account = editAccountNum.getText().toString().trim();
        String queryParam = "";
        if (account.length() == 11) {
            // 说明是手机号，用手机号字段查询
            queryParam = "PHONE=?";
        } else {
            queryParam = "YHBH=?";
        }
        List<Accounts> accounts = Accounts.find(Accounts.class, queryParam, account);
        if (accounts.isEmpty()) {
            showShortText("登录失败，请使用手机号或者账套编号登录！");
            return;
        }
        Accounts accounts1 = accounts.get(0);
        String passStr = editPass.getText().toString().trim();
        String dlmm = accounts1.getDLMM();
        boolean isMD5 = SecurityUtils.md5(passStr).toUpperCase().equals(dlmm);
        if (!isMD5) {
            showShortText("登录失败，请联系管理员！");
            return;
        }


        String printEffect = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_EFFECT, "网页二维码-二字段");
        String printerTitle = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, "单位名称");
        String printerPosition = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, "居中");
        String qcyName = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZCQCYNAME, "");
        String qcyCode = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZCQCYCODE, "");



        application.setLoginAccount(accounts1);
        application.setDeviceNo(deviceId);
        application.setPrinterEffect(printEffect);

        if(StringUtils.isNullOrEmpty(qcyName) || StringUtils.isNullOrEmpty(qcyCode)){
            qcyName = accounts1.getYHXM();
            qcyCode = accounts1.getYHBH();
        }
        application.setZcqcyName(qcyName);
        application.setZcqcyCode(qcyCode);

        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZTMC, application.getLoginAccount().getZTMC());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_YHBH, application.getLoginAccount().getYHBH());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ACCOUNT_NUM, editAccountNum.getText().toString());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_YHMM, passStr);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_DEVICENO, deviceId);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_EFFECT, printEffect);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, printerTitle);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, printerPosition);


        // 跳转首页
//        Intent intentMain = new Intent(mContext, Activity111.class);
        Intent intentMain = new Intent(mContext, MainActivity.class);
        startActivity(intentMain);
        finish();
    }


    private void initUpdateService() {
        XUpdate.get()
                .debug(true)
                //默认设置只在wifi下检查版本更新
                .isWifiOnly(false)
                //默认设置使用get请求检查版本
                .isGet(true)
                //默认设置非自动模式，可根据具体使用配置
                .isAutoMode(false)
                //设置默认公共请求参数
                .param("versionCode", UpdateUtils.getVersionCode(this))
                .param("appKey", getPackageName())
                //设置版本更新出错的监听
                .setOnUpdateFailureListener(new OnUpdateFailureListener() {
                    @Override
                    public void onFailure(UpdateError error) {
                        //对不同错误进行处理
                        if (error.getCode() != CHECK_NO_NEW_VERSION) {
                            Toast.makeText(LoginActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                //设置是否支持静默安装，默认是true
                .supportSilentInstall(true)
                //这个必须设置！实现网络请求功能。
                .setIUpdateHttpService(new OkHttpUtilsUpdateService())
                .init(getApplication());
    }

    public void refreshPrinters() {
        OkHttpUtils.getInstance().get(SysDefine.SERVER_PRINTERS_SYNC(), new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                JCPrinter.deleteAll(JCPrinter.class);
                List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
                if(results == null || results.isEmpty()){
                    return;
                }
                for (String dataline : results) {
                    if (!dataline.contains("ERROR")) {
                        DataTransmitHelper.insertPrinters(dataline);
                    }
                }
            }

            @Override
            public void onResponseFailure(String errMessage) {
            }
        });
    }


}


