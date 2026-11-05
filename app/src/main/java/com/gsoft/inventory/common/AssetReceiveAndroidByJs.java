package com.gsoft.inventory.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.gsoft.inventory.BuildConfig;
import com.gsoft.inventory.InventoryApplication;
import com.gsoft.inventory.LoginActivity;
import com.gsoft.inventory.abs.WebFragment;
import com.gsoft.inventory.const_data.ReqPageCodeConst;
import com.gsoft.inventory.const_data.UserRoleConst;
import com.gsoft.inventory.service.android_by_js.UploadFileByWebTask;
import com.gsoft.inventory.service.download.ZRFileDownloadManager;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.down.DownloadedFileUtils;
import com.gsoft.inventory.view.home.MainPagerAdapter;
import com.gsoft.inventory.view.pdf_view.PdfViewerActivity;
import com.gsoft.inventory.view.scan.BarcodeScannerActivity;
import com.xuexiang.xupdate.utils.UpdateUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
import static com.gsoft.inventory.InventoryApplication.getApplication;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.*;
import static com.gsoft.inventory.utils.DateFormatUtils.YYYYMMDD_HHMMSS;

/**
 * @author 10904
 */
public class AssetReceiveAndroidByJs {

    private Activity activity;
    private WebFragment webFragment;
    private WebView webView;

    private Uri capturedImageURI;

    private MainPagerAdapter mainPagerAdapter;

    /** H5 附件请求类型：下载 */
    private static final int FILE_REQUEST_DOWNLOAD = 1;
    /** H5 附件请求类型：打开（本地不存在时静默下载） */
    private static final int FILE_REQUEST_OPEN = 2;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** 等待存储权限授予后自动继续的附件请求 */
    private final List<PendingFileRequest> pendingFileRequests =
            Collections.synchronizedList(new ArrayList<PendingFileRequest>());

    /** Android 13 通知权限是否已发起过请求，避免重复弹框 */
    private boolean notificationPermissionRequested = false;

    public AssetReceiveAndroidByJs() {
    }

    public AssetReceiveAndroidByJs(Activity activity, WebView webView, WebFragment webFragment) {
        this.activity = activity;
        this.webView = webView;
        this.webFragment = webFragment;

    }

    @JavascriptInterface
    public void redirectLoginPage () {
        // 重定向到登录页面
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivity(intent);
    }

