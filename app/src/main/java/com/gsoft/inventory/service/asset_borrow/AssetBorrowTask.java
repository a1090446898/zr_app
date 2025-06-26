package com.gsoft.inventory.service.asset_borrow;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.AssetBorrow;
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
public class AssetBorrowTask implements Callable<List<AssetBorrow>> {

    private String userId;

    public AssetBorrowTask(String userId){
        this.userId = userId;
    }

    @Override
    public List<AssetBorrow> call() throws Exception {
        String urlParam = String.format(SysDefine.getJy(), userId);
        String result = OkHttpUtils.getInstance().get(urlParam);
        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            throw new RuntimeException("同步出错，".concat(result));
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("数据为空");
        }

        List<AssetBorrow> dataList = new ArrayList<>();
        for(String dataLine : results){
            if(dataLine.isEmpty() || "ERROR".equals(dataLine)){
                continue;
            }
            String[] rowStrArray = dataLine.split("\\|", -1);
            if(rowStrArray.length > 0){
                AssetBorrow item = new AssetBorrow();
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
