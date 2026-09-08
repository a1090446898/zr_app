package com.gsoft.inventory.update;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.xuexiang.xupdate.entity.DownloadEntity;
import com.xuexiang.xupdate.listener.impl.DefaultInstallListener;

import java.io.File;

/**
 * APK安装监听：自动安装失败时，引导用户手动覆盖安装
 */
public class GuideInstallListener extends DefaultInstallListener {

    @Override
    public boolean onInstallApk(@NonNull Context context, @NonNull File apkFile, @NonNull DownloadEntity downloadEntity) {
        boolean installSuccess = super.onInstallApk(context, apkFile, downloadEntity);
        if (!installSuccess) {
            guideUserToManualInstall(context, apkFile);
        }
        // 失败场景已自行引导处理，返回true避免重复走默认失败提示
        return true;
    }

    private void guideUserToManualInstall(Context context, File apkFile) {
        try {
            Intent intent = new Intent(context, UpdateManualInstallActivity.class);
            intent.putExtra(UpdateManualInstallActivity.EXTRA_APK_PATH, apkFile.getAbsolutePath());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}