package com.gsoft.inventory;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.adapter.WaitingSignaturePersonListAdapter;
import com.gsoft.inventory.entities.SignaturePerson;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

/**
 * @author 10904
 */
public class SignatureActivity extends BaseCompatActivity {

    WaitingSignaturePersonListAdapter signaturePersonListAdapter;
    List<SignaturePerson> signaturePeoples;
    ListView listView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        initializeView();
    }

    @Override
    public void initializeView() {
        QMUITopBar topBar = findViewById(R.id.topbar);
        topBar.setTitle("资产盘点确认");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightTextButton("签名网址", R.id.topbar_right_change_button).setOnClickListener(v -> {
            String url = "http://www.zrast.com:8034/pages/phone_check/index";
            // 点击复制到剪切板
            ClipboardUtils.copy(mContext, url);
        });


        listView = findViewById(R.id.listPersons);

        signaturePeoples = new ArrayList<>();


        signaturePersonListAdapter = new WaitingSignaturePersonListAdapter(signaturePeoples, mContext);
        signaturePersonListAdapter.setSendMessageCallBack(new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                runOnUiThread(() -> showShortText("发送成功！"));
            }

            @Override
            public void onResponseFailure(String errMessage) {
                runOnUiThread(() -> showShortText("发送失败，请稍后重试"));
            }
        });
        listView.setAdapter(signaturePersonListAdapter);


        this.searchAssetPersonTask();
    }

    public void refreshPersons(List<SignaturePerson> peoples) {
        signaturePeoples.addAll(peoples);
        Collections.sort(signaturePeoples, new Comparator<SignaturePerson>() {
            @Override
            public int compare(SignaturePerson o1, SignaturePerson o2) {
                return o1.getDepart().compareTo(o2.getDepart());
            }
        });

        // signaturePeoples.sort();
        signaturePersonListAdapter.notifyDataSetChanged();
    }

    public SignaturePerson findPerson(String name) {
        if (signaturePeoples == null || signaturePeoples.isEmpty()) return null;
        for (SignaturePerson person : signaturePeoples) {
            if (person.getName().equals(name)) return person;
        }
        return null;
    }


    /**
     * 调用第三方APP来打开文件Downloads/zr/目录下的文件
     */
    private void lookExportTxtFile() {
        // 获取最近导出的文件列表
        File zrDir = DownloadZrFileManager.getZrDirectory(this);
        if (!zrDir.exists()) {
            showShortText("未找到导出目录");
            return;
        }

        // 查找所有export_sign开头的txt文件
        File[] txtFiles = zrDir.listFiles((dir, name) ->
                name.startsWith("export_sign_") && name.endsWith(".txt"));

        if (txtFiles == null || txtFiles.length == 0) {
            showShortText("未找到导出的文件");
            return;
        }

        // 按修改时间排序，获取最新的文件
        Arrays.sort(txtFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        File latestFile = txtFiles[0];

        // 检查文件是否存在
        if (!latestFile.exists()) {
            showShortText("文件不存在");
            return;
        }

        try {
            // 获取文件URI
            Uri fileUri = FileProviderUtil.getFileUri(this, latestFile);

            // 创建打开文本文件的Intent
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "text/plain"); // 设置MIME类型为纯文本
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 授予读取权限

            // 检查是否有应用可以处理此类型的文件
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                showShortText("未找到可打开此文件的应用");
            }
        } catch (Exception e) {
            Log.e("lookExportTxtFile", "打开文件失败", e);
            showShortText("打开文件失败: " + e.getMessage());
        }
    }



    public void exportDataTask() {
        showProgressDialog("正在导出...", "");

        // 检查权限
        if (!DownloadZrFileManager.hasWritePermission(this)) {
            showShortText("请先授予存储权限");
            hideProgressDialog();
            DownloadZrFileManager.requestWritePermission(this, 1001);
            return;
        }
        AtomicBoolean isSuccess = new AtomicBoolean(true);
        CommonUtils.getExecutor().execute(() -> {
            String fileName = "";
            try {
                // 使用工具类获取zr目录
                File zrDir = DownloadZrFileManager.getZrDirectory(this);

                if (!zrDir.exists()) {
                    if (!zrDir.mkdirs()) {
                        Log.e("Export", "创建zr目录失败");
                        isSuccess.set(false);
                        return;
                    }
                }

                if (!zrDir.exists()) {
                    if (!zrDir.mkdirs()) {
                        Log.e("Export", "创建zr目录失败");
                        isSuccess.set(false);
                        return;
                    }
                }

                // 生成文件名（年月日）
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                fileName = "export_sign_" + sdf.format(new Date()) + ".txt";
                File exportFile = new File(zrDir, fileName);

                // 写入文件
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile))) {
//                    writer.write("部门名称，人员姓名，状态，电话，通知链接\n");

                    for (SignaturePerson person : signaturePeoples) {
                        if(!person.getSelectStatus()){
                            continue;
                        }
                        String url = String.format("固定资产确认通知：尊敬的 %s，请点击链接http://www.zrast.com/signature.html?phone=%s核实您名下的固定资产信息，并签字确认。", person.getName(), person.getTelephone());
                        String line = String.format("%s，%s，%s，%s，%s\n",
                                escapeCsvField(person.getDepart()),
                                escapeCsvField(person.getName()),
                                escapeCsvField(person.getStatus()),
                                escapeCsvField(person.getTelephone()),
                                url);
                        writer.write(line);
                    }
                }
            } catch (IOException e) {
                Log.e("Export", "导出签名文件失败", e);
                return;
            } finally {
                String finalFileName = fileName;
                CommonUtils.getHandler().post(() -> {
                    hideProgressDialog();
                    if (isSuccess.get()) {
                        // 弹出确认对话框说明导出成功，并提示导出路径在Downloads/zr下

                        // 创建确认对话框
                        new AlertDialog.Builder(this)
                                .setTitle("导出成功")
                                .setMessage("文件已成功导出至Downloads/zr/" + finalFileName)
                                .setPositiveButton("确定", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        showShortText("导出失败");
                    }
                });
            }
        });
    }

    // CSV转义方法
    private String escapeCsvField(String field) {
        if (field == null) return "";

        // 如果字段包含逗号、双引号或换行符，则用双引号包围并转义双引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private void searchAssetPersonTask(){
        showProgressDialog("", "正在检索");
        CommonUtils.getExecutor().execute(() -> {
            String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_GET_ASSETS_PEOPLES(), application.getLoginAccount().getZTBH()));
            Log.e("fetch users", responseString);
            if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                return;
            }

            List<SignaturePerson> personList = new ArrayList<>();
            try {
                List<String> results = gson.fromJson(responseString, new TypeToken<List<String>>() {
                }.getType());
                if (results == null || results.isEmpty()) {
                    return;
                }
                for (String dataLine : results) {
                    SignaturePerson person = DataTransmitHelper.transferSignaturePerson(dataLine);
                    if (person != null) personList.add(person);
                }
            } catch (Exception e) {
                // 将 e.printStackTrace() 替换为更可靠的日志记录
                Log.e("SignatureActivity", "解析签名人员数据时发生错误", e);
            }finally {
                CommonUtils.getHandler().post(() -> {
                    hideProgressDialog();
                    refreshPersons(personList);
                });
            }
        });
    }

}