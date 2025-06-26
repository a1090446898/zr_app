package com.gsoft.inventory.service.main;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.utils.*;

import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

/**
 * @author 10904
 */
public class SyncAssetsCategoryTask implements Callable<String> {

    private String ztbh;

    public SyncAssetsCategoryTask(String ztbh)
    {
        this.ztbh = ztbh;
    }

    @Override
    public String call() throws Exception {
        String formatStr = String.format(SysDefine.SERVERHOST_SYNC_ASSETCATEGORY_NEW(), ztbh);
        String result = OkHttpUtils.getInstance().get(formatStr);

        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            return "同步出错，".concat(result);
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {
            }.getType());
            if (results == null || results.isEmpty()) {
                return "同步出错，".concat(result);
            }
            AssetsCategory.deleteAll(AssetsCategory.class);
            int lineIndex = 0;
            for (String dataLine : results) {
                long newID = DataTransmitHelper.insertAssetsCategory(dataLine);
                if (newID == -1) {
                    stringBuilder.append("第".concat(String.valueOf(lineIndex)).concat("行导入失败；"));
                }
                lineIndex++;
            }
        } catch (Exception e) {
            stringBuilder.append("同步失败，请联系技术人员\n");
        }
        return stringBuilder.toString();
    }
}
