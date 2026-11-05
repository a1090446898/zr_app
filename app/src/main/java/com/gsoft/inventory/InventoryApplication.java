package com.gsoft.inventory;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import androidx.multidex.MultiDex;
import com.dothantech.lpapi.LPAPI;
import com.dothantech.printer.IDzPrinter;
import com.google.gson.Gson;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;
import com.orm.SugarContext;
import com.postek.cdf.CDFPTKAndroid;
import com.postek.cdf.CDFPTKAndroidImpl;

/**
 * @author 10904
 */
public class InventoryApplication extends com.orm.SugarApp {


    public MyActivityLifecycleCallbacks lifecycleCallbacks;
    private static Application application = null;

    private Accounts _loginAccount = null;
    private Gson gson = new Gson();

    private String zcqcyName = "";

    private String zcqcyCode = "";

    /**
     * 照片上传提示标志：本次登录后第一次拍照提示一次，避免每拍一张都弹框
     */
    private boolean photoUploadTipShown = false;
    /**
     * 设备号：首次进入APP时随机生成并持久化（KEY_DEVICENO），之后保持不变，可在登录设置页修改
     */
    private static String _deviceNo = "";
    /**
     * 打印设备型号
     */
    private static String printerDevice = "智软2号";

    private static CDFPTKAndroid cdf = null;

    private static String printerEffect = "大条码";

    private LPAPI dtPrinterApi;

    public CDFPTKAndroid getJCPrinter() {
        if (cdf == null) {
            initBSDPrinter();
        }
        return cdf;
    }


    public Accounts getLoginAccount() {
        return _loginAccount;
    }

    public void setLoginAccount(Accounts loginAccount) {
        _loginAccount = loginAccount;
        // 每次登录（在线/离线登录都会走这里）重置提示标志，下次登录后第一次拍照会再次提示
        photoUploadTipShown = false;
    }

    /**
     * 本次登录后是否还需要提示“照片待上传”
     *
     * @return true 表示本次登录还没提示过，需要弹框
     */
    public boolean needShowPhotoUploadTip() {
        return !photoUploadTipShown;
    }

    /**
     * 标记本次登录已提示过照片上传，后续拍照不再重复弹框
     */
    public void markPhotoUploadTipShown() {
        photoUploadTipShown = true;
    }

    public String getPrinterDevice() {
        return printerDevice;
    }

    public void setPrinterDevice(String deviceNo) {
        printerDevice = deviceNo;
    }


    public String getDeviceNo() {
        return _deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        _deviceNo = deviceNo;
    }

    public String getZcqcyName() {
        return zcqcyName;
    }

    public void setZcqcyName(String zcqcyName) {
        this.zcqcyName = zcqcyName;
    }


    public String getZcqcyCode() {
        return zcqcyCode;
    }

    public void setZcqcyCode(String zcqcyCode) {
        this.zcqcyCode = zcqcyCode;
    }

    public void setPrinterEffect(String printeEffect) {
        printerEffect = printeEffect;
    }

    public String getPrinterEffect() {
        return printerEffect;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
        application = this;
        // 设备号只在首次进入APP时随机生成并保存，后续启动保持不变（可在登录设置页修改）
        String savedDeviceNo = SharedPreferencesUtils.getString(this, SharedPreferencesUtils.KEY_DEVICENO, "");
        if (StringUtils.isNullOrEmpty(savedDeviceNo)) {
            savedDeviceNo = CommonUtils.getRandomTwoString();
            SharedPreferencesUtils.putString(this, SharedPreferencesUtils.KEY_DEVICENO, savedDeviceNo);
        }
        _deviceNo = savedDeviceNo;
        SysConfig.checkSoftStorage();
        SysConfig.assetsFieldsArray = getResources().getStringArray(R.array.assets_fields);
        SugarContext.init(this);
        //lifecycleCallbacks = new MyActivityLifecycleCallbacks();
        //application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        //AppCrashHandler crashHandler = AppCrashHandler.instance();
        //crashHandler.init(getApplicationContext(), lifecycleCallbacks);
        printerStateLiveData = new MutableLiveData<>();

    }

    @Override
    public void onTerminate() {
        SugarContext.terminate();
        super.onTerminate();
        if (dtPrinterApi != null) {
            dtPrinterApi.quit();
            dtPrinterApi = null;
        }
    }

    public MutableLiveData<Integer> getPrinterStateLiveData() {
        return printerStateLiveData;
    }

    private MutableLiveData<Integer> printerStateLiveData;

    public LPAPI getDTPrinter() {
        if (dtPrinterApi == null) {
            dtPrinterApi = LPAPI.Factory.createInstance(mDTPrinterCallback);
        }
        return dtPrinterApi;
    }

    private final LPAPI.Callback mDTPrinterCallback = new LPAPI.Callback() {
        @Override
        public void onStateChange(IDzPrinter.PrinterAddress arg0, IDzPrinter.PrinterState arg1) {
            Log.e("PRINTER", arg1.name());
            //final IDzPrinter.PrinterAddress printer = arg0;
            printerStateLiveData.postValue(arg1.ordinal());
            switch (arg1) {
                case Connected:
                case Connected2:
                    // 打印机连接成功，发送通知，刷新界面提示
                    break;
                case Disconnected:
                    // 打印机连接失败、断开连接，发送通知，刷新界面提示
                    break;
                default:
                    break;
            }
        }

        // 蓝牙适配器状态发生变化时被调用
        @Override
        public void onProgressInfo(IDzPrinter.ProgressInfo arg0, Object arg1) {
        }

        @Override
        public void onPrinterDiscovery(IDzPrinter.PrinterAddress arg0, IDzPrinter.PrinterInfo arg1) {
        }

        // 打印标签的进度发生变化是被调用
        @Override
        public void onPrintProgress(IDzPrinter.PrinterAddress address, Object bitmapData, IDzPrinter.PrintProgress progress, Object addiInfo) {
            switch (progress) {
                case Success:
                    // 打印标签成功，发送通知，刷新界面提示
                    break;
                case Failed:
                    // 打印标签失败，发送通知，刷新界面提示
                    break;
                default:
                    break;
            }
        }
    };

    public void initBSDPrinter() {
        if (cdf == null)
            cdf = new CDFPTKAndroidImpl(this, null);
    }



    public static Application getApplication() {
        return application;
    }




}


