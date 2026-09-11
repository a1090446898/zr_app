package com.gsoft.inventory.service.rfid;

/**
 * 统一RFID回调：适配器保证全部回调投递到主线程
 */
public interface RfidCallbacks {

    /** 连接结果（error=SUCCESS表示就绪，失败携带统一错误码与文案） */
    void onConnectResult(RfidError error);

    /** 扫到标签（逐条） */
    void onTagScanned(RfidTagData tag);

    /** 运行期硬件错误（如启动盘点失败） */
    void onHardwareError(RfidError error);
}