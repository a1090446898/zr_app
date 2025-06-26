package com.gsoft.inventory.service.main;

import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author 10904
 */
public class SyncDisposeAssetsTask implements Callable<String> {

    public SyncDisposeAssetsTask() {
    }

    @Override
    public String call() throws Exception {
        List<DisposeAsset> disposeAdding = DisposeAsset.find(DisposeAsset.class, "STATUS=?", "1");
        List<DisposeAsset> disposeDeleting = DisposeAsset.find(DisposeAsset.class, "STATUS=?", "2");
        if (disposeAdding.isEmpty() && disposeDeleting.isEmpty()) return "没有需要同步的资产处置记录";
        int addCount = 0;
        int deleteCount = 0;
        for (DisposeAsset asset : disposeAdding) {
            String result = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_SCAN_UPLOAD(), asset.getDispose(), asset.getCode()));
            if (result.contains("\"result\":\"OK\"") || result.contains("OK")) {
                asset.setStatus(0);
                asset.save();
                addCount++;
            }
        }
        for (DisposeAsset asset : disposeDeleting) {
            String result = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_SCAN_UPLOAD(), asset.getDispose(), asset.getCode()));
            if (result.contains("\"result\":\"OK\"") || result.contains("OK")) {
                asset.setStatus(0);
                asset.save();
                deleteCount++;
            }
        }
        return String.format("同步新增记录：%1$s条，删除记录：%2$s", addCount, deleteCount);
    }
}
