package com.gsoft.inventory.service.scanner;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;

/**
 * 红外/硬件扫码读取器统一接口：业务页面只依赖本接口，不感知具体厂家SDK
 * <p>
 * 各厂家适配器实现初始化/扫描/生命周期，并把SDK差异（广播action、触发方式、按键集合、线程模型）
 * 封闭在适配器内；新增厂家只需实现本接口并在ScannerFactory注册
 *
 * @author 10904
 */
public interface IScannerReader {

    /** 厂家名称（日志与页面提示用） */
    String getVendorName();

    /** 是否支持当前设备（轻量探测，不建立连接） */
    boolean isSupported(Context context);

    /** 是否支持程序软触发扫描（false时页面隐藏"开始/停止扫描"按钮，仅物理扫描键触发） */
    boolean supportsSoftwareTrigger();

    /** 初始化：注册回调/广播、打开解码器（页面onCreate调用一次） */
    void init(Activity activity, ScannerCallbacks callbacks);

    /** 页面恢复：注册广播/恢复扫描服务（与onPagePause配对） */
    void onPageResume();

    /** 页面暂停：停止出光/注销广播，避免后台重复响应 */
    void onPagePause();

    /** 页面销毁：释放SDK资源与广播 */
    void release();

    /** 软触发开始扫描（supportsSoftwareTrigger=true时有效） */
    void startScan();

    /** 软触发停止扫描 */
    void stopScan();

    /** 物理按键按下转发（适配器自行判断是否为其扫描键，命中返回true） */
    boolean onKeyDown(int keyCode, KeyEvent event);

    /** 物理按键抬起转发 */
    boolean onKeyUp(int keyCode, KeyEvent event);
}