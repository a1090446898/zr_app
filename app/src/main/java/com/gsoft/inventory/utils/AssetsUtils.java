package com.gsoft.inventory.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.const_data.Log_file_name;
import com.gsoft.inventory.entities.AssetPhoto;

import com.gsoft.inventory.utils.domain.Assets;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;

/**
 * @author 10904
 */
public class AssetsUtils {
    private final static Gson gson = new Gson();

    /** 已上传状态：统一由 PhotoStorageHelper 管理，避免两处定义不一致 */
    public static final String STATUS_UPLOADED = PhotoStorageHelper.STATUS_UPLOADED;

    public static void uploadAssets(Assets assets, Context context) {
        boolean isNet = NetworkUtils.checkNetConnect(context);
        if (!isNet) {
            showToast(context, "当前没有网络，联网上传失败");
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
                                assets.setISUPLOAD(Const.UPLOADED);
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

            @Override
            public void onFailure(Call call, IOException e) {
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
                                assets.setISUPLOAD(Const.UPLOADED);
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
        clone.setISUPLOAD(Const.NOT_UPLOAD);
        clone.setDOCID("");
        return clone;
    }


    public static void uploadAssetPhotos(Handler handler, Context context) {
        List<AssetPhoto> photoList = AssetPhoto.find(AssetPhoto.class, "STATUS=?", PhotoStorageHelper.STATUS_WAIT_UPLOAD);
        int size = photoList == null ? 0 : photoList.size();
        if (size == 0) {
            // 没有待上传照片也要给页面反馈，否则点了“一键上传”毫无反应
            updateProgress(handler, 100, "没有待上传的照片", 0);
            return;
        }

        CountDownLatch countLatch = new CountDownLatch(size);
        AtomicInteger doCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger allCount = new AtomicInteger(0);

        String message = MessageFormat.format("上传数量({0},{1}), 正在上传{2}", size, doCount.get(), "");
        updateProgress(handler, 0, message, 1);

        // 单线程池：照片逐张上传，避免同一单据并发写服务端
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        for (int i = 0; i < size; i++) {
            AssetPhoto photo = photoList.get(i);
            executorService.submit(() -> uploadAssetPhoto(context, photo, countLatch, size, failCount, doCount, allCount, handler));
        }

        // 等待任务必须在 shutdown 之前提交，否则会被线程池拒绝
        executorService.submit(() -> {
            try {
                countLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                String msg = MessageFormat.format("同步结束，总数{0}，成功{1}，失败{2}；失败的照片都保留在本地，可重新上传",
                        size, doCount.get(), failCount.get());
                updateProgress(handler, 100, msg, 0);
            }
        });
        executorService.shutdown();
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

    /**
     * 上传单张本地照片
     * <p>
     * 上传成功后只把记录状态改为“已上传”，绝不删除本地照片：
     * 本地照片是唯一副本，服务端没真正保存就删除会造成不可恢复的数据丢失。
     */
    public static void uploadAssetPhoto(Context context, AssetPhoto fileTmp,
                                        CountDownLatch countLatch,
                                        int size,
                                        AtomicInteger failCount, AtomicInteger doCount, AtomicInteger allCount,
                                        Handler handler) {
        try {
            doUploadAssetPhoto(context, fileTmp, countLatch, size, failCount, doCount, allCount, handler);
        } catch (Exception e) {
            // 任何异常都必须释放计数锁，否则进度框会永远停在“正在上传”
            e.printStackTrace();
            failCount.getAndIncrement();
            allCount.getAndIncrement();
            countLatch.countDown();
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            LogToFile.toLogFile(context, Log_file_name.UPLOAD_PHOTO_LOG_FILE_NAME, "同步照片异常, 原因：" + error + "\n");
            updateProgress(handler, 100, "照片上传异常：" + error, 1);
        }
    }

    private static void doUploadAssetPhoto(Context context, AssetPhoto fileTmp,
                                           CountDownLatch countLatch,
                                           int size,
                                           AtomicInteger failCount, AtomicInteger doCount, AtomicInteger allCount,
                                           Handler handler) {
        String docId = fileTmp.getDoc();
        String barCode = fileTmp.getCode();
        String userId = fileTmp.getUser();
        Long idValue = fileTmp.getId();
        long photoId = idValue == null ? 0L : idValue;
        String logFile = Log_file_name.UPLOAD_PHOTO_LOG_FILE_NAME;

        // 统一存储：历史压缩图迁移到外部资产照片目录，避免缓存被系统清理后上传失败
        Assets photoAssets = null;
        List<Assets> photoAssetsList = Assets.find(Assets.class, "DOCID=? AND BARCODEID=?", docId, barCode);
        if (photoAssetsList != null && !photoAssetsList.isEmpty()) {
            photoAssets = photoAssetsList.get(0);
        }
        String localPath = PhotoStorageHelper.ensurePhotoFile(context, photoAssets, fileTmp);
        if (StringUtils.isNullOrEmpty(localPath)) {
            localPath = fileTmp.getPath();
        }
        if (StringUtils.isNullOrEmpty(localPath)) {
            // 记录本身没有路径：计失败并释放计数锁，避免整个上传流程卡死
            failCount.getAndIncrement();
            allCount.getAndIncrement();
            countLatch.countDown();
            int progress = size <= 0 ? 0 : (int) ((allCount.get() * 100.0) / size);
            updateProgress(handler, progress, "照片文件路径为空", 1);
            LogToFile.toLogFile(context, logFile, "同步照片失败, 原因：照片记录路径为空\n");
            return;
        }
        File file = new File(localPath);
        if (!file.exists()) {
            // 文件已被剪切/清理：计失败并释放计数锁，避免整个上传流程卡死
            failCount.getAndIncrement();
            allCount.getAndIncrement();
            countLatch.countDown();
            int progress = size <= 0 ? 0 : (int) ((allCount.get() * 100.0) / size);
            updateProgress(handler, progress, "照片文件不存在：" + file.getName(), 1);
            LogToFile.toLogFile(context, logFile, "同步照片" + file.getName() + "失败, 原因：本地文件不存在\n");
            return;
        }

        Map<String, String> params = new HashMap<>();
        // 请求体里仍保留一份，兼容按表单域取参的服务端
        params.put("DOCID", docId);
        params.put("BARCODEID", barCode);
        params.put("USERID", userId);
        String name = file.getName();
        // 文件名过长时截断展示，避免substring越界
        String fileName = name.length() > 16 ? name.substring(0, 16) + "…" : name;
        String finalFileName = fileName;

        // 上传前记录服务端已有照片数，上传成功后用它判断服务端是否真的落库
        int beforeCount = queryServerPhotoCount(docId, barCode);

        CountDownLatch latch = new CountDownLatch(1);
        StringBuffer message = new StringBuffer();

        // 单据号/条码/用户同时放进查询串，避免服务端取不到参数
        OkHttpUtils.getInstance().uploadImageFile(
                SysDefine.SERVER_ASSET_UPLOAD_PHOTO(docId, barCode, userId), file, params, new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                allCount.getAndIncrement();
                boolean responseOk = result != null && "true".equalsIgnoreCase(result.trim());
                // 不能因为响应体不等于 true 就直接判失败：服务端可能已经保存成功，只是返回体格式不同
                int confirmState = confirmUploadedOnServer(docId, barCode, name, beforeCount);
                boolean success = confirmState == CONFIRM_OK
                        // 回查请求本身失败（网络抖动）时，以服务端明确的 true 为准，避免假失败导致重复上传
                        || (confirmState == CONFIRM_UNKNOWN && responseOk);
                String msg;
                if (success) {
                    doCount.getAndIncrement();
                    // 只改状态：记录和本地文件全部保留，便于事后核对与补传
                    markPhotoUploaded(photoId);
                    msg = MessageFormat.format("总数量{0}，成功{1}，失败{2}, 文件{3}", size, doCount.get(), failCount.get(), finalFileName);
                } else {
                    failCount.getAndIncrement();
                    msg = MessageFormat.format("总数量{0}，成功{1}，失败{2}, 文件{3}", size, doCount.get(), failCount.get(), finalFileName);
                    String reason = confirmState == CONFIRM_NOT_FOUND
                            ? "回查成功但服务端没有这张照片" : String.valueOf(result);
                    LogToFile.toLogFile(context, logFile, "同步照片" + name + "失败, 原因：" + reason
                            + ", DOCID=" + docId + ", BARCODEID=" + barCode + ", USERID=" + userId + "\n");
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
                LogToFile.toLogFile(context, logFile, "同步照片" + name + "失败, 原因：" + errMessage
                        + ", DOCID=" + docId + ", BARCODEID=" + barCode + ", USERID=" + userId + "\n");
                // 上传失败保留记录和文件，便于下次重试；无论记录是否存在都必须释放计数锁
                latch.countDown();
                countLatch.countDown();
            }
        });

        // 不阻塞主线程
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            int process = allCount.get();
            double p = size <= 0 ? 100 : ((double) process / size) * 100;
            int processInt = (int) p;
            updateProgress(handler, processInt, message.toString(), 1);
        }
    }

    /**
     * 回查确认的重试次数：服务端落库有延迟，立即回查容易把成功误判成失败 */
    private static final int CONFIRM_RETRY_TIMES = 3;
    /** 回查确认的重试间隔（毫秒） */
    private static final long CONFIRM_RETRY_DELAY_MS = 800L;
    /** 回查结论：确认服务端已有这张照片 */
    private static final int CONFIRM_OK = 1;
    /** 回查结论：查询正常，但服务端确实没有这张照片 */
    private static final int CONFIRM_NOT_FOUND = 0;
    /** 回查结论：查询本身失败（网络异常/返回体无法解析），无法给出结论 */
    private static final int CONFIRM_UNKNOWN = -1;

    /**
     * 回查服务器照片列表，确认本次照片真的挂在当前资产上
     *
     * @param beforeCount 上传前服务器已有照片数；-1 表示上传前查询失败
     * @return CONFIRM_OK / CONFIRM_NOT_FOUND / CONFIRM_UNKNOWN
     */
    private static int confirmUploadedOnServer(String docId, String barCode, String fileName, int beforeCount) {
        for (int i = 0; i < CONFIRM_RETRY_TIMES; i++) {
            if (i > 0) {
                try {
                    Thread.sleep(CONFIRM_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return CONFIRM_UNKNOWN;
                }
            }
            List<String> serverPhotos = queryServerPhotos(docId, barCode);
            if (serverPhotos == null) {
                // 本地查不到结论：继续重试，不能据此判上传失败
                continue;
            }
            if (beforeCount >= 0 && serverPhotos.size() > beforeCount) {
                return CONFIRM_OK;
            }
            if (containsPhoto(serverPhotos, fileName)) {
                return CONFIRM_OK;
            }
        }
        // 一次都没查成功，和“查到了但没有”必须区分开
        return queryServerPhotos(docId, barCode) == null ? CONFIRM_UNKNOWN : CONFIRM_NOT_FOUND;
    }

    /**
     * 查询服务器上该资产已上传的照片地址
     *
     * @return 查询失败返回 null；没有照片返回空列表
     */
    private static List<String> queryServerPhotos(String docId, String barCode) {
        try {
            String result = OkHttpUtils.getInstance().get(SysDefine.SERVER_ASSET_PHOTO_LIST(docId, barCode));
            if (result == null) {
                return null;
            }
            String body = result.trim();
            // 服务端没有照片时接口返回字面量 null（已实测），必须当成“0张”而不是查询失败，
            // 否则上传前的基线数量变成 -1，数量对比这层校验会直接失效
            if (body.length() == 0 || "null".equalsIgnoreCase(body) || "[]".equals(body)) {
                return new ArrayList<>();
            }
            if (body.startsWith("ERROR")) {
                return null;
            }
            List<String> photos = gson.fromJson(body, new TypeToken<List<String>>() {}.getType());
            return photos == null ? new ArrayList<>() : photos;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int queryServerPhotoCount(String docId, String barCode) {
        List<String> serverPhotos = queryServerPhotos(docId, barCode);
        return serverPhotos == null ? -1 : serverPhotos.size();
    }

    /**
     * 标记照片已上传：只更新状态，不删除数据库记录，也不删除本地文件
     */
    private static void markPhotoUploaded(long photoId) {
        if (photoId <= 0) {
            return;
        }
        try {
            AssetPhoto photo = AssetPhoto.findById(AssetPhoto.class, photoId);
            if (photo != null) {
                photo.setStatus(STATUS_UPLOADED);
                photo.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务端照片地址是否对应本次上传的文件名
     * <p>
     * 地址里可能带 ?IMGID=xx 这类查询串，中文/空格还会被百分号编码，
     * 必须先解码再去掉查询串比对，否则明明已上传也会判成没查到。
     */
    private static boolean containsPhoto(List<String> serverPhotos, String fileName) {
        if (serverPhotos == null || serverPhotos.isEmpty() || StringUtils.isNullOrEmpty(fileName)) {
            return false;
        }
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        String lowerBaseName = lowerName.contains(".")
                ? lowerName.substring(0, lowerName.lastIndexOf('.')) : lowerName;
        for (String url : serverPhotos) {
            if (url == null) {
                continue;
            }
            String path = url;
            int queryIndex = path.indexOf('?');
            if (queryIndex >= 0) {
                path = path.substring(0, queryIndex);
            }
            try {
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (Exception ignored) {
                // 解码失败按原文比对
            }
            path = path.toLowerCase(Locale.ROOT);
            if (path.contains(lowerName) || (lowerBaseName.length() >= 4 && path.contains(lowerBaseName))) {
                return true;
            }
        }
        return false;
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
