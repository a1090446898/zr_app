package com.gsoft.inventory.const_data;

import com.google.gson.Gson;

public interface Log_file_name {

    public static final Gson gson = new Gson();

    /**
     * 上传图片日志文件名
     */
    String UPLOAD_PHOTO_LOG_FILE_NAME = "upload_photo_log.txt";

    /**
     * 同步资产错误日志文件名
     */
    String LOG_ASSETS_SYNC_ERROR_FILE_NAME = "log_assets_sync_error.txt";
    String LOG_LOAD_CATEGORY_TASK_FILE_NAME = "log_load_category_task.log";


}
