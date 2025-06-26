package com.gsoft.inventory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import butterknife.BindArray;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.PrintLogInfo;
import com.gsoft.inventory.entities.ZCQCY;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.entity.UpdateEntity;
import com.xuexiang.xupdate.proxy.IUpdatePrompter;
import com.xuexiang.xupdate.proxy.IUpdateProxy;
import com.xuexiang.xupdate.service.OnFileDownloadListener;
import com.xuexiang.xupdate.utils.UpdateUtils;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联网登录页面
 */
public class LoginActivity extends BaseCompatActivity {
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.tv_bottom_copy)
    TextView tv_bottom_copy;

    @BindView(R.id.unitName)
    TextView unitName;
    @BindView(R.id.editAccount)
    EditText editAccount;
    @BindView(R.id.editPass)
    EditText editPass;
    @BindView(R.id.editUser)
    EditText editUser;
    @BindView(R.id.editDeviceNo)
    EditText editDeviceNo;
    @BindView(R.id.editPrinter)
    EditText editPrinter;
    @BindView(R.id.editPrinterTitle)
    EditText editPrinterTitle;

    @BindView(R.id.editPrinterPosition)
    EditText editPrinterPosition;
    @BindView(R.id.editQCY)
    EditText editQCY;

    @BindView(R.id.editInventorySetting)
    EditText editInventorySetting;
    @BindView(R.id.buttonLogin)
    QMUIButton buttonLogin;
    @BindView(R.id.buttonImportAccounts)
    QMUIButton buttonImportAccounts;
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
    @BindArray(R.array.printe_effect)
    String[] printeEffectArray;

    @BindArray(R.array.print_title_select)
    String[] printTitleSelect;

    @BindArray(R.array.print_position_select)
    String[] printPositionSelect;

    @BindArray(R.array.printfieldsTitle)
    String[] printfieldsTitle;

    @BindArray(R.array.printfieldsCode)
    String[] printfieldsCode;

    @BindArray(R.array.printfieldsCodeCz)
    String[] printfieldsCodeCz;

    @BindArray(R.array.printfieldsTitleCz)
    String[] printfieldsTitleCz;

    @BindView(R.id.textViewSelect)
    TextView textViewSelect;

    String[] ztArray = null;

    String[] sbArray = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D"};
    private String accountsCode = null;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;
    private Context context;

    private Gson gson = new Gson();

    private Map<String, String> filedsMap = new HashMap<>();

    List<ZCQCY> zcqcyArray = null;
    String[] zcqcnames = null;
    String qcyName = "";
    String qcyCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        context = this;

        // 初始化filedsMap数据
        for(int i = 0; i < printfieldsTitle.length; i++){
            filedsMap.put( printfieldsCode[i], printfieldsTitle[i]);
        }

        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("系统登录");
        // 设置操作
        topBar.addRightImageButton(R.drawable.ic_set, R.id.qmui_tab_segment_item_id).setOnClickListener(v -> {
            final QMUIDialog.EditTextDialogBuilder builderSetting = new QMUIDialog.EditTextDialogBuilder(mContext);
            String oldServerHost = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.SP_KEY_SERVERHOST, SysDefine.SERVERHOST);

            builderSetting.setTitle("服务器地址")
                    .setPlaceholder("不带http://")
                    .setInputType(InputType.TYPE_CLASS_TEXT)
                    .setDefaultText(oldServerHost)
                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                    .addAction("确定", (dialog, index) -> {
                        String text = builderSetting.getEditText().getText().toString().trim();
                        if (!text.startsWith("http://")) {
                            text = "http://".concat(text);
                        }
                        if (!text.endsWith("/")) {
                            text = text.concat("/");
                        }
                        SysDefine.setServerHost(text);
//                        SysDefine.resetServerURL();
                        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.SP_KEY_SERVERHOST, text);
                        dialog.dismiss();
                    })
                    .create(mCurrentDialogStyle).show();
        });
        findViewById(R.id.buttonPrint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMultiChoiceDialog();
            }
        });



        String ztmc = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZTMC, "");
        String yhbh = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_YHBH, "");
        String yhmm = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_YHMM, "");
        String sbhm = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_DEVICENO, "");
        String printer = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_EFFECT, "大一维码");
        String printerTitle = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, "单位名称");
        String printerPosition = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, "打印位置");
        String inventorySetting = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_INVENTORY_SETTING, "");
        String webserver = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.SP_KEY_SERVERHOST, "");
        String printFieldsCode =SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "");
        qcyName = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZCQCYNAME, "");
        qcyCode = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZCQCYCODE, "");

        String fields = "BARCODEID,ASSETSNAME";
        if (!StringUtils.isNullOrEmpty(webserver)) {
            SysDefine.setServerHost(webserver);
        }
        if (!StringUtils.isNullOrEmpty(ztmc)) {
            editAccount.setText(ztmc);
            unitName.setText(ztmc);
        }
        if (!StringUtils.isNullOrEmpty(yhbh)) {
            editUser.setText(yhbh);
        }
        if (!StringUtils.isNullOrEmpty(yhmm)) {
            editPass.setText(yhmm);
        }
        if (!StringUtils.isNullOrEmpty(sbhm)) {
            editDeviceNo.setText(sbhm);
        }
        if (!StringUtils.isNullOrEmpty(printer)) {
            editPrinter.setText(printer);
        }
        else{
            editPrinter.setText("网页二维码-二字段");
        }
        if (!StringUtils.isNullOrEmpty(printerTitle)) {
            editPrinterTitle.setText(printerTitle);
        }
        if (!StringUtils.isNullOrEmpty(printerPosition)) {
            editPrinterPosition.setText(printerPosition);
        }

        if(!StringUtils.isNullOrEmpty(qcyName)){
            editQCY.setText(qcyName);
        }
        if(!StringUtils.isNullOrEmpty(inventorySetting)){
            editInventorySetting.setText(inventorySetting);
        }

        if(StringUtils.isNullOrEmpty(printFieldsCode)){
            textViewSelect.setText("打印字段：" + "资产名称");
            SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, fields);
        }
        else{
            // 将printFieldsCode通过,分割成数组
            String text = "";
            String[] printFieldsCodeArray = printFieldsCode.split(",");
            for(int i = 0; i < printFieldsCodeArray.length; i++){
                String s = filedsMap.get(printFieldsCodeArray[i]);
                if(s != null){
                    text += s;
                }
                if(!StringUtils.isNullOrEmpty(text) && i < printFieldsCodeArray.length - 1){
                    text += ",";
                }
            }
            textViewSelect.setText("打印字段：" + text);
        }


        //必须设置图片的大小否则没有作用
        icon_user.setBounds(0, 0, 60, 60);
        icon_pass.setBounds(0, 0, 60, 60);
        icon_list.setBounds(0, 0, 60, 60);
        icon_device.setBounds(0, 0, 60, 60);
        icon_printer.setBounds(0, 0, 60, 60);
        editAccount.setCompoundDrawables(icon_list, null, null, null);
        editUser.setCompoundDrawables(icon_user, null, null, null);
        editPass.setCompoundDrawables(icon_pass, null, null, null);
        editDeviceNo.setCompoundDrawables(icon_device, null, null, null);
        editPrinter.setCompoundDrawables(icon_printer, null, null, null);
        editPrinterTitle.setCompoundDrawables(icon_printer, null, null, null);
        editPrinterPosition.setCompoundDrawables(icon_printer, null, null, null);
        editQCY.setCompoundDrawables(icon_user, null, null, null);
        editInventorySetting.setCompoundDrawables(icon_user, null, null, null);


        editInventorySetting.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(inventorySettingArray, (dialog, which) -> {
                    String whichStr = inventorySettingArray[which];
                    editInventorySetting.setText(whichStr);
                    cleanQcy();
                    SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_INVENTORY_SETTING, whichStr);
                    dialog.dismiss();
                    resetZCQCY();
                })
                .create(mCurrentDialogStyle).show());
        editPrinter.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                
                .addItems(printeEffectArray, (dialog, which) -> {
                    editPrinter.setText(printeEffectArray[which]);
                    if (printeEffectArray[which].equals("网页二维码-二字段")) {
                        SharedPreferencesUtils.deleShare(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE);
                        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, fields);
                    }
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editPrinterTitle.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                
                .addItems(printTitleSelect, (dialog, which) -> {
                    editPrinterTitle.setText(printTitleSelect[which]);
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editPrinterPosition.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                
                .addItems(printPositionSelect, (dialog, which) -> {
                    editPrinterPosition.setText(printPositionSelect[which]);
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editAccount.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                
                .addItems(ztArray, (dialog, which) -> {
                    editAccount.setText(ztArray[which]);
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        buttonImportAccounts.setOnClickListener(view -> new SyncServerAccountsTask().execute());
        buttonLogin.setOnClickListener((view) -> {

            if (StringUtils.isNullOrEmpty(editUser.getText().toString())) {
                showShortText("请输入用户名");
                return;
            }
            if (StringUtils.isNullOrEmpty(qcyName) || StringUtils.isNullOrEmpty(qcyCode)) {
                showShortText("请选择资产清查人员");
                return;
            }
            if (StringUtils.isNullOrEmpty(editDeviceNo.getText().toString())) {
                showShortText("请选择设备编号");
                return;
            }
            if (StringUtils.isNullOrEmpty(editPrinter.getText().toString())) {
                showShortText("请选择打印设备");
                return;
            }
            // 检查是否有网络连接
            if (NetworkUtils.checkNetConnect(mContext)) {
                // 进行同步打印信息
                new SyncPrintLogTask().execute();
                // 在线登录
                new SyncServerAccountsTask().execute(
                        editUser.getText().toString().trim(), editPass.getText().toString().trim()
                );
            } else {
                onLoginAccess();
            }
        });
        new SyncZCQCYTask().execute("");
        // 检查版本更新
        XUpdate.newBuild(mContext)
                .updateUrl(SysDefine.SERVER_APK_UPDATE())
                .update();

        tv_bottom_copy.setText("贵州兴财通科技有限公司  版权所有 v".concat(UpdateUtils.getVersionName(context)));
    }


    @Override
    protected void onResume() {
        super.onResume();
        ztArray = new String[SysConfig.getZTList().size()];
        SysConfig.getZTList().toArray(ztArray);
    }


    /**
     * 清空清查员
     */
    private void cleanQcy () {
        this.editQCY.setText("");
        this.qcyName = "";
        this.qcyCode = "";
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYNAME, "");
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYCODE, "");
    }

    private void resetZCQCY() {

        String inventorySetting = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_INVENTORY_SETTING, "");
        String userType = "";
        if("智软盘点".equals(inventorySetting)){
            userType = "01";
        }
        else{
            userType = "02";
        }
        zcqcyArray = ZCQCY.find(ZCQCY.class, "USER_TYPE = ?", userType);
        zcqcnames = new String[zcqcyArray.size()];
        for (int i = 0; i < zcqcyArray.size(); i++) {
            zcqcnames[i] = zcqcyArray.get(i).NAME;
        }
        editQCY.setOnClickListener(view -> {
            new QMUIDialog.CheckableDialogBuilder(mContext)
                    
                    .addItems(zcqcnames, (dialog, i) -> {
                        editQCY.setText(zcqcnames[i]);
                        qcyName = zcqcnames[i];
                        qcyCode = zcqcyArray.get(i).CODE;
                        dialog.dismiss();
                    })
                    .create(mCurrentDialogStyle).show();
        });
    }

    StringBuilder sbFields = new StringBuilder();
    StringBuilder sbTips = new StringBuilder();

    private void showMultiChoiceDialog() {
        String textPrinter = editPrinter.getText().toString();
        String[] fields = null;
        String[] fieldsCode = null;
        if ("财政二维码".equals(textPrinter)) {
            fields = printfieldsTitleCz;
            fieldsCode = printfieldsCodeCz;
        } else {
            fields = printfieldsTitle;
            fieldsCode = printfieldsCode;
        }

        String[] finalFields = fields;
        String[] finalFieldsCode = fieldsCode;
        final QMUIDialog.MultiCheckableDialogBuilder builder = new QMUIDialog.MultiCheckableDialogBuilder(context)
                .addItems(fields, (dialog, which) -> {
                    if (sbFields.indexOf(finalFieldsCode[which]) == -1) {
                        sbFields.append(finalFieldsCode[which]).append(",");
                        sbTips.append(finalFields[which]).append(",");
                    }
                });
        builder.addAction("取消", (dialog, index) -> dialog.dismiss());
        builder.addAction("提交", (dialog, index) -> {
            if (builder.getCheckedItemIndexes().length < 1 || sbFields.length() < 1) {
                Toast.makeText(context, "必须选择要打印的字段", Toast.LENGTH_SHORT).show();
                return;
            }

            // 去掉结尾处的逗号
            if (sbFields.length() > 0) {
                sbFields.deleteCharAt(sbFields.length() - 1);
                sbTips.deleteCharAt(sbTips.length() - 1);
            }
            textViewSelect.setText("打印字段：" + sbTips.toString());
            showShortText(sbTips.toString());
            SharedPreferencesUtils.deleShare(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE);
            SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, sbFields.toString());
            dialog.dismiss();
        });
        if (sbFields.length() > 0) {
            sbFields.delete(0, sbFields.length());
            sbTips.delete(0, sbTips.length());
        }
        builder.create(mCurrentDialogStyle).show();
    }

    long clicktimestrap = 0;
    int clickCount = 0;

    class SyncServerAccountsTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步");
        }

        @Override
        protected String doInBackground(String... params) {
            String account = params[0];
            String pwd = params[1];
            String s = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_SYNC_LOGINACCOUNTS(), account.trim(), SecurityUtils.md5(pwd.trim()).toUpperCase()));
            if (StringUtils.isNullOrEmpty(s) || s.contains("ERROR")) {
                return "登录失败，用户名或密码错误！";
            }
            StringBuilder stringBuilder = new StringBuilder();
            try {
                List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {}.getType());
                if (results == null || results.isEmpty()) {
                    return "登录失败，无效的用户名！";
                }
                Accounts.deleteAll(Accounts.class);
                for (String dataLine : results) {
                    long newID = DataTransmitHelper.insertAccounts(dataLine);
                    if (newID == -1) {
                        stringBuilder.append("登录出现异常，");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                stringBuilder.append("登录失败，请联系技术人员\n");
            }
            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (StringUtils.isNullOrEmpty(s)) {
                showShortText("登录成功！");
                onLoginAccess();
            } else {
                showShortText(s);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }
    }

    class SyncPrintLogTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            String jsonStr = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO, "");
            if (!StringUtils.isNullOrEmpty(jsonStr)) {
                List<PrintLogInfo> printLogInfoList = gson.fromJson(jsonStr, new TypeToken<List<PrintLogInfo>>() {
                }.getType());
                JsonArray jsonArray = new JsonArray();
                for (PrintLogInfo item : printLogInfoList) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("data", item.toLineString());
                    jsonArray.add(jsonObject);
                }
                result = OkHttpUtils.getInstance().postAssetsJson(SysDefine.PRINT_LOG_URL(), gson.toJson(jsonArray));
                if (result.contains("\"result\":\"OK\"")) {
                    SharedPreferencesUtils.deleShare(mContext, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO);
                }
            }
            return result;
        }
    }

    private void onLoginAccess() {
        List<Accounts> accounts = Accounts.find(Accounts.class, "YHBH=?", editUser.getText().toString().trim());
        Accounts accounts1 = accounts.get(0);
        String passStr = editPass.getText().toString().trim();
        String dlmm = accounts1.getDLMM();
        boolean isMD5 = SecurityUtils.md5(passStr).toUpperCase().equals(dlmm);
        if (!isMD5) {
            showShortText("登录失败，请联系管理员！");
            return;
        }

        application.setLoginAccount(accounts1);
        application.setDeviceNo(editDeviceNo.getText().toString());
        application.setPrinterEffect(editPrinter.getText().toString());
        application.setZcqcyName(qcyName);
        application.setZcqcyCode(qcyCode);
        Intent intentMain = new Intent(mContext, MainActivity.class);
        startActivity(intentMain);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZTMC, application.getLoginAccount().getZTMC());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_YHBH, application.getLoginAccount().getYHBH());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_YHMM, passStr);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_DEVICENO, editDeviceNo.getText().toString());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_EFFECT, editPrinter.getText().toString());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, editPrinterTitle.getText().toString());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, editPrinterPosition.getText().toString());
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYNAME, qcyName);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYCODE, qcyCode);
        finish();
    }

    /**
     * 同步资产人员
     */
    class SyncZCQCYTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步");
        }

        @Override
        protected String doInBackground(String... params) {
            // 账套编号
            String ztbh = "";

            // 用户名
            String yhbh = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_YHBH, "");
            if(!StringUtils.isNullOrEmpty(yhbh)){
                // 通过用户名取查找
                List<Accounts> accounts = Accounts.find(Accounts.class, "YHBH=?", yhbh);
                if(!accounts.isEmpty()){
                    ztbh = accounts.get(0).getZTBH();
                    if(StringUtils.isNullOrEmpty(ztbh)){
                        ztbh = "";
                    }
                }
            }
            String s = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_SYNC_ZCQCY(), ztbh));
            if (StringUtils.isNullOrEmpty(s) || s.contains("ERROR")) {
                return "同步资产清查人员失败！";
            }
            StringBuilder stringBuilder = new StringBuilder();
            try {
                List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {}.getType());
                if (results == null || results.isEmpty()) {
                    return "同步资产清查人员失败！";
                }
                ZCQCY.deleteAll(ZCQCY.class);
                for (String dataLine : results) {
                    long newID = DataTransmitHelper.insertZCQCY(dataLine);
                    if (newID == -1) {
                        stringBuilder.append("同步资产清查人员出现异常，");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                stringBuilder.append("同步资产清查人员失败，请联系技术人员\n");
            }
            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (StringUtils.isNullOrEmpty(s)) {
                showShortText("同步资产清查人员成功！");
            } else {
                showShortText(s);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 82) {
            if (clicktimestrap == 0) {
                clicktimestrap = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - clicktimestrap > 3000) {
                clicktimestrap = 0;
                clickCount = 0;
            } else if (System.currentTimeMillis() - clicktimestrap < 3000 && clickCount >= 5) {
                importPrinter();
            } else if (System.currentTimeMillis() - clicktimestrap < 3000) {
                clickCount++;
            }
        } else if (keyCode == 139) {
            Intent print = new Intent(mContext, PrintLabelActivity.class);
            startActivity(print);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void importPrinter() {
        final QMUIDialog.EditTextDialogBuilder builderDC = new QMUIDialog.EditTextDialogBuilder(mContext);
        builderDC.setTitle("系统提示")
                .setPlaceholder("输入确认码")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String text = builderDC.getEditText().getText().toString();
                    if (!StringUtils.isNullOrEmpty(text) && text.equals("1234560")) {
                        dialog.dismiss();
                        Intent intentImportPrinter = new Intent(mContext, ImportPrintersActivity.class);
                        startActivity(intentImportPrinter);
                        clicktimestrap = 0;
                        clickCount = 0;
                    }
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show();
    }
}

/*
class CustomUpdatePrompter implements IUpdatePrompter {


    private Context mContext;

    public CustomUpdatePrompter(Context context) {
        mContext = context;
    }

    @Override
    public void showPrompt(@NonNull UpdateEntity updateEntity, @NonNull IUpdateProxy updateProxy) {
        showUpdatePrompt(updateEntity, updateProxy);
    }

    */
/**
     * 显示自定义提示
     *
     * @param updateEntity
     * @param updateProxy
     *//*

    private void showUpdatePrompt(final @NonNull UpdateEntity updateEntity, final @NonNull IUpdateProxy updateProxy) {
        String updateInfo = UpdateUtils.getDisplayUpdateInfo(updateEntity);

        new AlertDialog.Builder(mContext)
                .setTitle(String.format("是否升级到%s版本？", updateEntity.getVersionName()))
                .setMessage(updateInfo)
                .setPositiveButton("升级", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateProxy.startDownload(updateEntity, new OnFileDownloadListener() {
                            @Override
                            public void onStart() {
                                HProgressDialogUtils.showHorizontalProgressDialog(mContext, "下载进度", false);
                            }

                            @Override
                            public void onProgress(float progress, long total) {
                                HProgressDialogUtils.setProgress(Math.round(progress * 100));
                            }

                            @Override
                            public boolean onCompleted(File file) {
                                HProgressDialogUtils.cancel();
                                return true;
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                HProgressDialogUtils.cancel();
                            }
                        });
                    }
                })
                .setNegativeButton("暂不升级", null)
                .setCancelable(false)
                .create()
                .show();
    }

}*/