    @JavascriptInterface
    public String getLoginUserInfo() {
        // 返回当前登录用户信息
        Accounts loginAccount = ((InventoryApplication) getApplication()).getLoginAccount();
        String ipService = SharedPreferencesUtils.getString(activity, SharedPreferencesUtils.SP_KEY_SERVERHOST, "http://www.zrast.com");
        // 将登录用户关键信息转换成JSON字符串
        try {
            if (loginAccount == null) {
                return "";
            }
            // 创建一个 JSONObject 用于存储用户信息
            String flavor = BuildConfig.FLAVOR;
            JSONObject userInfoJson = new JSONObject();
            // 假设 Accounts 类有 getUsername 和 getUserId 方法，根据实际情况修改
            userInfoJson.put("userId", loginAccount.getYHBH());
            userInfoJson.put("userName", loginAccount.getYHXM());
            userInfoJson.put("deptId", loginAccount.getBMBH());
            userInfoJson.put("deptName", loginAccount.getBMMC());
            userInfoJson.put("phone", loginAccount.getPHONE());
            userInfoJson.put("accountId", loginAccount.getZTBH());
            userInfoJson.put("accountName", loginAccount.getZTMC());
            userInfoJson.put("ipService", ipService);
            userInfoJson.put("appFlavor", flavor);
            // 权限信息：原始角色值 + 可见导入模式，复用 UserRoleConst 保证与原生导入页口径一致
            String role = loginAccount.getROLE() == null ? "" : loginAccount.getROLE();
            JSONArray importModes = new JSONArray();
            for (int mode : UserRoleConst.getVisibleModes(role)) {
                importModes.put(getImportModeKey(mode));
            }
            userInfoJson.put("role", role);
            userInfoJson.put("importModes", importModes);
            // 将 JSONObject 转换成 JSON 字符串
            return userInfoJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 导入模式转换为H5可识别的权限标识
     *
     * @param mode UserRoleConst 中的导入模式
     * @return personal 个人导入；department 部门导入；all 全部部门导入
     */
    private String getImportModeKey(int mode) {
        switch (mode) {
            case UserRoleConst.MODE_PERSONAL:
                return "personal";
            case UserRoleConst.MODE_DEPARTMENT:
                return "department";
            default:
                return "all";
        }
    }

    @JavascriptInterface
    public String getAndroidVersion() {
       return UpdateUtils.getVersionName(activity);
    }

    // 供 JavaScript 调用的方法
    @JavascriptInterface
    public void scanCode() {
        // 将 context 转换为 Activity 类型，调用 Activity 自带的 startActivityForResult 方法
        if (activity != null) {
            Intent intent = new Intent(activity, BarcodeScannerActivity.class);
            activity.startActivityForResult(intent, REQUEST_CODE_BARCODE_SCAN);

        }
    }


    // 从相册选择图片
    @JavascriptInterface
    public void toAlbum() {
        activity.runOnUiThread(() -> {
            if (CommonPermissionUtils.checkStoragePermission(activity)) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");

                activity.startActivityForResult(Intent.createChooser(intent, "选择图片"), FILE_CHOOSER_REQUEST_CODE);
            }
        });
    }

    // 从相册选择图片
    @JavascriptInterface
    public void toWebFragmentAlbum() {
        activity.runOnUiThread(() -> {
            if (CommonPermissionUtils.checkStoragePermission(activity)) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                webFragment.startActivityForResult(Intent.createChooser(intent, "选择图片"), FILE_CHOOSER_REQUEST_CODE);
            }
        });
    }


    @JavascriptInterface
    public void toShowPdf(String url) {
        Intent testIntent1 = new Intent(activity, PdfViewerActivity.class);
        testIntent1.putExtra("PDF_URL", url);
        activity.startActivity(testIntent1);
    }

    /**
     * H5 调用：下载附件
     * fileName 约定为 "{fileId}_{原始文件名}"，原生全程透传不做解析
     * extra 为 JSON 字符串扩展位，当前版本传 "{}" 或空串，解析失败按空对象处理
     */
    @JavascriptInterface
    public void downloadFile(String fileUrl, String fileName, String extra) {
        handleFileRequest(fileUrl, fileName, extra, FILE_REQUEST_DOWNLOAD);
    }

    /**
     * H5 调用：打开附件（本地已存在直接打开，不存在先静默下载再打开）
     */
    @JavascriptInterface
    public void openFile(String fileUrl, String fileName, String extra) {
        handleFileRequest(fileUrl, fileName, extra, FILE_REQUEST_OPEN);
    }

