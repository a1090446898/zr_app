package com.gsoft.inventory.service.asset_return;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.AssetBorrowDetailItem;
import com.gsoft.inventory.entities.AssetReceive;
import com.gsoft.inventory.entities.AssetReturn;
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
public class AssetReturnTask implements Callable<List<AssetReturn>> {

    private String userId;

    public AssetReturnTask(String userId){
        this.userId = userId;
    }

    @Override
    public List<AssetReturn> call() throws Exception {
        String urlParam = String.format(SysDefine.getGh(), userId);
        String result = OkHttpUtils.getInstance().get(urlParam);
        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            throw new RuntimeException("同步出错，".concat(result));
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("数据为空");
        }

        List<AssetReturn> dataList = new ArrayList<>();
        for(String dataLine : results){
            if(dataLine.isEmpty() || "ERROR".equals(dataLine)){
                continue;
            }
            String[] rowStrArray = dataLine.split("\\|", -1);
            if(rowStrArray.length > 0){
                AssetReturn item = new AssetReturn();
                String assetId =  rowStrArray[0].trim();
                String assetsName =  rowStrArray[1].trim();
                String barcodeId =  rowStrArray[2].trim();
                String assetsStandard =  rowStrArray[3].trim();
                String assetsUser =  rowStrArray[4].trim();
                String assetsLayAdd =  rowStrArray[5].trim();
                String assetsCurrPrice =  rowStrArray[5].trim();

                item.setAssetId(assetId);
                item.setAssetsName(assetsName);
                item.setBarcodeId(barcodeId);
                item.setAssetsStandard(assetsStandard);
                item.setAssetsUser(assetsUser);
                item.setAssetsLayAdd(assetsLayAdd);
                item.setAssetsCurrPrice(assetsCurrPrice);
                dataList.add(item);
            }
        }

        return dataList;
    }
}
