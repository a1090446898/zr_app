package com.gsoft.inventory.service.login;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.BadgeNotificationPojo;
import com.gsoft.inventory.entities.PrintLogInfo;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

/**
 * @author 10904
 */
public class SyncGetBadgeAndNotification implements Callable<List<BadgeNotificationPojo>> {

    private Context context;

    public SyncGetBadgeAndNotification(Context context) {
        this.context = context;
    }

    @Override
    public List<BadgeNotificationPojo> call() throws Exception {
        List<BadgeNotificationPojo> resultList = new ArrayList<>();
        String userId = SharedPreferencesUtils.getString(context, SharedPreferencesUtils.KEY_YHBH, "");
        if(StringUtils.isNullOrEmpty(userId)){
            return resultList;
        }
        String url = String.format(SysDefine.getBadgeAndNotificationApi(), userId);
        String result = OkHttpUtils.getInstance().get(url);
        List<String> jsonResult = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        for (String item : jsonResult) {
            if(StringUtils.isNullOrEmpty(item) || "ERROR".equals(item)){
                continue;
            }
            // 进行解析
            String[] splitArr = item.split("\\|");

            BadgeNotificationPojo badgeNotificationPojo = new BadgeNotificationPojo();
            if(splitArr.length > 1){
                badgeNotificationPojo.setBadgeCount(splitArr[0]);
            }
            if(splitArr.length > 2){
                badgeNotificationPojo.setFlowTag(splitArr[1]);
            }
            if(splitArr.length > 3){
                badgeNotificationPojo.setNoticeContent(splitArr[2]);
            }
            resultList.add(badgeNotificationPojo);
        }
        return resultList;
    }
}
