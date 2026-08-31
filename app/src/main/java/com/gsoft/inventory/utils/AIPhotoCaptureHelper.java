package com.gsoft.inventory.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import android.graphics.Bitmap;
import androidx.core.content.FileProvider;
import id.zelory.compressor.Compressor;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gsoft.inventory.InventoryApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.gsoft.inventory.utils.SysDefine.AI_PHOTO_UPLOAD_API;
import static com.gsoft.inventory.utils.SysDefine.AI_PHOTO_UPLOAD_RECOGNITION_API;

public class AIPhotoCaptureHelper {
    private static final String TAG = "AIPhotoCaptureHelper";
    private static final int REQUEST_CAMERA_PERMISSION = 1002;

    private InventoryApplication application;
    private Activity activity;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ProgressDialog progressDialog;
    private OnAIPhotoCompleteListener listener;
    private Assets currentAssets;

    private String currentPhotoPath; // 保存当前拍照的图片路径

    public interface OnAIPhotoCompleteListener {
        void onAIPhotoComplete(Assets assets);

        void onAIPhotoError(String error);
    }

    // 修改构造函数，接收预创建的ActivityResultLauncher
    public AIPhotoCaptureHelper(Activity activity, ActivityResultLauncher<Intent> cameraLauncher, OnAIPhotoCompleteListener listener) {
        this.activity = activity;
        this.cameraLauncher = cameraLauncher;
        this.listener = listener;
        application = (InventoryApplication) activity.getApplication();
    }

