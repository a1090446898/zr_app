package com.gsoft.inventory.service.photo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 查询资产在服务器上已上传的照片地址
 * <p>
 * 由 AsyncManager 统一调度，替代原 PhotoActivity 内部的 AsyncTask，
 * 返回空列表表示暂无已上传照片。
 *
 * @author 10904
 */
public class RequestAssetPhotosTask implements Callable<List<String>> {

    private final Gson gson = new Gson();
    private final String docId;
    private final String barcodeId;

    public RequestAssetPhotosTask(String docId, String barcodeId) {
        this.docId = docId;
        this.barcodeId = barcodeId;
    }

    @Override
    public List<String> call() throws Exception {
        String result = OkHttpUtils.getInstance().get(
                String.format(SysDefine.SERVER_ASSET_PHOTO_LIST(), docId, barcodeId));
        if (StringUtils.isNullOrEmpty(result) || result.contains("ERROR")) {
            return new ArrayList<>();
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {
        }.getType());
        return results == null ? new ArrayList<>() : results;
    }
}