package com.gsoft.inventory.service.rfid;

/**
 * 统一RFID错误码：各厂家SDK错误码映射后的通用语义，UI文案由此枚举统一输出
 * @author 10904
 */
public enum RfidError {

    SUCCESS(""),
    SDK_VERIFY_FAILED("RFID连接失败：SDK校验失败"),
    FILE_INVALID("RFID连接失败：校验文件不正确，请更新应用"),
    STORAGE_PERMISSION("RFID连接失败：存储权限未授予，请在系统设置中开启「所有文件访问」"),
    PORT_CONFLICT("RFID连接失败：模块端口冲突或异常，请重启应用"),
    NOT_IN_SUPPORT_LIST("RFID连接失败：当前设备不支持RFID盘点"),
    MODEL_NOT_SUPPORTED("RFID连接失败：当前设备不是RFID指定机型"),
    DEVICE_NOT_SUPPORTED("当前设备不支持RFID模块"),
    OPEN_FAILED("RFID模块打开失败，请重启应用重试"),
    START_FAILED("启动盘点失败"),
    CONNECT_FAILED("RFID模块初始化失败，请重启应用重试"),
    DEVICE_NOT_FOUND("RFID连接失败：未搜索到蓝牙枪柄设备，请靠近枪柄后重试"),
    BLUETOOTH_DISABLED("RFID连接失败：蓝牙未开启，请先开启蓝牙"),
    LOCATION_REQUIRED("RFID连接失败：请开启定位服务并允许定位权限（搜索蓝牙枪柄需要）"),
    UNKNOWN("RFID模块连接失败，请重启应用重试");

    private final String message;

    RfidError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}