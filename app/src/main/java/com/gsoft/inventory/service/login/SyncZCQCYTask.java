package com.gsoft.inventory.service.login;

import android.content.Context;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.ZCQCY;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;

import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

public class SyncZCQCYTask implements Callable<String> {

    private Context context;

    public SyncZCQCYTask(Context context) {
        this.context = context;
    }

    @Override
    public String call() throws Exception {
        // 账套编号
        String ztbh = "";

        // 用户名
        String yhbh = SharedPreferencesUtils.getString(context, SharedPreferencesUtils.KEY_YHBH, "");
        if (!StringUtils.isNullOrEmpty(yhbh)) {
            // 通过用户名取查找
            List<Accounts> accounts = Accounts.find(Accounts.class, "YHBH=?", yhbh);
            if (!accounts.isEmpty()) {
                ztbh = accounts.get(0).getZTBH();
                if (StringUtils.isNullOrEmpty(ztbh)) {
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
            List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {
            }.getType());
            if (results == null || results.isEmpty()) {
                return "同步资产清查人员失败！";
            }
            ZCQCY.deleteAll(ZCQCY.class);
            for (String dataLine : results) {
                long newId = DataTransmitHelper.insertZCQCY(dataLine);
                if (newId == -1) {
                    stringBuilder.append("同步资产清查人员出现异常，");
                }
            }
        } catch (Exception e) {
            stringBuilder.append("同步资产清查人员失败，请联系技术人员\n");
        }
        return stringBuilder.toString();
    }
}
