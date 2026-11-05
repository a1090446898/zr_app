package com.gsoft.inventory.utils.ds_print;

import android.content.Context;

import com.gsoft.inventory.utils.SharedPreferencesUtils;

/**
 * 得实RFID打印参数（独立于宝威打印栈，仅本包与得实打印页使用）
 * <p>
 * 对外只暴露四项：
 * 1. 打印分辨率（203/300dpi，默认203）：决定位图像素数，换机型时需同步调整；
 * 2. 整版偏移X/Y（mm，可负）：预览与发送共用，二维码与文字线框一起平移；
 * 3. 上次连接的蓝牙MAC/名称：用于下次进页自动重连；
 * 4. 条码长度固定14位：与EPC写入规则绑定，不提供修改。
 * <p>
 * 浓度、灰度阈值、读写功率、打印速度、方向修正一律由代码固定（见DsPrintManager），
 * 现场用厂家调试工具校准，不在App内暴露，避免误调导致整批标签报废。
 *
 * @author 10904
 */
public final class DsPrintSettings {

    /** 203dpi（默认）：约8dot/mm，60mm=480dot、70mm=560dot */
    public static final int DPI_203 = 203;
    /** 300dpi：约11.81dot/mm，60mm=709dot、70mm=827dot */
    public static final int DPI_300 = 300;
    /** 默认分辨率：现场多数为203dpi */
    public static final int DEFAULT_DPI = DPI_203;

    /** 偏移绝对值上限（mm）：超出视为输入错误，避免整版被裁出纸外 */
    public static final float OFFSET_LIMIT_MM = 10f;

    private static final String KEY_DPI = "KEY_DS_PRINT_DPI";
    private static final String KEY_OFFSET_X = "KEY_DS_PRINT_OFFSET_X";
    private static final String KEY_OFFSET_Y = "KEY_DS_PRINT_OFFSET_Y";
    private static final String KEY_LAST_MAC = "KEY_DS_PRINT_LAST_MAC";
    private static final String KEY_LAST_NAME = "KEY_DS_PRINT_LAST_NAME";

    private DsPrintSettings() {
    }

    /** 当前分辨率（203或300） */
    public static int getDpi(Context context) {
        return SharedPreferencesUtils.getInt(context, KEY_DPI, DEFAULT_DPI) == DPI_300 ? DPI_300 : DPI_203;
    }

    public static void setDpi(Context context, int dpi) {
        SharedPreferencesUtils.putInt(context, KEY_DPI, dpi == DPI_300 ? DPI_300 : DPI_203);
    }

    /** 每毫米点数：203dpi≈8，300dpi≈11.81 */
    public static float getDotsPerMm(Context context) {
        return getDpi(context) / 25.4f;
    }

    /** 整版水平偏移（mm）：正数右移、负数左移 */
    public static float getOffsetX(Context context) {
        return parseFloat(SharedPreferencesUtils.getString(context, KEY_OFFSET_X, "0"));
    }

    public static void setOffsetX(Context context, float mm) {
        SharedPreferencesUtils.putString(context, KEY_OFFSET_X, String.valueOf(clampOffset(mm)));
    }

    /** 整版纵向偏移（mm）：正数下移、负数上移 */
    public static float getOffsetY(Context context) {
        return parseFloat(SharedPreferencesUtils.getString(context, KEY_OFFSET_Y, "0"));
    }

    public static void setOffsetY(Context context, float mm) {
        SharedPreferencesUtils.putString(context, KEY_OFFSET_Y, String.valueOf(clampOffset(mm)));
    }

    /** 上次成功连接的蓝牙MAC（空串表示无记录） */
    public static String getLastMac(Context context) {
        return SharedPreferencesUtils.getString(context, KEY_LAST_MAC, "");
    }

    /** 上次成功连接的蓝牙名称 */
    public static String getLastMacName(Context context) {
        return SharedPreferencesUtils.getString(context, KEY_LAST_NAME, "");
    }

    public static void setLastDevice(Context context, String mac, String name) {
        SharedPreferencesUtils.putString(context, KEY_LAST_MAC, mac == null ? "" : mac);
        SharedPreferencesUtils.putString(context, KEY_LAST_NAME, name == null ? "" : name);
    }

    /** 偏移限幅：只挡明显输错的数值，不挡正常校准范围 */
    public static float clampOffset(float mm) {
        if (mm > OFFSET_LIMIT_MM) {
            return OFFSET_LIMIT_MM;
        }
        if (mm < -OFFSET_LIMIT_MM) {
            return -OFFSET_LIMIT_MM;
        }
        return mm;
    }

    private static float parseFloat(String text) {
        try {
            return Float.parseFloat(text.trim());
        } catch (Exception e) {
            return 0f;
        }
    }
}