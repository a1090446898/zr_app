package com.gsoft.inventory.service.scanner;

/**
 * 统一扫码错误码：各厂家SDK差异映射后的通用语义，UI文案由此枚举统一输出
 *
 * @author 10904
 */
public enum ScannerError {

    NOT_SUPPORTED("当前设备不支持红外扫描"),
    INIT_FAILED("扫码模块初始化失败，请重启应用重试"),
    START_FAILED("启动扫描失败，请重试"),
    UNKNOWN("扫码模块异常，请重启应用重试");

    private final String message;

    ScannerError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}