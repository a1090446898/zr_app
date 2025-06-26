package com.gsoft.inventory.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gsoft.inventory.const_data.Log_file_name;
import com.gsoft.inventory.entities.AssetPhoto;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 10904
 */
public class AssetsUtils {
    private final static Gson gson = new Gson();

    public static void uploadAssets(Assets assets, Context context) {
        boolean isNet = NetworkUtils.checkNetConnect(context);
        if (!isNet) {
            return;
        }

        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("data", assets.toLineString());
        jsonArray.add(jsonObject);
        OkHttpUtils.getInstance().postAssetsJson(SysDefine.SERVERHOST_ASSETS(), gson.toJson(jsonArray), new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                try {
                    JSONArray jsonResult = new JSONArray(result);
                    if (jsonResult.length() >= 1) {
                        String resultStr = jsonResult.getJSONObject(0).getString("result");
                        if ("1".equals(resultStr)) {
                            if ("待删除".equals(assets.getREMARKS())) {
                                assets.delete();
                                showToast(context, "同步删除成功");
                            } else {
                                assets.setISUPLOAD("1");
                                assets.save();
                                showToast(context, "联网上传成功");
                            }
                        } else {
                            onResponseFailure("联网更新失败，请联系技术人员");
                        }
                    } else {
                        onResponseFailure("联网更新失败，请联系技术人员");
                    }
                } catch (JSONException e) {
                    onResponseFailure("联网更新失败，请联系技术人员");
                }
            }

            @Override
            public void onResponseFailure(String errMessage) {
                showToast(context, "联网上传失败");
            }
        });
    }

    public static void uploadAddAssets(Assets assets, Context context) {
        if (!NetworkUtils.checkNetConnect(context)) {
            return;
        }
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("data", assets.toLineAddString());
        jsonArray.add(jsonObject);
        OkHttpUtils.getInstance().postAssetsJson(SysDefine.SERVERHOST_ASSETS(), gson.toJson(jsonArray), new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                try {
                    JSONArray jsonResult = new JSONArray(result);
                    if (jsonResult.length() >= 1) {
                        String resultStr = jsonResult.getJSONObject(0).getString("result");
                        if ("1".equals(resultStr)) {
                            if ("待删除".equals(assets.getREMARKS())) {
                                assets.delete();
                                showToast(context, "同步删除成功");
                            } else {
                                assets.setISUPLOAD("1");
                                assets.save();
                                showToast(context, "联网上传成功");
                            }
                        } else {
                            onResponseFailure("联网更新失败，请联系技术人员");
                        }
                    } else {
                        onResponseFailure("联网更新失败，请联系技术人员");
                    }
                } catch (JSONException e) {
                    onResponseFailure("联网更新失败，请联系技术人员");
                }
            }

            @Override
            public void onResponseFailure(String errMessage) {
                showToast(context, "联网上传失败");
            }
        });
    }


    /**
     * 发起后端请求，进行复制图片
     *
     * @param context
     */
    public static void copyImgs(Assets assets, String copyDocId, String docId, String barcodeId, String qcyCode, Context context,  OnCopyCallback callback) {
        if (!NetworkUtils.checkNetConnect(context)) {
            if (callback != null) {
                callback.onCopyFailure("网络未连接");
            }
            return;
        }

        String url = SysDefine.COPY_IMG() + "&OLDID=" + copyDocId + "&NEWID=" + docId + "&BARCODEID=" + barcodeId + "&userId=" + qcyCode;
        OkHttpUtils.getInstance().get(url, new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                if ("true".equals(result)) {
                    assets.setISUPIMG(0);
                    assets.setCOPY_DOC_ID("");
                    assets.save();
                    // 进行回调
                    if (callback != null) {
                        callback.onCopySuccess(assets); // 成功回调
                    }
                } else {
                    onResponseFailure("联网复制图片失败，请联系技术人员");
                    if (callback != null) {
                        callback.onCopyFailure("联网复制图片失败，请联系技术人员");
                    }
                }
            }

            @Override
            public void onResponseFailure(String errMessage) {
                if (callback != null) {
                    callback.onCopyFailure("联网复制图片失败: " + errMessage);
                }
            }
        });
    }

    // 定义回调接口
    public interface OnCopyCallback {
        void onCopySuccess(Assets assets);
        void onCopyFailure(String errorMessage);
    }


    private static void showToast(Context context, String toast) {

        // 使用 Handler 切换到主线程显示 Toast
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
        });
    }

    public static boolean deleteAssets(String barCode) {
        String result = OkHttpUtils.getInstance().get(SysDefine.SERVERHOST_DEL().concat(barCode));
        return result.startsWith("ERROR:");
    }

    public static Assets cloneAssets(Assets source) {
        Assets clone = new Assets();
        if (SysConfig.assetsFieldsArray == null) {
            Field[] fields = source.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    //允许访问私有字段
                    field.setAccessible(true);
                    field.set(clone, field.get(source));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (int i = 0; i < SysConfig.assetsFieldsArray.length; i++) {
                try {
                    Field field = source.getClass().getDeclaredField(SysConfig.assetsFieldsArray[i]);
                    try {
                        //允许访问私有字段
                        field.setAccessible(true);
                        field.set(clone, field.get(source));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
        clone.setSERIALCODE("");
        clone.setBARCODEID("");
        clone.setISUPLOAD("0");
        clone.setDOCID("");
        return clone;
    }


    public static void uploadAssetPhotos(Handler handler, Context context) {
        List<AssetPhoto> photoList = AssetPhoto.find(AssetPhoto.class, "STATUS=?", "待上传");
        // ----- 测试增加
/*        AssetPhoto assetPhoto = photoList.get(0);
        assetPhoto.setDoc("123");
        for (int i = 0; i < 5; i++) {
            photoList.add(assetPhoto);
        }*/
        // ----- 测试结束
        int size = photoList.size();
        if (size == 0) {
            return;
        }

        CountDownLatch countLatch = new CountDownLatch(size);
        AtomicInteger doCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger allCount = new AtomicInteger(0);


        String message = MessageFormat.format("上传数量({0},{1}), 正在上传{2}", size, doCount.get(), "");

        // 更新进度条
        updateProgress(handler, 0, message, 1);

        // 创建一个单线程池
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        for (int i = 0; i < size; i++) {
            AssetPhoto photo = photoList.get(i);
            executorService.submit(() -> uploadAssetPhoto(context, photo, countLatch, size, failCount, doCount, allCount, handler));
        }

        // 在后台线程中等待所有上传任务完成
        executorService.submit(() -> {
            try {
                countLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                String msg = MessageFormat.format("所有同步完成，总数{0}，成功{1}，失败{2}", size, doCount.get(), failCount.get());
                updateProgress(handler, 100, msg, 0);
            }
        });

    }

    private static void updateProgress(Handler handler, int progress, String message, int show) {
        Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("progress", progress);
        b.putString("message", message);
        b.putInt("show", show);
        msg.setData(b);
        handler.sendMessage(msg);
    }

    public static void uploadAssetPhoto(Context context, AssetPhoto fileTmp,
                                        CountDownLatch countLatch,
                                        int size,
                                        AtomicInteger failCount, AtomicInteger doCount, AtomicInteger allCount,
                                        Handler handler) {

        String docId = fileTmp.getDoc();
        String barCode = fileTmp.getCode();
        String userId = fileTmp.getUser();
        File file = new File(fileTmp.getPath());
        long photoId = fileTmp.getId();
        Map<String, String> params = new HashMap<>();
        params.put("DOCID", docId);
        params.put("BARCODEID", barCode);
        params.put("USERID", userId);
        String name = file.getName();
        // name截取前五个字符
        String fileName = name.substring(0, 16);
        fileName += "…";
        String finalFileName = fileName;

        CountDownLatch latch = new CountDownLatch(1);
        StringBuffer message = new StringBuffer();

        OkHttpUtils.getInstance().uploadImageFile(SysDefine.SERVER_ASSET_UPLOAD_PHOTO(), file, params, new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                allCount.getAndIncrement();
                String msg = "";
                if ("true".equals(result)) {
                    doCount.getAndIncrement();
                    msg = MessageFormat.format("总数量{0}，成功{1}，失败{2}, 文件{3}", size, doCount.get(), failCount.get(), finalFileName);
                    AssetPhoto assetPhoto = AssetPhoto.findById(AssetPhoto.class, photoId);
                    if (assetPhoto == null) {
                        return;
                    }
                    // 删除条目
                    assetPhoto.delete();
                    // 删除文件
//                    file.delete();
                } else {
                    failCount.getAndIncrement();
                    msg = MessageFormat.format("总数量{0}，成功{1}，失败{2}, 文件{3}", size, doCount.get(), failCount.get(), finalFileName);
                    LogToFile.toLogFile(context, Log_file_name.UPLOAD_PHOTO_LOG_FILE_NAME, "同步照片"+name+"失败, 原因：" + result + "\n");
                }
                message.append(msg);
                latch.countDown();
                countLatch.countDown();
            }

            @Override
            public void onResponseFailure(String errMessage) {
                allCount.getAndIncrement();
                failCount.getAndIncrement();
                String msg = MessageFormat.format("总数量{0}，成功{1}，失败{2}, 文件{3}", size, doCount.get(), failCount.get(), finalFileName);
                message.append(msg);
                AssetPhoto assetPhoto = AssetPhoto.findById(AssetPhoto.class, photoId);
                if (assetPhoto == null) {
                    return;
                }
                latch.countDown();
                countLatch.countDown();
            }
        });

        // 不阻塞主线程
        try {
            // 主线程等待，直到回调完成
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 处理中断异常
        } finally {
            // 更新进度条
            int process = allCount.get();
            double p = ((double) process / size) * 100;
            int processInt = (int) p;
            updateProgress(handler, processInt, message.toString(), 1);
        }


    }

    private static final AtomicInteger sequence = new AtomicInteger(0);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
    private static String lastTimestamp = "";

    public static synchronized String generalDocId(String deviceNo) {

        // 生成规则，随机一个大写字母，后面13位时间戳，最后四位随机数字
        Random random = new Random();
        // 1. 随机大写字母（A-Z）
        char letter = (char) (random.nextInt(26) + 'A');

        // 2. 13位时间戳（当前时间的毫秒数）
        long timestamp = System.currentTimeMillis();

        // 3. 四位随机数字（补零格式化）
        int suffix = random.nextInt(10000);
        String suffixStr = String.format("%04d", suffix);

        return "" + letter + timestamp + suffixStr;

    }


}