    /**
     * H5 调用：存储权限引导
     * Android 11+ 跳“所有文件访问”设置页；Android 6~10 直接弹系统权限框
     */
    @JavascriptInterface
    public void openAppSettings() {
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (DownloadZrFileManager.hasWritePermission(activity)) {
                resumePendingFileRequests();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                    activity.startActivityForResult(intent, REQUEST_CODE_DOWNLOAD_SETTINGS);
                } catch (Exception e) {
                    // 部分定制系统不支持该入口时退回应用详情页
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                        activity.startActivityForResult(intent, REQUEST_CODE_DOWNLOAD_SETTINGS);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                requestStoragePermission();
            }
        });
    }

    /**
     * 处理附件请求：参数校验、本地文件判断、权限不足时挂起等待授权
     */
    private void handleFileRequest(String fileUrl, String fileName, String extra, int requestType) {
        if (StringUtils.isNullOrEmpty(fileUrl) || StringUtils.isNullOrEmpty(fileName)) {
            notifyFileRequestFailure(requestType, fileName, "参数错误：下载地址或文件名为空");
            return;
        }
        if (activity == null) {
            notifyFileRequestFailure(requestType, fileName, "页面已关闭");
            return;
        }
        JSONObject extraObject = parseExtra(extra);
        String bizModule = extraObject.optString("bizModule", "");
        if (!StringUtils.isNullOrEmpty(bizModule)) {
            Log.d("H5FileBridge", "附件请求，业务模块：" + bizModule + "，文件：" + fileName);
        }
        requestNotificationPermissionIfNeed();

        // openFile：本地已有完整文件时直接打开，不发起任何网络请求
        if (requestType == FILE_REQUEST_OPEN) {
            File localFile = DownloadedFileUtils.getLocalFile(fileName);
            if (localFile.exists() && localFile.length() > 0) {
                openDownloadedFile(localFile, fileName);
                return;
            }
        }

        PendingFileRequest request = new PendingFileRequest(requestType, fileUrl, fileName, extra);
        if (!DownloadZrFileManager.hasWritePermission(activity)) {
            boolean firstPending;
            synchronized (pendingFileRequests) {
                firstPending = pendingFileRequests.isEmpty();
                pendingFileRequests.add(request);
            }
            if (firstPending) {
                callJs("onNeedStoragePermission");
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && firstPending) {
                requestStoragePermission();
            }
            return;
        }
        executeFileRequest(request);
    }

    private void executeFileRequest(final PendingFileRequest request) {
        if (activity == null) {
            notifyFileRequestFailure(request.requestType, request.fileName, "页面已关闭");
            return;
        }
        // 授权后重试 openFile 时再次确认本地文件，避免重复下载
        if (request.requestType == FILE_REQUEST_OPEN) {
            File localFile = DownloadedFileUtils.getLocalFile(request.fileName);
            if (localFile.exists() && localFile.length() > 0) {
                openDownloadedFile(localFile, request.fileName);
                return;
            }
        }
        Context appContext = activity.getApplicationContext();
        ZRFileDownloadManager.getInstance(appContext).enqueue(request.fileUrl, request.fileName,
                request.requestType == FILE_REQUEST_DOWNLOAD, new ZRFileDownloadManager.RequestCallback() {
                    @Override
                    public void onProgress(String name, int percent, long currentBytes, long totalBytes) {
                        callJs("onFileDownloadProgress", name, percent, currentBytes, totalBytes);
                    }

                    @Override
                    public void onFinished(boolean success, String savedPath, String msg) {
                        if (request.requestType == FILE_REQUEST_DOWNLOAD) {
                            callJs("onFileDownloaded", request.fileName, savedPath, success, msg);
                            return;
                        }
                        if (!success) {
                            // 需求约定 openFile 链路通用失败文案为“文件下载失败”，权限与鉴权原因原样透出
                            String openMsg = "文件下载失败";
                            if (!StringUtils.isNullOrEmpty(msg) && (msg.contains("登录已失效")
                                    || msg.contains("存储空间不足") || msg.contains("存储权限"))) {
                                openMsg = msg;
                            }
                            callJs("onFileOpened", request.fileName, false, openMsg);
                            return;
                        }
                        openDownloadedFile(new File(savedPath), request.fileName);
                    }
                });
    }

    private void openDownloadedFile(final File file, final String fileName) {
        if (activity == null) {
            callJs("onFileOpened", fileName, false, "页面已关闭");
            return;
        }
        final String mimeType = DownloadedFileUtils.getMimeType(fileName);
        activity.runOnUiThread(() -> {
            String error = DownloadedFileUtils.openWithExternalApp(activity, file, mimeType);
            if (error == null) {
                callJs("onFileOpened", fileName, true, "");
            } else {
                callJs("onFileOpened", fileName, false, error);
            }
        });
    }

    /**
     * 供 WebView 容器转发：权限申请结果
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_DOWNLOAD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                resumePendingFileRequests();
            } else {
                failAllPendingFileRequests("存储权限未授予");
            }
        }
    }

    /**
     * 供 WebView 容器转发：设置页返回
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_DOWNLOAD_SETTINGS) {
            return;
        }
        if (activity != null && DownloadZrFileManager.hasWritePermission(activity)) {
            resumePendingFileRequests();
        } else {
            failAllPendingFileRequests("存储权限未授予");
        }
    }

    private void requestStoragePermission() {
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(() -> ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_DOWNLOAD_PERMISSION));
    }

    private void resumePendingFileRequests() {
        List<PendingFileRequest> requests;
        synchronized (pendingFileRequests) {
            if (pendingFileRequests.isEmpty()) {
                return;
            }
            requests = new ArrayList<>(pendingFileRequests);
            pendingFileRequests.clear();
        }
        for (PendingFileRequest request : requests) {
            executeFileRequest(request);
        }
    }

    private void failAllPendingFileRequests(String msg) {
        List<PendingFileRequest> requests;
        synchronized (pendingFileRequests) {
            if (pendingFileRequests.isEmpty()) {
                return;
            }
            requests = new ArrayList<>(pendingFileRequests);
            pendingFileRequests.clear();
        }
        for (PendingFileRequest request : requests) {
            notifyFileRequestFailure(request.requestType, request.fileName, msg);
        }
    }

    private void notifyFileRequestFailure(int requestType, String fileName, String msg) {
        String name = fileName == null ? "" : fileName;
        if (requestType == FILE_REQUEST_DOWNLOAD) {
            callJs("onFileDownloaded", name, "", false, msg);
        } else {
            callJs("onFileOpened", name, false, msg);
        }
    }

    private JSONObject parseExtra(String extra) {
        if (StringUtils.isNullOrEmpty(extra)) {
            return new JSONObject();
        }
        try {
            return new JSONObject(extra);
        } catch (Exception e) {
            // 非法 JSON 按空对象处理，不报错、不中断任务
            return new JSONObject();
        }
    }

    private void requestNotificationPermissionIfNeed() {
        // compileSdk 为 30，Android 13 的权限常量用字面量 33 判断
        if (Build.VERSION.SDK_INT < 33 || notificationPermissionRequested || activity == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionRequested = true;
        activity.runOnUiThread(() -> ActivityCompat.requestPermissions(activity,
                new String[]{POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATION_PERMISSION));
    }

    /**
     * 回调 H5：所有参数做 JSON 转义，避免文件名含特殊字符时破坏 JS 语法
     */
    private void callJs(final String function, final Object... args) {
        if (webView == null) {
            return;
        }
        StringBuilder script = new StringBuilder();
        script.append("javascript:(function(){try{var f=window.androidFun&&window.androidFun.")
                .append(function)
                .append(";if(typeof f!=='function'){return;}f(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                script.append(",");
            }
            Object arg = args[i];
            if (arg instanceof Number || arg instanceof Boolean) {
                script.append(String.valueOf(arg));
            } else {
                script.append(JSONObject.quote(arg == null ? "" : String.valueOf(arg)));
            }
        }
        script.append(");}catch(e){}})()");
        final String jsCode = script.toString();
        mainHandler.post(() -> {
            try {
                if (webView != null) {
                    webView.evaluateJavascript(jsCode, null);
                }
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * 待存储权限授予后继续的附件请求
     */
    private static class PendingFileRequest {
        final int requestType;
        final String fileUrl;
        final String fileName;
        final String extra;

        PendingFileRequest(int requestType, String fileUrl, String fileName, String extra) {
            this.requestType = requestType;
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.extra = extra;
        }
    }

    // 拍照
    @SuppressLint("QueryPermissionsNeeded")
    @JavascriptInterface
    public void takePhoto() {
        // 在主线程中执行以下操作，因为涉及到启动 Activity 等 UI 相关操作
        activity.runOnUiThread(() -> {
            // 检查是否已经获取了存储权限
            if (CommonPermissionUtils.checkCameraPermission(activity) && CommonPermissionUtils.checkStoragePermission(activity)) {
                // 创建一个用于启动相机拍照的 Intent
                Intent takePictureIntent = new Intent(ACTION_IMAGE_CAPTURE);
                // 添加默认类别（华为系统需要显式声明）
                takePictureIntent.addCategory(Intent.CATEGORY_DEFAULT);
                // 检查设备上是否有能够处理该拍照 Intent 的 Activity
                PackageManager packageManager = activity.getPackageManager();
                ComponentName componentName = takePictureIntent.resolveActivity(packageManager);
                if (componentName != null || packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    try {
                        // 创建临时图片文件
                        File photoFile = createImageFile();
                        // 使用FileProvider生成安全的内容URI
                        capturedImageURI = FileProvider.getUriForFile(
                                activity,
                                activity.getPackageName() + ".provider",
                                photoFile
                        );
                        // 添加URI读写权限
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageURI);
                        activity.startActivityForResult(takePictureIntent, TAKE_PHOTO_CHOOSER_REQUEST_CODE);

                    } catch (IOException ex) {
                        // 若创建临时文件时发生 IO 异常，打印异常堆栈信息
                        ex.printStackTrace();
                    }
                }
            } else {
                // 请求缺失的权限（使用 ReqPageCodeConst 中定义的相机权限请求码）
                CommonPermissionUtils.requestCameraPermission(activity);
            }
        });

    }


    // 将图片路径返回给JS
    public void returnImagePath(String base64Image) {
        String jsCode = "javascript:androidFun.onTakePhoto('" + base64Image + "')";
        webView.evaluateJavascript(jsCode, null);
    }

    // 将图片路径返回给JS
    public void returnFilePath(String fileName, String base64Str) {
        String jsCode = "javascript:androidFun.onFileSelected('" + fileName + "', '" + base64Str + "')";
        webView.evaluateJavascript(jsCode, null);
    }


    // 创建临时图片文件
    private File createImageFile() throws IOException {
        String timeStamp = YYYYMMDD_HHMMSS.format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir("temp_pictures");
        if (storageDir != null && !storageDir.exists()) {
            // 创建目录（包括父目录）
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }


    public Uri getCapturedImageURI() {
        return capturedImageURI;
    }


    public void returnScanCode(String barcodeResult) {
        // 统一走JSON转义，避免条码中的单引号/换行破坏JS语法
        callJs("onScanResult", barcodeResult == null ? "" : barcodeResult);
    }

    // JS调用此方法触发文件选择
    @JavascriptInterface
    public void chooseFile(String type) {
        // 检查权限
        if (!CommonPermissionUtils.checkStoragePermissions(activity)) {
            CommonPermissionUtils.requestStoragePermissions(activity);
            return;
        }

        // 触发原生文件选择
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // 允许所有文件类型
        if(StringUtils.isNullOrEmpty(type)){
            type = "*/*";
        }
        intent.setType(type);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, ReqPageCodeConst.FILE_CHOOSER_REQUEST_CODE);
    }

    // 用于接收文件选择结果
    public void onFileSelected(Uri uri) {
        if (uri != null) {
            // 返回文件URI给H5
            String jsCode = "javascript:androidFun.onFileSelected('" + uri.toString() + "')";
            webView.evaluateJavascript(jsCode, null);
        } else {
            String jsCode = "javascript:androidFun.onFileSelected('')";
            webView.evaluateJavascript(jsCode, null);
        }
    }

    /**
     * H5页面调用，进行文件上传
     *
     * @param tempFileUri
     * @param uploadUrl
     * @param fileName
     * @param id
     */
    @JavascriptInterface
    public void uploadFile(String tempFileUri, String uploadUrl, String fileName, String id, String callBackName) {
        // 将临时文件URI转换为文件对象
        UploadFileByWebTask task = new UploadFileByWebTask(activity, tempFileUri, uploadUrl, id);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            String data = result.getData();
            // TODO 调用js方法，进行回调通知

        });
    }

    /**
     * 跳转至拨号界面，并填充电话号码
     * @param phone
     */
    @JavascriptInterface
    public void callPhone(String phone){
        if (phone != null && !phone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            activity.startActivity(intent);
        }
    }




  /*  // 设置MainActivity回调监听器
    public void setOnFragmentSwitchListener(MainPagerAdapter mainPagerAdapter) {
        this.mainPagerAdapter = mainPagerAdapter;
    }

    // 在 WebView 的 JavaScript 接口中调用
    @JavascriptInterface
    public void switchToHomeFragment() {
        if (mainPagerAdapter != null) {
            mainPagerAdapter.changeWebHomeFragment(false);
        }
    }*/

}
