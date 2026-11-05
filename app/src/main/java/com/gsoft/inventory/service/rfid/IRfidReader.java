package com.gsoft.inventory.service.rfid;

import android.content.Context;

/**
 * RFID读取器统一接口：业务层只依赖本接口，不感知具体厂家SDK
 * <p>
 * 各厂家适配器实现连接/扫描/参数下发，并把SDK差异（线程模型/参数体系/错误码/按键集合）
 * 封闭在适配器内；新增厂家只需实现本接口并在RfidReaderFactory注册
 */
public interface IRfidReader {

    /** 厂家名称（日志与提示用） */
    String getVendorName();

    /** 是否支持当前设备（轻量探测，不建立连接） */
    boolean isSupported(Context context);

    /** 连接前是否需要存储权限（得实SDK需读校验文件） */
    boolean requiresStoragePermission();

    /** 连接前是否需要定位权限（优博讯BLE枪柄首次搜索需要，targetSdk=30走legacy权限模型） */
    boolean requiresLocationPermission();

    /** 是否支持EPC+TID盘点区域（不支持时设置页隐藏该项） */
    boolean supportsTid();

    /** 功率可设范围 [min,max]，UI输入校验用 */
    int[] getPowerRange();

    /** 默认功率（用户从未手动设置过时使用）：优博讯按文档取设备支持的最大功率，其他厂家33 */
    int getDefaultPower();

    /** 是否连接就绪（可开始扫描） */
    boolean isReady();

    /** 是否正在连接中 */
    boolean isConnecting();

    /** 连接模块：结果经callbacks.onConnectResult异步回调（主线程） */
    void connect(Context context, RfidCallbacks callbacks);

    /** 断开并释放模块（页面销毁时调用） */
    void disconnect();

    /** 开始连续盘点 */
    void startScan();

    /** 停止连续盘点 */
    void stopScan();

    /** 应用参数配置（功率/盘点区域/会话/频率区域），适配器内部缓存供休眠唤醒后重放 */
    void applyConfig(RfidConfig config);

    /** 页面恢复：东集模块休眠断电后需重新open；得实连接失败(1~3次)延迟自动重连 */
    void onPageResume();

    /** 页面暂停：东集需close防止系统休眠后模块状态错乱；得实无需处理 */
    void onPagePause();

    /** 判断是否设备RFID硬件触发键（各机型按键集合不同，由适配器各自维护） */
    boolean isTriggerKey(int keyCode, int scanCode);
}