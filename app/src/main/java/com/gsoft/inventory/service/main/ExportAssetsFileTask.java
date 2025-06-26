package com.gsoft.inventory.service.main;

import android.content.Context;
import android.text.TextUtils;
import com.gsoft.inventory.utils.DataTransmitHelper;
import com.gsoft.inventory.utils.FileSearchHelper;
import com.gsoft.inventory.utils.SysConfig;

import java.io.File;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.utils.SysDefine.PROCESS_RESULT_SUCCESS;

/**
 * @author 10904
 * 导出资产到文件操作
 */
public class ExportAssetsFileTask implements Callable<String> {

    private Context context;

    public ExportAssetsFileTask() {

    }

    public ExportAssetsFileTask(Context context) {
        this.context = context;
    }

    @Override
    public String call() {
        try{
            String fileDirPath =  context.getExternalFilesDir(null) + "/export";
            File dirExportPath = new File(fileDirPath);
            if (!dirExportPath.exists()) {
                dirExportPath.mkdirs();
            }
            String filePath = SysConfig.getExportFilePath(fileDirPath);
            // 具体导出操作
            String resultMessage = DataTransmitHelper.exportAssets(filePath);
            if (new File(filePath).exists() && TextUtils.isEmpty(resultMessage)) {
                FileSearchHelper.notifySystemToScan(filePath);
                return PROCESS_RESULT_SUCCESS;
            }
            return resultMessage;
        } catch (Exception e) {
            return "导出过程中发生异常: " + e.getMessage();
        }
    }
}
