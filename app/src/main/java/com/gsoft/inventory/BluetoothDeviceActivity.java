package com.gsoft.inventory;

import android.Manifest;
import androidx.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.dothantech.lpapi.LPAPI;
import com.dothantech.printer.IDzPrinter.PrinterAddress;
import com.gsoft.inventory.adapter.PrinterAddressAdapter;
import com.gsoft.inventory.entities.JCPrinter;
import com.gsoft.inventory.utils.BluetoothDeviceDetail;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.SecurityUtils;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;

import java.util.List;

public class BluetoothDeviceActivity extends PrintCompatActivity {
    private static final int REQUEST_CODE1 = 1001;
    private static final int RESULT_CODE1 = 1002;

    private QMUIPullRefreshLayout mPullRefreshLayout;
    private ListView bt_list;
    private PrinterAddressAdapter bluetoothListAdapter;
    QMUITopBar topBar;
    private int theSameCount = 0;
    int transAssetsIndex = 0;
    PrinterAddress printerAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_device);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("连接蓝牙");
        transAssetsIndex = getIntent().getIntExtra(SysDefine.IntentExtra_ASSETS_ARRAY_INDEX, -1);
        if (StringUtils.isNullOrEmpty(application.getPrinterDevice())) {
            showShortText("登录超时，请重新登录并选择打印机");
            return;
        }
        mPullRefreshLayout = (QMUIPullRefreshLayout) findViewById(R.id.pull_to_refresh);
        bt_list = (ListView) findViewById(R.id.bt_list);
        List<PrinterAddress> pairedPrinters = application.getDTPrinter().getAllPrinterAddresses(null);
        bluetoothListAdapter = new PrinterAddressAdapter(mContext, pairedPrinters);
        bt_list.setAdapter(bluetoothListAdapter);
        mPullRefreshLayout.setOnPullListener(new QMUIPullRefreshLayout.OnPullListener() {
            @Override
            public void onMoveTarget(int offset) {

            }

            @Override
            public void onMoveRefreshView(int offset) {

            }

            @Override
            public void onRefresh() {
                mPullRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        share.search();
                        mPullRefreshLayout.finishRefresh();
                    }
                }, 200);
            }
        });
        requestPermission();
        turnOnBluetooth();
        /*IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);*/
        bt_list.setOnItemClickListener((parent, view, position, id) -> {
            //share.stopSearch();
            printerAddress = pairedPrinters.get(position);
            application.getDTPrinter().openPrinterByAddress(printerAddress);
        });

        application.getPrinterStateLiveData().observe(this, state -> {
            if (state == null) {
                showShortText("打印机未连接");
            } else if (state == 2 || state == 1) {
                showShortText("打印机已连接");
                onConnectedPrinter();
            } else if (state == 0 || state == 5) {
                showShortText("打印机已断开");
            }
        });
    }

    private void onConnectedPrinter() {
        if (printerAddress == null) return;
        String[] names = printerAddress.shownName.split("-");
        String printerName = names[names.length - 1];
        List<JCPrinter> findArray = JCPrinter.find(JCPrinter.class, "encryno=?", SecurityUtils.md5(printerName).toUpperCase());
        if (findArray == null || findArray.size() == 0) {
            application.getDTPrinter().closePrinter();
            showShortText("打印条码驱动失败，请退出重试！");
            return;
        }


        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINTER_MAC, printerAddress.macAddress);
        SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_PRINTER_NAME, printerAddress.shownName);
        SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_PRINTER_TYPE, printerAddress.addressType.value());

        if (transAssetsIndex > -1) {
            Intent dataIntent = new Intent();
            dataIntent.putExtra(SysDefine.IntentExtra_ASSETS_ARRAY_INDEX, transAssetsIndex);
            setResult(RESULT_OK, dataIntent);
        } else {
            setResult(RESULT_OK);
        }
        finish();
    }

    // 打印文本一维码
    private boolean printText1DBarcode(String text, String onedBarcde, Bundle param) {

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 开始一个页面的绘制，绘制文本字符串
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().drawText("贵阳市南明区公安分局", 0, 4, 70, 5, 3.5);
        // 设置之后绘制的对象内容旋转180度
        //application.getDTPrinter().setItemOrientation(90);
        // 绘制一维码，此一维码绘制时内容会旋转180度，
        // 传入参数(需要绘制的一维码的数据, 绘制的一维码左上角水平位置, 绘制的一维码左上角垂直位置, 绘制的一维码水平宽度, 绘制的一维码垂直高度)
        application.getDTPrinter().draw1DBarcode(onedBarcde, LPAPI.BarcodeType.CODE128, 15, 9, 40, 10, 3);

        application.getDTPrinter().drawText(String.format("资产名称：%1$s  使用人：%2$s", "便携式打印机", "张露露"), 0, 20, 70, 5, 3);
        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }

    //请求权限
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int check = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (check != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            setResult(RESULT_CODE1);
        }
    }

    private void turnOnBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }
    }

    private void deviceFound(final android.bluetooth.BluetoothDevice device, final int rssi) {
        if (share == null || share.devices == null || bluetoothListAdapter == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showShortText("登录超时，请重启APP！");
                }
            });
            return;
        }
        if (share.devices.size() > 0) {
            if (share.devices_addrs.contains(device.getAddress())) {
                theSameCount++;
                if (theSameCount > 30) {
                    theSameCount = 0;
                    final int index = share.devices_addrs.indexOf(device.getAddress());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            share.devices.get(index).setDetail(device.getName(), device.getAddress(), rssi);
                            bluetoothListAdapter.notifyDataSetChanged();
                        }
                    });
                }
                return;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                share.addDevice(new BluetoothDeviceDetail(device.getName(), device.getAddress(), rssi));
                bluetoothListAdapter.notifyDataSetChanged();
//                nAdapter.notifyDataSetChanged();
            }
        });
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (android.bluetooth.BluetoothDevice.ACTION_FOUND.equals(action)) {
                final android.bluetooth.BluetoothDevice deviceGet = intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                Log.i("找到设备", deviceGet.getAddress());
                //信号强度。
                final int rssi = intent.getExtras().getShort(android.bluetooth.BluetoothDevice.EXTRA_RSSI);
                try {
                    deviceFound(deviceGet, rssi);
                } catch (Exception ex) {
                    showShortText("搜索失败，" + ex.getMessage());
                } catch (UnknownError unex) {
                    showShortText("搜索失败，" + unex.getMessage());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i("查找结束", "共有:" + share.devices.size());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i("查找中", "正在查找设备");
            } else if (android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final android.bluetooth.BluetoothDevice deviceGet = intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                switch (deviceGet.getBondState()) {
                    case android.bluetooth.BluetoothDevice.BOND_BONDING://正在配对
                        Log.d("BlueToothTestActivity", "正在配对......");
                        // searchDevices.setText("正在配对......");
                        break;
                    case android.bluetooth.BluetoothDevice.BOND_BONDED://配对结束
                        Log.d("BlueToothTestActivity", "完成配对");
                        break;
                    case android.bluetooth.BluetoothDevice.BOND_NONE://取消配对/未配对
                        Log.d("BlueToothTestActivity", "取消配对");
                    default:
                        break;
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // 注册广播接收器，接收并处理搜索结果
        registerReceiver(receiver, share.getIntentFilter());
        /**
         * 设置搜索模式
         * share.model_default() 获取一般搜索模式字符串, 只有SPP或BLE
         */
        //tv_search_model.setText(share.model_default());
        //btn_search.performClick();
        share.search();
    }

    @Override
    protected void onPause() {
        super.onPause();
        share.stopSearch();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
        }
    }
}
