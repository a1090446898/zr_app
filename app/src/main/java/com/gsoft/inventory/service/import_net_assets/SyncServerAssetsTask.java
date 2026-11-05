package com.gsoft.inventory.service.import_net_assets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Assets;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author 10904
 */
public class SyncServerAssetsTask implements Callable<String> {

    /**
     * 同步结果标记：服务端返回空数据（[]）
     */
    public static final String RESULT_EMPTY_DATA = "同步数据为空";

    private final String deptIds;
    private final String userId;
    private final boolean isClearAssets;
    private final Gson gson = new Gson();
    private String ztbh;

    public SyncServerAssetsTask(String ztbh, String deptIds, String userId, boolean isClearAssets) {
        this.ztbh = ztbh;
        this.deptIds = deptIds;
        this.userId = userId;
        this.isClearAssets = isClearAssets;
    }

    @Override
    public String call() {
        String formatUrl = String.format(SysDefine.SERVERHOST_SYNC_ASSETS_WITH_USER(), ztbh, deptIds, userId);
        String s = OkHttpUtils.getInstance().get(formatUrl);
        if (StringUtils.isNullOrEmpty(s) || s.startsWith("ERROR")) {
            return "同步出错，".concat(s);
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {}.getType());
            if (results == null) {
                return "同步出错，".concat(s);
            }
            if (results.isEmpty()) {
                return RESULT_EMPTY_DATA;
            }
            if (isClearAssets) {
                Assets.deleteAll(Assets.class);
            }
            for (String dataLine : results) {
                long newID = DataTransmitHelper.insertAssets(dataLine);
                if (newID == -1) {
                    String[] rowStrArray = dataLine.split("\\|", -1);
                    String bcode = rowStrArray[0].trim();
                    stringBuilder.append("条码编号：".concat(bcode).concat("导入失败；"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stringBuilder.append("同步失败，请联系技术人员\n");
        }
        return stringBuilder.toString();
    }
}