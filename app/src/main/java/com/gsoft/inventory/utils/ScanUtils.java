package com.gsoft.inventory.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import com.gsoft.inventory.service.asset_code_scan.ScanCodeResultHandle;
import com.journeyapps.barcodescanner.ScanOptions;
import com.rscja.barcode.BarcodeDecoder;
import com.rscja.barcode.BarcodeFactory;
import com.rscja.deviceapi.entity.BarcodeEntity;
import permissions.dispatcher.NeedsPermission;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

/**
 * @author 10904
 */
public class ScanUtils {
    private final BarcodeDecoder barcodeDecoder = BarcodeFactory.getInstance().getBarcodeDecoder();

    private Activity activity = null;
    private Handler scanCodeResultHandler = null;

    public ScanUtils(Activity activity) {
        this.activity = activity;
        this.scanCodeResultHandler = new ScanCodeResultHandle(activity);
        new InitOpenAsync().execute();
    }


    public void onDestroy() {
        closeScanDevice();
    }

    private void closeScanDevice() {
        if (barcodeDecoder != null) {
            barcodeDecoder.close();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 139 && event.getRepeatCount() == 0) {
            start();
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 139 && event.getRepeatCount() == 0) {
            stop();
            return true;
        }
        return false;
    }


    /**
     * 打开红外线开始扫码
     */
    private void start() {
        barcodeDecoder.startScan();
    }

    /**
     * 关闭红外线
     */
    private void stop() {
        barcodeDecoder.stopScan();
    }

    private void open() {
        if (activity != null) {
            barcodeDecoder.open(activity);
            // 手持设备扫码回调
            barcodeDecoder.setDecodeCallback(barcodeEntity -> {
                if (barcodeEntity.getResultCode() == BarcodeDecoder.DECODE_SUCCESS) {
                    handleRedScanResult(barcodeEntity);
                }
            });
        }
    }

    /**
     * 处理红外线扫码结果
     *
     * @param barcodeEntity
     */
    private void handleRedScanResult(BarcodeEntity barcodeEntity) {
        Message msgScanResult = new Message();
        String barCode = barcodeEntity.getBarcodeData();
        byte[] barcodeBytesData = barcodeEntity.getBarcodeBytesData();
        if (barCode.toUpperCase().startsWith("HTTP") && barCode.toUpperCase().contains("BARCODE")) {
            onScanResult(barCode);
        } else if (barCode.length() > 25) {
            if (!barCode.contains("条码编号：")) {
                // 过滤掉无法解码的字节
                byte[] filteredBytes = filterInvalidUtf8Bytes(barcodeBytesData);
                String utf8String = new String(filteredBytes, StandardCharsets.UTF_8);
                String[] parts = utf8String.split("\n");
                if (parts.length > 0) {
                    barCode = parts[0];
                }
            }
            onScanResult(barCode);
        } else {
            showShortText(barCode);
            msgScanResult.arg1 = 1;
            msgScanResult.obj = barCode;
            scanCodeResultHandler.sendMessage(msgScanResult);
        }
    }

    public static byte[] filterInvalidUtf8Bytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 0) {
                sb.append((char) b);
            }
        }
        return sb.toString().getBytes();
    }


    /**
     * 处理最终的barCode结果
     *
     * @param barCode
     */
    public void onScanResult(String barCode) {
        Message msgScanResult = new Message();
        try {
            if (barCode.toUpperCase().startsWith("HTTP")) {
                if (barCode.toUpperCase().contains("BARCODE")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BARCODE") + 8);
                } else if (barCode.toUpperCase().contains("BID")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BID") + 4);
                }
            } else if (barCode.length() > 25 && barCode.contains("条码编号：")) {
                barCode = barCode.substring(5, barCode.indexOf("\n"));
            }
            showShortText(barCode);
            msgScanResult.arg1 = 1;
            msgScanResult.obj = barCode;

        } catch (Exception ex) {
            msgScanResult.arg1 = 0;
            msgScanResult.obj = ex.getMessage();
        }
        scanCodeResultHandler.sendMessage(msgScanResult);
    }

    private void showShortText(String text) {
        if (activity != null) {
            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
        }
    }


    public class InitOpenAsync extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mypDialog = new ProgressDialog(activity);
            mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mypDialog.setMessage("初始化扫码中请稍后...");
            mypDialog.setCanceledOnTouchOutside(false);
            mypDialog.setCancelable(false);
            mypDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            open();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mypDialog.cancel();
        }


    }


}
