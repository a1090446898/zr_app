package com.gsoft.inventory.service.import_net_assets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.utils.*;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author 10904
 */
public class SyncServerAssetsTask implements Callable<String> {
    private final String bmbh;
    private final boolean isClearAssets;
    private final Gson gson = new Gson();
    private String ztbh;

    public SyncServerAssetsTask(String ztbh,  String bmbh, boolean isClearAssets) {
        this.bmbh = bmbh;
        this.ztbh = ztbh;
        this.isClearAssets = isClearAssets;
    }

    @Override
    public String call() {
        String s = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVERHOST_SYNC_ASSETS(), ztbh, bmbh));
        if (StringUtils.isNullOrEmpty(s) || s.startsWith("ERROR")) {
            return "同步出错，".concat(s);
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {}.getType());
            if (results == null || results.isEmpty()) {
                return "同步出错，".concat(s);
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
            stringBuilder.append("同步失败，请联系技术人员\n");
        }
        return stringBuilder.toString();
    }
}