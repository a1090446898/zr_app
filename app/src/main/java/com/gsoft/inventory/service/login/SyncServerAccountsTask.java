package com.gsoft.inventory.service.login;

import android.util.Log;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;

import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

/**
 * @author 10904
 */
public class SyncServerAccountsTask implements Callable<String> {

    private String account;
    private String password;

    public SyncServerAccountsTask(String account, String pwd) {
        this.account = account.trim();
        String password = pwd.trim();
        this.password = SecurityUtils.md5(password).toUpperCase();
    }

    @Override
    public String call() throws Exception {
        String s = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_SYNC_LOGINACCOUNTS(),
                this.account, this.password));
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
            Log.e("LoginError", "SyncServerAccountsTask: ", e);
            stringBuilder.append("登录失败，请联系技术人员\n");
        }
        return stringBuilder.toString();
    }
}
