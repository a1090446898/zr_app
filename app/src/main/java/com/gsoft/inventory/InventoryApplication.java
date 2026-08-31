package com.gsoft.inventory;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;
import androidx.lifecycle.MutableLiveData;
import androidx.multidex.MultiDex;
import com.dothantech.lpapi.LPAPI;
import com.dothantech.printer.IDzPrinter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.JCPrinter;
import com.gsoft.inventory.update.OkHttpUtilsUpdateService;
import com.gsoft.inventory.utils.*;
import com.orm.SugarContext;
import com.postek.cdf.CDFPTKAndroid;
import com.postek.cdf.CDFPTKAndroidImpl;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.entity.UpdateError;
import com.xuexiang.xupdate.listener.OnUpdateFailureListener;
import com.xuexiang.xupdate.utils.UpdateUtils;

import java.util.List;

import static com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NEW_VERSION;

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
     * 设备号码1~Z
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


