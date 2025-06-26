package com.gsoft.inventory.service.dress_manage;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.DressManageDetailItem;
import com.gsoft.inventory.entities.DressManageDetailItem;
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
public class DressManageDetailTask implements Callable<List<DressManageDetailItem>> {

    private String docId;

    public DressManageDetailTask(String docId){
        this.docId = docId;
    }

    @Override
    public List<DressManageDetailItem> call() throws Exception {
        String urlParam = String.format(SysDefine.getFsDetailList(), docId);
        String result = OkHttpUtils.getInstance().get(urlParam);
        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            throw new RuntimeException("同步出错，".concat(result));
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("数据为空");
        }

        List<DressManageDetailItem> dataList = new ArrayList<>();
        for(String dataLine : results){
            if(dataLine.isEmpty() || "ERROR".equals(dataLine)){
                continue;
            }
            String[] rowStrArray = dataLine.split("\\|", -1);
            if(rowStrArray.length > 0){
                DressManageDetailItem item = new DressManageDetailItem();
                String docId =  rowStrArray[0].trim();
                String dressName =  rowStrArray[1].trim();
                String receiveNum =  rowStrArray[2].trim();
                String inventoryQuantity =  rowStrArray[3].trim();
                String receiveUser =  rowStrArray[4].trim();
                String receiveDepart =  rowStrArray[5].trim();

                item.setDocId(docId);
                item.setDressName(dressName);
                item.setReceiveNum(receiveNum);
                item.setInventoryQuantity(inventoryQuantity);
                item.setReceiveUser(receiveUser);
                item.setReceiveDepart(receiveDepart);

                dataList.add(item);
            }
        }

        return dataList;
    }
}
