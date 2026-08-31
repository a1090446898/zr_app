package com.gsoft.inventory.service.login;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.PrintLogInfo;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

public class SyncPrintLogTask implements Callable<String> {

    private Context context;

    public SyncPrintLogTask(Context context) {
        this.context = context;
    }

    @Override
    public String call() throws Exception {
        String result = "";
        String jsonStr = SharedPreferencesUtils.getString(context, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO, "");
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
                SharedPreferencesUtils.deleShare(context, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO);
            }
        }
        return result;
    }
}
