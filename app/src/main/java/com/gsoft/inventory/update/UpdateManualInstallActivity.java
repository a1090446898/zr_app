package com.gsoft.inventory.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.gsoft.inventory.utils.StringUtils;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.xuexiang.xupdate.utils.ApkInstallUtils;

import java.io.File;

/**
 * 自动安装失败后的手动安装引导页
 */
public class UpdateManualInstallActivity extends Activity {

    public static final String EXTRA_APK_PATH = "extra_apk_path";

    private File apkFile;
    private File apkParentDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String apkPath = getIntent().getStringExtra(EXTRA_APK_PATH);
        if (StringUtils.isNullOrEmpty(apkPath)) {
            finish();
            return;
        }
        apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            Toast.makeText(this, "未找到安装包文件", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        apkParentDir = apkFile.getParentFile();
        showGuideDialog();
    }

    private void showGuideDialog() {
        String message = "新版本安装包已下载完成：\n" + apkFile.getAbsolutePath()
                + "\n\n自动安装未成功，可点击下方按钮手动安装。";
        QMUIDialog dialog = new QMUIDialog.MessageDialogBuilder(this)
                .setTitle("系统提示")
                .setMessage(message)
                .addAction("取消", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                    }
                })
                .addAction("打开文件夹", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                        openApkFolder();
                    }
                })
                .addAction(0, "立即安装", QMUIDialogAction.ACTION_PROP_NEGATIVE, new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                        installApkManually();
                    }
                })
                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog);
        dialog.setOnDismissListener(dialogInterface -> finish());
        dialog.show();
    }

    /**
     * 重新调起系统安装器覆盖安装（与手动安装同一流程）
     */
    private void installApkManually() {
        try {
            Intent installIntent = ApkInstallUtils.getInstallAppIntent(apkFile);
            if (installIntent != null) {
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(installIntent);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "无法调起安装程序，请前往文件夹手动安装", Toast.LENGTH_LONG).show();
    }

    /**
     * 打开安装包所在文件夹
     */
    private void openApkFolder() {
        if (apkParentDir == null) {
            Toast.makeText(this, "安装包路径：" + apkFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apkParentDir), "resource/folder");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "未找到文件管理器，请手动前往：" + apkParentDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}