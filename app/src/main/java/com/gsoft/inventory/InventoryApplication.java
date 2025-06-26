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


    private static CDFPTKAndroid cdf = null;

    public CDFPTKAndroid getJCPrinter() {
        if (cdf == null) {
            initBSDPrinter();
        }
        return cdf;
    }

    private static Application application = null;
    public MyActivityLifecycleCallbacks lifecycleCallbacks;

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

    private Accounts _loginAccount = null;
    private Gson gson = new Gson();

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

    private String zcqcyName = "";

    public String getZcqcyCode() {
        return zcqcyCode;
    }

    public void setZcqcyCode(String zcqcyCode) {
        this.zcqcyCode = zcqcyCode;
    }

    private String zcqcyCode = "";
    /**
     * 设备号码1~Z
     */
    private static String _deviceNo = "";
    /**
     * 打印设备型号
     */
    private static String printerDevice = "智软2号";

    public void setPrinterEffect(String printeEffect) {
        printerEffect = printeEffect;
    }

    public String getPrinterEffect() {
        return printerEffect;
    }

    private static String printerEffect = "大条码";

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
        initUpdateService();
        refreshPrinters();
        printerStateLiveData = new MutableLiveData<>();
    }

    private LPAPI dtPrinterApi;


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

    public void refreshPrinters() {
        OkHttpUtils.getInstance().get(SysDefine.SERVER_PRINTERS_SYNC(), new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                JCPrinter.deleteAll(JCPrinter.class);
                List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {
                }.getType());
                if(results == null || results.size() == 0){
                    return;
                }
                for (String dataline : results) {
                    if (!dataline.contains("ERROR")) {
                        DataTransmitHelper.insertPrinters(dataline);
                    }
                }
            }

            @Override
            public void onResponseFailure(String errMessage) {
            }
        });
    }

    public static Application getApplication() {
        return application;
    }

    private void initUpdateService() {
        XUpdate.get()
                .debug(true)
                //默认设置只在wifi下检查版本更新
                .isWifiOnly(false)
                //默认设置使用get请求检查版本
                .isGet(true)
                //默认设置非自动模式，可根据具体使用配置
                .isAutoMode(false)
                //设置默认公共请求参数
                .param("versionCode", UpdateUtils.getVersionCode(this))
                .param("appKey", getPackageName())
                //设置版本更新出错的监听
                .setOnUpdateFailureListener(new OnUpdateFailureListener() {
                    @Override
                    public void onFailure(UpdateError error) {
                        //对不同错误进行处理
                        if (error.getCode() != CHECK_NO_NEW_VERSION) {
                            Toast.makeText(InventoryApplication.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                //设置是否支持静默安装，默认是true
                .supportSilentInstall(true)
                //这个必须设置！实现网络请求功能。
                .setIUpdateHttpService(new OkHttpUtilsUpdateService())
                .init(this);
    }


}


