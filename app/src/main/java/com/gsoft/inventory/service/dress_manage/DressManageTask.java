package com.gsoft.inventory.service.dress_manage;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.DressManage;
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
public class DressManageTask implements Callable<List<DressManage>> {

    private String userId;

    public DressManageTask(String userId){
        this.userId = userId;
    }

    @Override
    public List<DressManage> call() throws Exception {
        String urlParam = String.format(SysDefine.getFs(), userId);
        String result = OkHttpUtils.getInstance().get(urlParam);
        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            throw new RuntimeException("同步出错，".concat(result));
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("数据为空");
        }

        List<DressManage> dataList = new ArrayList<>();
        for(String dataLine : results){
            if(dataLine.isEmpty() || "ERROR".equals(dataLine)){
                continue;
            }
            String[] rowStrArray = dataLine.split("\\|", -1);
            if(rowStrArray.length > 0){
                DressManage item = new DressManage();
                String docId =  rowStrArray[0].trim();
                String receiveDepart =  rowStrArray[1].trim();
                String receiveUser =  rowStrArray[2].trim();
                String receiveTime =  rowStrArray[3].trim();
                String receivePlace =  rowStrArray[4].trim();
                String giveUser =  rowStrArray[5].trim();
                String giveTime =  rowStrArray[5].trim();

                item.setDocId(docId);
                item.setReceiveDepart(receiveDepart);
                item.setReceiveUser(receiveUser);
                item.setReceiveTime(receiveTime);
                item.setReceivePlace(receivePlace);
                item.setGiveUser(giveUser);
                item.setGiveTime(giveTime);
                dataList.add(item);
            }
        }

        return dataList;
    }
}