    /**
     * 开始AI拍照流程
     */
    public void startAIPhotoCapture(Assets assets) {
        this.currentAssets = assets;
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            launchCamera();
        }
    }

    /**
     * 启动相机
     */
    private void launchCamera() {
        Intent takePictureIntent = buildCameraIntent();
        if (takePictureIntent != null) {
            // 创建临时文件，指定相机输出路径，确保能获取到照片
            File photoFile = createTempImageFile();
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                // 必须显式授权，华为相机严格校验，否则无法写入照片
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                currentPhotoPath = photoFile.getAbsolutePath();
            }
            try {
                cameraLauncher.launch(takePictureIntent);
            } catch (Exception e) {
                Log.e(TAG, "启动相机失败", e);
                Toast.makeText(activity, "启动相机失败: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onAIPhotoError("启动相机失败: " + e.getMessage());
                }
            }
        } else {
            Toast.makeText(activity, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onAIPhotoError("没有可用的相机应用");
            }
        }
    }

    /**
     * 构建可用的相机Intent：
     * 1. 标准系统相机Intent（Android 11+ 需配合 Manifest 中 <queries> 声明才能解析）
     * 2. 鸿蒙部分机型标准Intent解析失败，逐一尝试显式指定华为相机包名
     */
    private Intent buildCameraIntent() {
        PackageManager pm = activity.getPackageManager();
        // 方式一：标准相机Intent
        Intent standardIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (standardIntent.resolveActivity(pm) != null) {
            return standardIntent;
        }
        // 方式二：华为/荣耀相机包名逐一尝试（不同EMUI/鸿蒙版本相机包名不同）
        String[] cameraPackages = {
                "com.huawei.camera",
                "com.huawei.hwcamera",
                "com.huawei.camera2",
                "com.huawei.camera3d"
        };
        for (String pkg : cameraPackages) {
            Intent pkgIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            pkgIntent.setPackage(pkg);
            if (pkgIntent.resolveActivity(pm) != null) {
                Log.d(TAG, "使用华为相机包名: " + pkg);
                return pkgIntent;
            }
        }
        return null;
    }

    /**
     * 创建临时图片文件
     */
    private File createTempImageFile() {
        try {
            File storageDir = activity.getExternalFilesDir("temp_pictures");
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }
            Accounts loginAccount = application.getLoginAccount();
            String userId = loginAccount.getYHBH();
            String fileName = "ai_photo_" + userId + "_" + System.currentTimeMillis() + ".jpg";
            return new File(storageDir, fileName);
        } catch (Exception e) {
            Log.e(TAG, "创建临时文件失败", e);
            return null;
        }
    }

    public String getCapturedPhotoPath() {
        return currentPhotoPath;
    }

    /**
     * 压缩图片
     */
    private String compressImage(String imagePath) {
        try {
            File file = new File(imagePath);
            File compressedFile = new Compressor(activity)
                    .setMaxWidth(1280)
                    .setMaxHeight(720)
                    .setQuality(70)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .compressToFile(file);
            // 压缩后没有变小则使用原图
            if (compressedFile.length() >= file.length()) {
                compressedFile.delete();
                Log.d(TAG, "压缩后未变小，使用原图");
                return imagePath;
            }
            String compressedPath = compressedFile.getAbsolutePath();
            Log.d(TAG, "图片压缩完成: " + compressedPath + "，大小 " + file.length() / 1024 + "KB -> " + compressedFile.length() / 1024 + "KB");
            return compressedPath;
        } catch (Exception e) {
            Log.e(TAG, "图片压缩失败，使用原图", e);
            return imagePath;
        }
    }

    /**
     * 获取最新拍摄的照片路径
     */
    private String getLatestImageFromGallery() {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Images.ImageColumns.DATE_ADDED + " DESC";

        Cursor cursor = activity.getContentResolver().query(
                contentUri,
                projection,
                null,
                null,
                sortOrder
        );

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();
            return imagePath;
        }
        return null;
    }

    /**
     * 上传图片到服务器并获取URL
     */
    private void uploadImageToServer(String imagePath) {
        // 先压缩图片
        String compressedPath = compressImage(imagePath);


        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        // 获取当前登录账户信息

        Accounts loginAccount = application.getLoginAccount();
        String userId = loginAccount.getYHBH();

        String imageName = "ai_photo_" + userId+"_" + System.currentTimeMillis() + ".jpg";

        // 上传压缩后的图片，而不是原图
        File imageFile = new File(compressedPath);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageName,
                        RequestBody.create(MediaType.parse("image/jpeg"), imageFile))
                .build();

        // 首先上传图片获取URL
        Request request = new Request.Builder()
                .url(AI_PHOTO_UPLOAD_API) // 先上传图片获取URL
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(() -> {
                    hideUploadProgress();
                    Log.e(TAG, "上传失败", e);
                    Toast.makeText(activity, "上传失败: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    if (listener != null) {
                        listener.onAIPhotoError("上传失败: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();

                activity.runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            // 解析响应，获取图片URL
                            JSONObject jsonResponse = new JSONObject(responseData);
                            Log.d(TAG, responseData);
                            String imageUrl = jsonResponse.optString("url", "");

                            if (!imageUrl.isEmpty()) {
                                // 图片上传成功，获取到URL，现在调用识图接口
                                callImageRecognitionApi(imageUrl);
                            } else {
                                hideUploadProgress();
                                Toast.makeText(activity, "获取图片URL失败",
                                        Toast.LENGTH_LONG).show();
                                if (listener != null) {
                                    listener.onAIPhotoError("获取图片URL失败");
                                }
                            }
                        } catch (Exception e) {
                            hideUploadProgress();
                            Log.e(TAG, "解析图片上传响应失败", e);
                            Toast.makeText(activity, "解析上传响应失败",
                                    Toast.LENGTH_LONG).show();
                            if (listener != null) {
                                listener.onAIPhotoError("解析上传响应失败");
                            }
                        }
                    } else {
                        hideUploadProgress();
                        Toast.makeText(activity, "图片上传失败: " + responseData,
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, responseData);
                        if (listener != null) {
                            listener.onAIPhotoError("图片上传失败: " + responseData);
                        }
                    }
                });
            }
        });
    }

    /**
     * 调用识图接口
     */
    private void callImageRecognitionApi(String imageUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        try {
            // 构建JSON请求体，传递图片URL和固定的识别字段
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("imageUrl", imageUrl);

            JSONArray fieldsArray = new JSONArray();

            JSONObject nameField = new JSONObject();
            nameField.put("key", "name");
            nameField.put("label", "产品名称");
            nameField.put("hint", "产品名称");
            fieldsArray.put(nameField);

            JSONObject brandField = new JSONObject();
            brandField.put("key", "brand");
            brandField.put("label", "品牌");
            brandField.put("hint", "产品品牌");
            fieldsArray.put(brandField);

            JSONObject modelField = new JSONObject();
            modelField.put("key", "model");
            modelField.put("label", "产品型号");
            modelField.put("hint", "产品型号");
            fieldsArray.put(modelField);

            JSONObject unitField = new JSONObject();
            unitField.put("key", "unit");
            unitField.put("label", "计量单位");
            unitField.put("hint", "物品的计量单位，比如个，把，颗等");
            fieldsArray.put(unitField);

            jsonBody.put("fields", fieldsArray);

            // 按产品名称匹配资产分类
            JSONArray categoryMatchKeys = new JSONArray();
            categoryMatchKeys.put("name");
            jsonBody.put("categoryMatchKeys", categoryMatchKeys);



            jsonBody.put("fields", fieldsArray);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody.toString()
            );

            Request request = new Request.Builder()
                    .url(AI_PHOTO_UPLOAD_RECOGNITION_API) // 调用识图接口
                    .post(requestBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        hideUploadProgress();
                        Log.e(TAG, "识图请求失败", e);
                        Toast.makeText(activity, "识图请求失败: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        if (listener != null) {
                            listener.onAIPhotoError("识图请求失败: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body().string();

                    activity.runOnUiThread(() -> {
                        hideUploadProgress();
                        if (response.isSuccessful()) {
                            Toast.makeText(activity, "AI识别完成",
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "识图返回数据："+ responseData);
                            // 解析服务器返回的数据
                            Assets parsedAssets = parseAssetData(responseData);
                            if (parsedAssets != null) {
                                if (listener != null) {
                                    listener.onAIPhotoComplete(parsedAssets);
                                }
                            } else {
                                if (listener != null) {
                                    listener.onAIPhotoError("解析识别结果失败");
                                }
                            }
                        } else {
                            String friendlyMessage = buildRecognitionErrorMessage(responseData);
                            Toast.makeText(activity, friendlyMessage,
                                    Toast.LENGTH_LONG).show();
                            if (listener != null) {
                                listener.onAIPhotoError(friendlyMessage);
                            }
                        }

                    });
                }
            });
        } catch (
                Exception e) {
            Log.e(TAG, "构建识图请求JSON失败", e);
            activity.runOnUiThread(() -> {
                hideUploadProgress();
                Toast.makeText(activity, "构建识图请求失败: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                if (listener != null) {
                    listener.onAIPhotoError("构建识图请求失败: " + e.getMessage());
                }
            });


        }
    }

    /**
     * 将识别接口错误响应转换为用户友好的提示信息
     */
    private String buildRecognitionErrorMessage(String responseData) {
        try {
            JSONObject jsonResponse = new JSONObject(responseData);
            JSONObject errorObj = jsonResponse.optJSONObject("error");
            if (errorObj != null) {
                String message = errorObj.optString("message", "");
                // 视觉模型下载图片超时
                if (message.contains("Timeout while downloading url")) {
                    return "图片识别超时，请检查网络后重试";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析识别错误响应失败", e);
        }
        return "识别失败: " + responseData;
    }

    /**
     * 解析服务器返回的资产数据
     */
    private Assets parseAssetData(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            Assets assets = new Assets();

            // 数据在 data.values 中
            JSONObject dataObj = jsonObject.optJSONObject("data");
            if (dataObj != null) {
                JSONObject values = dataObj.optJSONObject("values");
                if (values != null) {
                    assets.setASSETSNAME(optStringValue(values, "name"));
                    assets.setBRAND(optStringValue(values, "brand"));
                    assets.setASSETSMODEL(optStringValue(values, "model"));
                    assets.setASSETSMEASURE(optStringValue(values, "unit"));
                }

                // 解析分类匹配结果：只取第一个有结果的key的第一个候选项
                JSONObject categoryMatches = dataObj.optJSONObject("categoryMatches");
                if (categoryMatches != null && categoryMatches.length() > 0) {
                    Iterator<String> keys = categoryMatches.keys();
                    while (keys.hasNext()) {
                        JSONArray matches = categoryMatches.optJSONArray(keys.next());
                        if (matches == null || matches.length() == 0) {
                            continue;
                        }
                        JSONObject match = matches.optJSONObject(0);
                        if (match == null) {
                            continue;
                        }
                        // 资产类别code：本地确认合法才赋值（大类code格式与本地表不一致，用名称转换校验）
                        String parentCategoryCode = match.optString("parentCategoryCode", "");
                        if (!StringUtils.isNullOrEmpty(parentCategoryCode)
                                && !StringUtils.isNullOrEmpty(AssetsCategoryHelper.getAssetsCategoryTitle(parentCategoryCode))) {
                            assets.setASSETSTYPE(parentCategoryCode);
                        }
                        // 资产分类code：查本地表确认存在才赋值
                        String categoryCode = match.optString("categoryCode", "");
                        if (!StringUtils.isNullOrEmpty(categoryCode)
                                && AssetsCategoryHelper.getAssetsCategory(categoryCode) != null) {
                            assets.setASSETSSORTCODE(categoryCode);
                        }
                        break;
                    }
                }
            }

            return assets;
        } catch (Exception e) {
            Log.e(TAG, "解析资产数据失败", e);
            return null;
        }
    }

    /**
     * 安全读取JSON字符串字段：字段不存在、值为JSON null或字符串"null"时统一返回空串
     */
    private String optStringValue(JSONObject jsonObject, String key) {
        if (jsonObject == null || jsonObject.isNull(key)) {
            return "";
        }
        String value = jsonObject.optString(key, "");
        return "null".equals(value) ? "" : value;
    }

    /**
     * 处理权限请求结果
     */
    public void handlePermissionResult(int requestCode, String[] permissions,
                                       int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(activity, "需要相机权限才能使用AI拍照功能", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onAIPhotoError("需要相机权限才能使用AI拍照功能");
                }
            }
        }
    }

    public void showUploadProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("正在上传照片到服务器...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void hideUploadProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    /**
     * 直接上传图片到服务器（供外部调用）
     */
    public void uploadImageToServerPublic(String imagePath) {
        uploadImageToServer(imagePath);
    }

    /**
     * 获取最新图像（供外部调用）
     */
    public String getLatestImageFromGalleryPublic() {
        return getLatestImageFromGallery();
    }

}
