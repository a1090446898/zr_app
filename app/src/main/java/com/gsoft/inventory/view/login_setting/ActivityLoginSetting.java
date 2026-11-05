package com.gsoft.inventory.view.login_setting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.JCPrinter;
import com.gsoft.inventory.entities.ZCQCY;
import com.gsoft.inventory.ui.MyQMUIDialog;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 10904
 */
public class ActivityLoginSetting extends BaseCompatActivity {

    QMUITopBar topBar;

    EditText editServerIP;
    EditText editPrinter;

    EditText editDeviceNo;

    EditText editPrinterTitle;

    EditText editPrinterPosition;

    TextView textPrinterField;
    Button buttonSave;

    EditText editQCY;

    RadioGroup radioGroupOfflineLogin;
    RadioButton radioNo;
    RadioButton radioYes;
    TextView importAuthorizationCodeBtnId;


    @BindArray(R.array.printfieldsTitleCz)
    String[] printfieldsTitleCz;

    @BindArray(R.array.printfieldsCodeCz)
    String[] printfieldsCodeCz;

    @BindArray(R.array.printfieldsTitle)
    String[] printfieldsTitle;

    @BindArray(R.array.printfieldsCode)
    String[] printfieldsCode;

    @BindArray(R.array.print_title_select)
    String[] printTitleSelect;

    @BindArray(R.array.print_position_select)
    String[] printPositionSelect;

    private Map<String, String> filedsMap = new HashMap<>();

    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    private Context context;


    List<ZCQCY> zcqcyArray = null;
    String[] zcqcnames = null;
    String qcyName = "";
    String qcyCode = "";

