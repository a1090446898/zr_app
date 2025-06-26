package com.gsoft.inventory.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtils {

    public static final String NAME = "INVENTORY_SHAREDATA";
    public static final String KEY_PROTOCAL = "KEY_PROTOCAL";
    public static final String KEY_ZTMC = "KEY_ZTMC";
    public static final String KEY_YHBH = "KEY_YHBH";
    public static final String KEY_YHMM = "KEY_YHMM";
    public static final String KEY_DEVICENO = "KEY_DEVICENO";
    public static final String KEY_QCY = "KEY_QCY";
    public static final String KEY_QCY_CODE = "KEY_QCY_CODE";
    public static final String KEY_PRINTDEVICE = "KEY_PRINTDEVICE";
    public static final String KEY_PRINT_FIELDS_CODE = "KEY_PRINT_FIELDS_CODE";
    public static final String KEY_EDIT_QCY_CODE = "KEY_EDIT_QCY_CODE";
    public static final String KEY_PRINT_EFFECT = "KEY_PRINT_EFFECT";
    public static final String KEY_PRINT_TITLE = "KEY_PRINT_TITLE";
    public static final String KEY_PRINT_POSITION = "KEY_PRINT_POSITION";
    public static final String KEY_INVENTORY_SETTING = "KEY_INVENTORY_SETTING";
    public static final String SP_KEY_SERVERHOST = "SP_KEY_SERVERHOST";
    public static final String KEY_ZCQCYNAME = "SP_KEY_ZCQCYNAME";
    public static final String KEY_ZCQCYCODE = "SP_KEY_ZCQCYCODE";

    public static final String KEY_PRINTER_MAC = "KEY_PRINTER_MAC";
    public static final String KEY_PRINTER_NAME = "KEY_PRINTER_NAME";
    public static final String KEY_PRINTER_TYPE = "KEY_PRINTER_TYPE";

    // 用来保存记录打印信息，存放为JSON格式
    public static final String KEY_SAVE_PRINT_INFO = "KEY_SAVE_PRINT_INFO";

    //键 值
    public static void putInt(Context mContext, String key, int value) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(key, value).commit();
    }

    public static void putString(Context mContext, String key, String value) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        sp.edit().putString(key, value).commit();
    }

    public static void putBoolean(Context mContext, String key, boolean value) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key, value).commit();
    }

    //键 默认值
    public static String getString(Context mContext, String key, String defValue) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }

    public static int getInt(Context mContext, String key, int defValue) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);
    }

    public static boolean getBoolean(Context mContext, String key, boolean defValue) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defValue);
    }

    //删除单个
    public static void deleShare(Context mContext, String key) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        sp.edit().remove(key).commit();
    }

    //删除全部键值对信息
    public static void deleAll(Context mContext) {
        SharedPreferences sp = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        sp.edit().clear().commit();
    }
}
