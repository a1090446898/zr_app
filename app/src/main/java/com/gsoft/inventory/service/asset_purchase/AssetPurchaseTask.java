package com.gsoft.inventory.service.asset_purchase;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.AssetPurchase;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

/**
 * @author 10904
 */
public class AssetPurchaseTask implements Callable<List<AssetPurchase>> {

    private String userId;

    public AssetPurchaseTask(String userId){
        this.userId = userId;
    }

    @Override
    public List<AssetPurchase> call() throws Exception {
        String urlParam = String.format(SysDefine.getCg(), userId);
        String result = OkHttpUtils.getInstance().get(urlParam);
        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            throw new RuntimeException("同步出错，".concat(result));
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("数据为空");
        }

        List<AssetPurchase> dataList = new ArrayList<>();
        for(String dataLine : results){
            if(dataLine.isEmpty() || "ERROR".equals(dataLine)){
                continue;
            }
            String[] rowStrArray = dataLine.split("\\|", -1);
            if(rowStrArray.length > 0){
                AssetPurchase item = new AssetPurchase();
                String DOCID =  rowStrArray[0].trim();
                String NAME =  rowStrArray[1].trim();
                String APPLICANTDATE =  rowStrArray[2].trim();
                String UNITNAME =  rowStrArray[3].trim();
                String APPLY_EXPLAIN =  rowStrArray[4].trim();
                String STATE =  rowStrArray[5].trim();

                item.setDOCID(DOCID);
                item.setNAME(NAME);
                item.setAPPLICANTDATE(APPLICANTDATE);
                item.setUNITNAME(UNITNAME);
                item.setAPPLY_EXPLAIN(APPLY_EXPLAIN);
                item.setSTATE(STATE);
                dataList.add(item);
            }
        }

        return dataList;
    }
}
