package com.gsoft.inventory.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.core.content.FileProvider;
import com.gsoft.inventory.BuildConfig;
import com.gsoft.inventory.InventoryApplication;
import com.gsoft.inventory.LoginActivity;
import com.gsoft.inventory.abs.WebFragment;
import com.gsoft.inventory.const_data.ReqPageCodeConst;
import com.gsoft.inventory.service.android_by_js.UploadFileByWebTask;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.view.home.MainPagerAdapter;
import com.gsoft.inventory.view.pdf_view.PdfViewerActivity;
import com.gsoft.inventory.view.scan.BarcodeScannerActivity;
import com.xuexiang.xupdate.utils.UpdateUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

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
            // 将 JSONObject 转换成 JSON 字符串
            return userInfoJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
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
        String jsCode = "javascript:androidFun.onScanResult('" + barcodeResult + "')";
        webView.evaluateJavascript(jsCode, null);
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
