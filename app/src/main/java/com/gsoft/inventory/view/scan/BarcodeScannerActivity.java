package com.gsoft.inventory.view.scan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.CommonPermissionUtils;
import com.gsoft.inventory.utils.ScanDeviceUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.rscja.barcode.BarcodeDecoder;
import com.rscja.barcode.BarcodeFactory;
import com.rscja.barcode.BarcodeUtility;

import static com.gsoft.inventory.const_data.ReqPageCodeConst.REQUEST_HUAWEI_CODE_SCAN;

public class BarcodeScannerActivity extends AppCompatActivity {

    // ==================== 新设备广播扫码 ====================
    private static final String SCAN_ACTION = "com.android.server.scannerservice.broadcast";
    private BroadcastReceiver scanReceiver;
    private boolean isScanReceiverRegistered = false;

    // ==================== 旧设备硬件扫码 ====================
    private BarcodeDecoder barcodeDecoder;
    private boolean isOldDeviceInitialized = false;

    // ==================== 通用 ====================
    private Button btnSwitchMode;
    private MediaPlayer mediaPlayer;
    public HmsScanAnalyzerOptions options;
    private boolean isNewDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        isNewDevice = ScanDeviceUtils.isNewDevice();

        initHuaweiScan();

        if (isNewDevice) {
            initBroadcastScan();
        } else {
            initHoldScan();
        }

        CommonPermissionUtils.requestCameraPermission(this);
        CommonPermissionUtils.requestReadFilePermission(this);
    }

    private void initHuaweiScan() {
        options = new HmsScanAnalyzerOptions.Creator()
                .setHmsScanTypes(HmsScan.ALL_SCAN_TYPE)
                .setViewType(0)
                .setErrorCheck(true).create();

        btnSwitchMode = findViewById(R.id.btn_switch_mode);
        btnSwitchMode.setOnClickListener(e -> toHuaweiScan());
    }

    private void toHuaweiScan() {
        boolean hasPermission = CommonPermissionUtils.requestCameraPermission(this);
        if (hasPermission) {
            ScanUtil.startScan(this, REQUEST_HUAWEI_CODE_SCAN, options);
        }
    }

    private void initHoldScan() {
        barcodeDecoder = BarcodeFactory.getInstance().getBarcodeDecoder();

        BarcodeUtility.getInstance().enablePlaySuccessSound(this, true);
        BarcodeUtility.getInstance().setContinuousScanIntervalTime(this, 100);

        barcodeDecoder.open(this);
        isOldDeviceInitialized = true;

        barcodeDecoder.setDecodeCallback(barcodeEntity -> {
            if (barcodeEntity.getResultCode() == BarcodeDecoder.DECODE_SUCCESS) {
                String result = barcodeEntity.getBarcodeData();
                handleBackResult(result);
            }
        });
    }

    private void initBroadcastScan() {
        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SCAN_ACTION.equals(intent.getAction())) {
                    String code = intent.getStringExtra("scannerdata");
                    if (!StringUtils.isNullOrEmpty(code)) {
                        // 去掉数据末尾的换行符
                        code = code.replace("\n", "").replace("\r", "");
                        playScanSound();
                        handleBackResult(code);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNewDevice && scanReceiver != null && !isScanReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SCAN_ACTION);
            intentFilter.setPriority(Integer.MAX_VALUE);
            registerReceiver(scanReceiver, intentFilter);
            isScanReceiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isNewDevice && isScanReceiverRegistered) {
            try {
                unregisterReceiver(scanReceiver);
            } catch (IllegalArgumentException e) {
                Log.e("BarcodeScannerActivity", "注销广播接收器时出错", e);
            }
            isScanReceiverRegistered = false;
        }
    }

    private void playScanSound() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.scan_sound);
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("BarcodeScannerActivity", "播放音频失败", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HUAWEI_CODE_SCAN && data != null) {
            playScanSound();

            int errorCode = data.getIntExtra(ScanUtil.RESULT_CODE, ScanUtil.SUCCESS);
            if (errorCode == ScanUtil.SUCCESS) {
                HmsScan resultObj = data.getParcelableExtra(ScanUtil.RESULT);
                if (resultObj != null) {
                    String result = resultObj.getOriginalValue();
                    handleBackResult(result);
                }
            }
        }
    }

    private void handleBackResult(String result) {
        if (!StringUtils.isNullOrEmpty(result)) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("barcode_result", result);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isNewDevice && barcodeDecoder != null && keyCode == 139 && event.getRepeatCount() == 0) {
            barcodeDecoder.startScan();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isNewDevice && barcodeDecoder != null && keyCode == 139 && event.getRepeatCount() == 0) {
            barcodeDecoder.stopScan();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isOldDeviceInitialized && barcodeDecoder != null) {
            try {
                barcodeDecoder.close();
            } catch (IllegalArgumentException e) {
                Log.e("BarcodeScannerActivity", "关闭扫码器时出错", e);
            }
            isOldDeviceInitialized = false;
        }

        if (isNewDevice && isScanReceiverRegistered) {
            try {
                unregisterReceiver(scanReceiver);
            } catch (IllegalArgumentException e) {
                Log.e("BarcodeScannerActivity", "注销广播接收器时出错", e);
            }
            isScanReceiverRegistered = false;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
