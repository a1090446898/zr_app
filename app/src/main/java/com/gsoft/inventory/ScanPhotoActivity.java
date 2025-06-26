package com.gsoft.inventory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.gsoft.inventory.utils.BaseCompatActivity;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 10904
 * 用于手机扫码
 */
public class ScanPhotoActivity extends BaseCompatActivity {

    // 谷歌扫码配置
    private static final String TAG = "QRCodeScanner";
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int ASPECT_RATIO = AspectRatio.RATIO_4_3;
    private static final int FLASH_MODE_ON = 1;
    private static final int FLASH_MODE_OFF = 0;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    private PreviewView previewView;
    private Button flashlightBtn;
    private TextView resultTextView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private Camera camera;
    private CameraControl cameraControl;

    private ProcessCameraProvider cameraProvider;
    private AtomicBoolean isFlashOn = new AtomicBoolean(true); // 默认开启闪光灯
    private AtomicBoolean isScanning = new AtomicBoolean(true);
    private Handler mainHandler;
    private Handler timerHandler;
    private Runnable zoomRunnable;
    // 2秒没扫到码就放大
    private static final int ZOOM_DELAY = 2000;
    // 放大倍数
    private static final float ZOOM_FACTOR = 5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_scan_priview);
        this.initializeView();
    }

    @Override
    public void initializeView() {
        // 初始化视图
        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);
        flashlightBtn = findViewById(R.id.flashlightBtn);

        // 闪光灯按钮点击事件
        flashlightBtn.setOnClickListener(v -> toggleFlashlight());

        // 检查相机权限
        if (hasCameraPermission()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSION);
        }

        // 创建相机执行器和条码扫描器
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 配置扫描选项（仅识别QR码，提高性能）
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E
                )
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // 初始化闪光灯按钮文本
        flashlightBtn.setText("⚡ 关闭");

        mainHandler = new Handler(Looper.getMainLooper());
        timerHandler = new Handler(Looper.getMainLooper());
        zoomRunnable = () -> {
            if (camera != null) {
                float currentZoom = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                float newZoom = currentZoom * ZOOM_FACTOR;
                cameraControl.setZoomRatio(newZoom);
            }
        };
    }

    // 检查相机权限
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "相机权限被拒绝，无法使用扫码功能", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // 启动相机
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // 配置预览用例
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 配置图像分析用例
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        // 降低分辨率提高性能
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // 设置图像分析器
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                // 选择后置摄像头
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // 绑定所有用例到生命周期
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        (LifecycleOwner) this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                cameraControl = camera.getCameraControl();

                // 启用自动对焦
                if (camera.getCameraInfo().hasFlashUnit()) {
                    cameraControl.enableTorch(true); // 默认开启闪光灯
                }

                // 开始计时
                timerHandler.postDelayed(zoomRunnable, ZOOM_DELAY);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "启动相机失败", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // 分析图像中的条码
    private void analyzeImage(ImageProxy imageProxy) {
        if (imageProxy == null) return;

        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes.length == 0) {
            imageProxy.close();
            return;
        }

        // 从ImageProxy创建InputImage
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        @SuppressLint("UnsafeOptInUsageError")
        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), rotationDegrees);

        // 处理条码识别
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        // 取消计时
                        timerHandler.removeCallbacks(zoomRunnable);
                        // 获取第一个识别到的条码
                        Barcode barcode = barcodes.get(0);
                        processBarcodeResult(barcode);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "条码识别失败", e);
                })
                .addOnCompleteListener(task -> {
                    // 无论成功与否，都必须关闭ImageProxy
                    imageProxy.close();
                });
    }

    // 处理识别结果
    private void processBarcodeResult(Barcode barcode) {
        // 在主线程更新UI
        runOnUiThread(() -> {
            String rawValue = barcode.getRawValue();
            if (rawValue != null) {
                resultTextView.setText("扫描结果: " + rawValue);

                // 可选：识别到后暂停扫描一段时间
                try {
                    Thread.sleep(200); // 暂停1秒，避免连续触发
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void toggleFlashlight() {
        if (cameraControl == null) {
            Toast.makeText(this, "相机未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查设备是否支持闪光灯
        if (!camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "设备不支持闪光灯", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newFlashState = !isFlashOn.get();
        isFlashOn.set(newFlashState);

        int newFlashMode = newFlashState ? FLASH_MODE_ON : FLASH_MODE_OFF;
        String flashText = newFlashState ? "⚡ 关闭" : "⚡ 闪光灯";

        // 更新按钮文本
        flashlightBtn.setText(flashText);

        // 设置闪光灯模式
        cameraControl.enableTorch(newFlashState);

        Toast.makeText(this, newFlashState ? "闪光灯已开启" : "闪光灯已关闭", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 释放资源
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        // 关闭线程池
        if(cameraExecutor != null){
            cameraExecutor.shutdown();
        }
        barcodeScanner.close();
        timerHandler.removeCallbacks(zoomRunnable);
    }
}