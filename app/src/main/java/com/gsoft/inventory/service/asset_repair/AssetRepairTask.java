package com.gsoft.inventory.service.asset_repair;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.AssetRepair;
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
public class AssetRepairTask implements Callable<List<AssetRepair>> {

    private String userId;

    public AssetRepairTask(String userId){
        this.userId = userId;
    }

    @Override
    public List<AssetRepair> call() throws Exception {
        String urlParam = String.format(SysDefine.getBx(), userId);
        String result = OkHttpUtils.getInstance().get(urlParam);
        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            throw new RuntimeException("同步出错，".concat(result));
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("数据为空");
        }

        List<AssetRepair> dataList = new ArrayList<>();
        for(String dataLine : results){
            if(dataLine.isEmpty() || "ERROR".equals(dataLine)){
                continue;
            }
            String[] rowStrArray = dataLine.split("\\|", -1);
            if(rowStrArray.length > 0){
                AssetRepair item = new AssetRepair();
                String DOCID =  rowStrArray[0].trim();
                String SENDUSER =  rowStrArray[1].trim();
                String SENDPHONE =  rowStrArray[2].trim();
                String SENDDATE =  rowStrArray[3].trim();
                String SENDREASON =  rowStrArray[4].trim();
                String DOUSER =  rowStrArray[5].trim();
                String RESULT =  rowStrArray[6].trim();
                String DODATE =  rowStrArray[7].trim();
                String FID =  rowStrArray[8].trim();

                item.setDOCID(DOCID);
                item.setSENDUSER(SENDUSER);
                item.setSENDPHONE(SENDPHONE);
                item.setSENDDATE(SENDDATE);
                item.setSENDREASON(SENDREASON);
                item.setDOUSER(DOUSER);
                item.setRESULT(RESULT);
                item.setDODATE(DODATE);
                item.setFID(FID);

                dataList.add(item);
            }
        }

        return dataList;
    }
}
