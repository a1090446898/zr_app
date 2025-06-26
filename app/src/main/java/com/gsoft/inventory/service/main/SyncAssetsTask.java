package com.gsoft.inventory.service.main;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.const_data.Log_file_name;
import com.gsoft.inventory.entities.SyncResult;
import com.gsoft.inventory.utils.*;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

/**
 * @author 10904
 */
public class SyncAssetsTask implements Callable<String> {
    private Context context;

    private String zcqcyCode;

    public SyncAssetsTask(Context context, String zcqcyCode) {
        this.context = context;
        this.zcqcyCode = zcqcyCode;
    }

    @Override
    public String call() throws Exception {
        List<Assets> assetsList = Assets.find(Assets.class, "ISUPLOAD=?", "0");
        List<Assets> needCopyImgList = Assets.find(Assets.class, "ISUPIMG=?", "1");

        StringBuilder stringBuilder = new StringBuilder();
        String responseString = "";
        CountDownLatch imageSyncLatch = new CountDownLatch(needCopyImgList.size());
        AtomicBoolean isImageSyncFailed = new AtomicBoolean(false);
        if(!needCopyImgList.isEmpty()){
            stringBuilder.append("同步图片").append(needCopyImgList.size()).append("份。\n");
        }
        // 先同步图片，如果图片同步失败，则不同步数据
        for (Assets assets : needCopyImgList) {
            if(assets.getISUPIMG() == null || assets.getISUPIMG() == 1){
                AssetsUtils.copyImgs(assets, assets.getCOPY_DOC_ID(), assets.getDOCID(), assets.getBARCODEID(), zcqcyCode, context, new AssetsUtils.OnCopyCallback() {
                    @Override
                    public void onCopySuccess(Assets assets) {
                        imageSyncLatch.countDown(); // 同步成功，计数减1
                    }

                    @Override
                    public void onCopyFailure(String errorMessage) {
                        isImageSyncFailed.set(true); // 标记失败
                        imageSyncLatch.countDown(); // 同步失败，计数减1
                    }
                });
            }else {
                imageSyncLatch.countDown(); // 无需同步图片，直接减1
            }
        }

        // 2. 等待所有图片同步完成
        try {
            imageSyncLatch.await(); // 阻塞等待
        } catch (InterruptedException e) {
            return "图片同步被中断，请重试";
        }

        // 3. 检查图片同步结果
        if (isImageSyncFailed.get()) {
            return "存在图片同步失败，已停止数据同步！";
        }



        if (!assetsList.isEmpty()) {
            JsonArray jsonArray = new JsonArray();
            for (Assets assets : assetsList) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("data", assets.toLineString());
                jsonArray.add(jsonObject);
            }

            try {
                responseString = OkHttpUtils.getInstance().postAssetsJson(SysDefine.SERVERHOST_ASSETS(), gson.toJson(jsonArray));
                List<SyncResult> results = gson.fromJson(responseString, new TypeToken<List<SyncResult>>() {}.getType());
                if (results == null || results.isEmpty()) {
                    return "同步失败，请联系技术人员";
                }
                for (SyncResult sr : results) {
                    String barcode = sr.barcodeid;
                    if (sr.result.equals("1")) {
                        List<Assets> assets = Assets.find(Assets.class, "BARCODEID=?", barcode);
                        for (Assets ast : assets) {
                            if (ast.getREMARKS().equals("待删除")) {
                                ast.delete();
                            } else {
                                ast.setISUPLOAD("1");
                                ast.save();
                            }
                        }
                    } else {
                        stringBuilder.append("条码：".concat(barcode).concat("同步失败\n"));
                    }
                }
            } catch (Exception e) {
                return "同步失败，请联系技术人员";
            }
        }
        String result = stringBuilder.toString();
        if(result.isEmpty()){
            return "同步成功";
        }
        else{
            String error = "同步失败数据: \n" + responseString + "\n";
            LogToFile.toLogFile(context, Log_file_name.LOG_ASSETS_SYNC_ERROR_FILE_NAME, error);
        }
        return result;
    }
}