    String offlineLogin = "离线";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_setting);
        context = this;
        this.initializeView();

        this.initData();
    }

    @Override
    public void initializeView() {
        ButterKnife.bind(this);

        topBar = findViewById(R.id.topbar);
        editServerIP = findViewById(R.id.editServerIP);
        editPrinter = findViewById(R.id.editPrinter);
        editDeviceNo = findViewById(R.id.editDeviceNo);
        editPrinterTitle = findViewById(R.id.editPrinterTitle);
        editPrinterPosition = findViewById(R.id.editPrinterPosition);
        textPrinterField = findViewById(R.id.textPrinterField);
        buttonSave = findViewById(R.id.buttonSave);
        editQCY = findViewById(R.id.editQCY);
        radioGroupOfflineLogin = findViewById(R.id.radioGroupOfflineLogin);
        radioNo = findViewById(R.id.radioNo);
        radioYes = findViewById(R.id.radioYes);
        importAuthorizationCodeBtnId = findViewById(R.id.importAuthorizationCodeBtnId);

        topBar.setTitle("设置");

    }


    public void initData() {
        // 初始化filedsMap数据
        for (int i = 0; i < printfieldsTitle.length; i++) {
            filedsMap.put(printfieldsCode[i], printfieldsTitle[i]);
        }

        String serverHost = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.SP_KEY_SERVERHOST, SysDefine.SERVERHOST);
        String printer = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_EFFECT, "网页二维码-二字段");
        String printerTitle = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, "单位名称");
        String printerPosition = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, "居中");
        String printFieldsCode = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "");
        qcyName = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZCQCYNAME, "");
        qcyCode = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_ZCQCYCODE, "");
        offlineLogin = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_OFFLINE_LOGIN, "在线");

        // 设备号：首次启动自动生成并持久化，此处可直接修改（两位，首位大写字母）
        String deviceNo = application.getDeviceNo();
        if (StringUtils.isNullOrEmpty(deviceNo)) {
            deviceNo = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_DEVICENO, "");
        }
        editDeviceNo.setText(deviceNo);

        // 赋值
        editServerIP.setText(serverHost);
        editPrinter.setText(printer);
        editPrinterTitle.setText(printerTitle);
        editPrinterPosition.setText(printerPosition);
        editQCY.setText(qcyName);

        String fields = "BARCODEID,ASSETSNAME";
        if (StringUtils.isNullOrEmpty(printFieldsCode)) {
            setTextPrinterField("资产名称");
            SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, fields);
        } else {
            // 将printFieldsCode通过,分割成数组
            String text = "";
            String[] printFieldsCodeArray = printFieldsCode.split(",");
            for (int i = 0; i < printFieldsCodeArray.length; i++) {
                String s = filedsMap.get(printFieldsCodeArray[i]);
                if (s != null) {
                    text += s;
                }
                if (!StringUtils.isNullOrEmpty(text) && i < printFieldsCodeArray.length - 1) {
                    text += ",";
                }
            }
            setTextPrinterField(text);
        }

        if("在线".equals(offlineLogin)){
            radioYes.setChecked(true);
            radioNo.setChecked(false);
        } else {
            radioYes.setChecked(false);
            radioNo.setChecked(true);
        }


        // 设置监听事件
        editPrinter.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, PrintLabelSelectActivity.class);
            intent.putExtra(PrintLabelSelectActivity.EXTRA_CURRENT_LABEL, editPrinter.getText().toString());
            startActivityForResult(intent, SysDefine.REQESTCODE_SELECT_PRINT_LABEL);
        });

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

        textPrinterField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMultiChoiceDialog();
            }
        });


        radioGroupOfflineLogin.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioNo) {
                // 离线登录
                offlineLogin = "离线";
            } else {
                // 在线登录
                offlineLogin = "在线";
            }
        });


        // 初始化清查用户
        this.initCleanUser();


        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 保存设置
                String serverHost = editServerIP.getText().toString();
                String printer = editPrinter.getText().toString();
                String printerTitle = editPrinterTitle.getText().toString();
                String printerPosition = editPrinterPosition.getText().toString();
                String printFieldsCode = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "");

                // 先校验设备号格式，不通过时给出对应提示并阻止保存
                String deviceNo = editDeviceNo.getText().toString().trim();
                if (!checkDeviceNo(deviceNo)) {
                    return;
                }

                if (!serverHost.startsWith("http://")) {
                    serverHost = "http://".concat(serverHost);
                }
                if (!serverHost.endsWith("/")) {
                    serverHost = serverHost.concat("/");
                }

                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.SP_KEY_SERVERHOST, serverHost);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_EFFECT, printer);
                // 同步内存中的打印类型，保存后立即生效（避免必须重新登录才切换标签）
                application.setPrinterEffect(printer);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, printerTitle);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, printerPosition);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, printFieldsCode);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYNAME, qcyName);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYCODE, qcyCode);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_OFFLINE_LOGIN, offlineLogin);
                // 设备号持久化，并同步到内存供条码生成使用
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_DEVICENO, deviceNo);
                application.setDeviceNo(deviceNo);

                SysDefine.setServerHost(serverHost);
                showShortText("保存成功");
                // 返回上一页
                finish();
            }
        });

        importAuthorizationCodeBtnId.setOnClickListener((view)-> {
            this.searchImportAuthorCodeTask();
        });
    }

    /**
     * 校验设备号格式：必须两位，第一位为大写字母(A-Z)，第二位为数字(0-9)且不能为4
     * 校验不通过时直接提示具体原因
     *
     * @param deviceNo 用户输入的设备号（已去除首尾空格）
     * @return 是否通过校验
     */
    private boolean checkDeviceNo(String deviceNo) {
        if (StringUtils.isNullOrEmpty(deviceNo)) {
            showShortText("设备号不能为空");
            return false;
        }
        if (deviceNo.length() != 2) {
            showShortText("设备号必须为两位，如：A1");
            return false;
        }
        char first = deviceNo.charAt(0);
        if (first < 'A' || first > 'Z') {
            showShortText("设备号第一位必须为大写字母（A-Z）");
            return false;
        }
        char second = deviceNo.charAt(1);
        if (second < '0' || second > '9') {
            showShortText("设备号第二位必须为数字（0-9）");
            return false;
        }
        if (second == '4') {
            showShortText("设备号第二位不能为4");
            return false;
        }
        return true;
    }


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

        // 获取已保存的字段代码
        String savedFieldsCode = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "");
        String[] savedFieldsCodeArray = savedFieldsCode.split(",");
        // 选中的List
        List<String> selectedFieldsCodeList = new ArrayList<>(Arrays.asList(savedFieldsCodeArray));
        List<String> selectedFieldsTitleList = new ArrayList<>();
        for (String code : savedFieldsCodeArray) {
            String title = filedsMap.get(code);
            if(!StringUtils.isNullOrEmpty(title)){
                selectedFieldsTitleList.add(title);
            }
        }


        String[] finalFieldsCode = fieldsCode;
        String[] finalFieldsTitle = fields;

        // 找出对应的索引
        ArrayList<Integer> checkedIndexes = new ArrayList<>();
        for (String code : savedFieldsCodeArray) {
            for (int i = 0; i < fieldsCode.length; i++) {
                if (code.equals(fieldsCode[i])) {
                    checkedIndexes.add(i);
                    break;
                }
            }
        }
        // 将 ArrayList 转换为 int 数组
        int[] checkedItems = new int[checkedIndexes.size()];
        for (int i = 0; i < checkedIndexes.size(); i++) {
            checkedItems[i] = checkedIndexes.get(i);
        }
        final QMUIDialog.MultiCheckableDialogBuilder builder = new QMUIDialog.MultiCheckableDialogBuilder(context)
                .addItems(fields, (dialog, which) -> {
                    // 获取当前选中状态（优化判断方式）
                    boolean isChecked = checkedIndexes.contains(which);
                    String codeToCheck = finalFieldsCode[which];
                    String titleToCheck = finalFieldsTitle[which];

                    if (isChecked) {
                        // 之前已经选中状态，移除字段
                        // 先检查是否存在再移除，避免NoSuchElementException
                        if(selectedFieldsCodeList.contains(codeToCheck) && selectedFieldsTitleList.contains(titleToCheck)){
                            selectedFieldsCodeList.remove(codeToCheck);
                            selectedFieldsTitleList.remove(titleToCheck);
                            checkedIndexes.remove(Integer.valueOf(which));
                        }
                    } else {
                        // 未选中状态，添加字段
                        if(!StringUtils.isNullOrEmpty(codeToCheck) && !StringUtils.isNullOrEmpty(titleToCheck)){
                            selectedFieldsCodeList.add(codeToCheck);
                            selectedFieldsTitleList.add(titleToCheck);
                            checkedIndexes.add(which);
                        }
                    }
                })
                .setCheckedItems(checkedItems);
        builder.addAction("取消", (dialog, index) -> dialog.dismiss());
        builder.addAction("提交", (dialog, index) -> {
            if (builder.getCheckedItemIndexes().length == 0 || selectedFieldsCodeList.isEmpty()) {
                Toast.makeText(context, "必须选择要打印的字段", Toast.LENGTH_SHORT).show();
                return;
            }
            // 将选中的List转化为字符串，用逗号拼接，去除最后一个逗号
            String codeString = String.join(",", selectedFieldsCodeList);
            String titleString = String.join(",", selectedFieldsTitleList);

            setTextPrinterField(titleString);
            SharedPreferencesUtils.deleShare(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE);
            SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, codeString);
            dialog.dismiss();
        });

        builder.create(mCurrentDialogStyle).show();
    }


    /**
     * 编辑公安警告标签警示内容（最多3行，每行建议不超过20个汉字）
     */
    private void showPoliceWarningEditDialog() {
        String savedContent = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_POLICE_WARNING_CONTENT, "");
        if (StringUtils.isNullOrEmpty(savedContent)) {
            savedContent = SysConfig.POLICE_WARNING_DEFAULT;
        }
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("公安警告标签内容")
                .setPlaceholder("每行一条警示语，最多3行，每行不超过20个汉字")
                .setDefaultText(savedContent)
                .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String content = builder.getEditText().getText().toString().trim();
                    if (StringUtils.isNullOrEmpty(content)) {
                        showShortText("内容不能为空，未保存");
                        return;
                    }
                    if (content.split("\n").length > 3) {
                        showShortText("最多3行警示语，未保存");
                        return;
                    }
                    SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_POLICE_WARNING_CONTENT, content);
                    showShortText("公安警告标签内容已保存");
                })
                .create(mCurrentDialogStyle).show();
    }


    /**
     * 清空清查员
     */
    private void cleanQcy() {
        this.editQCY.setText("无");
        this.qcyName = "";
        this.qcyCode = "";
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYNAME, "");
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_ZCQCYCODE, "");
    }

    private void initCleanUser() {

        // 01为智软集团的人，02为其它单位的人
        String userType = "01";
        zcqcyArray = ZCQCY.find(ZCQCY.class, "USER_TYPE = ?", userType);
        zcqcnames = new String[zcqcyArray.size() + 1];
        zcqcnames[0] = "无";
        for (int i = 1; i < zcqcyArray.size(); i++) {
            zcqcnames[i] = zcqcyArray.get(i).NAME;
        }
        editQCY.setOnClickListener(view -> {
            new QMUIDialog.CheckableDialogBuilder(mContext)
                    .addItems(zcqcnames, (dialog, i) -> {
                        String zcqcname = zcqcnames[i];
                        if("无".equals(zcqcname)){
                            cleanQcy();
                            dialog.dismiss();
                            return;
                        }
                        editQCY.setText(zcqcname);
                        qcyName = zcqcname;
                        qcyCode = zcqcyArray.get(i).CODE;
                        dialog.dismiss();
                    }).create(mCurrentDialogStyle).show();
        });
    }


    private void setTextPrinterField(String text){
        textPrinterField.setText(text + "【点击设置】");
    }


    public void searchImportAuthorCodeTask() {
        showProgressDialog("设备码导入中……", "");
        JCPrinter.deleteAll(JCPrinter.class);

        AtomicInteger isFindCodeTxt = new AtomicInteger(0);
        CommonUtils.getExecutor().execute(() -> {
            List<FileBean> fileList = DataTransmitHelper.searchImportFiles(mContext);
            // 处理文件
            for (FileBean fileBean : fileList) {
                String name = fileBean.getName();
                if ("code.txt".equals(name)) {
                    String codeStr = readAuthorizationCode(fileBean);
                    if (!StringUtils.isNullOrEmpty(codeStr)) {
                        isFindCodeTxt.set(1);
                        // 以换行符将codeStr拆分为数组
                        String[] results = codeStr.split("\n");
                        // 数据库操作
                        JCPrinter.deleteAll(JCPrinter.class);
                        for (String dataLine : results) {
                            if (!dataLine.contains("ERROR")) {
                                DataTransmitHelper.insertPrinters(dataLine);
                            }
                        }
                    }
                    showShortText("设备授权码导入成功。");
                }
            }


            CommonUtils.getHandler().post(() -> {
                if (isFindCodeTxt.get() == 0) {
                    // 创建确认对话框
                    new AlertDialog.Builder(this)
                            .setTitle("导入失败")
                            .setMessage("未查询到Downloads/zr/code.txt文件，请检查文件是否存在或者为空")
                            .setPositiveButton("确定", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .setCancelable(false)
                            .show();
                    showShortText("设备授权码导入失败。");
                }
                hideProgressDialog();
            });
        });
    }


    // 读取文件内容
    private String readAuthorizationCode(FileBean fileBean) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(fileBean.getPath())) {
            byte[] content = new byte[fis.available()];
            int readStatus = fis.read(content);
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 打印标签选择页返回：更新显示，并保留原弹框选择时的联动处理
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SysDefine.REQESTCODE_SELECT_PRINT_LABEL || resultCode != RESULT_OK || data == null) {
            return;
        }
        String effect = data.getStringExtra(PrintLabelSelectActivity.EXTRA_SELECTED_LABEL);
        if (StringUtils.isNullOrEmpty(effect)) {
            return;
        }
        editPrinter.setText(effect);
        if ("网页二维码-二字段".equals(effect)) {
            String fields = "BARCODEID,ASSETSNAME";
            SharedPreferencesUtils.deleShare(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE);
            SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, fields);
            setTextPrinterField("资产名称");
        } else if ("公安警告标签".equals(effect)) {
            // 选择公安警告标签后，弹出警示内容编辑框
            showPoliceWarningEditDialog();
        }
        // 同步内存中的打印类型，选择后立即生效
        application.setPrinterEffect(effect);
    }
}
