package com.gsoft.inventory.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * @author 10904
 */
public class LogToFile {


    /**
     *
     * @param mContext
     * @param logFileName
     * @param logContent
     */
    /**
     * 将日志内容写入指定文件
     * @param mContext 上下文
     * @param logFileName 日志文件名
     * @param logContent 日志内容
     */
    public static void toLogFile(Context mContext, String logFileName, String logContent) {
        // 获取存储路径
        String path = mContext.getExternalFilesDir(null) + "/errorLogs";
        File pathFile = new File(path);

        // 创建目录
        if (!pathFile.exists()) {
            boolean created = pathFile.mkdirs();
            if (!created) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(mContext, "创建errorLogs目录失败", Toast.LENGTH_SHORT).show());
                return;
            }
        }

        // 文件路径拼接
        String subDirPath = path + File.separator + logFileName;
        File file = new File(subDirPath);

        // 写入文件操作，true表示追加模式
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(String.format(Locale.getDefault(), "%s\n", logContent));
            writer.flush();
        } catch (IOException e) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(mContext, "写入日志文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
