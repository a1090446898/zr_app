package com.gsoft.inventory.utils;

import android.content.Context;

import com.gsoft.inventory.R;

public class DictionaryHelper {

    /**
     * 根据词典的代码返回名称
     *
     * @param code      选择的时候按顺序的代码 01，02，03等
     * @param arrSource 字典数组
     * @return 名称
     */
    public static String getDictionaryName(String code, String[] arrSource) {
        if (arrSource == null) return "";
        try {
            int dIndex = Integer.parseInt(StringUtils.leftTrim(code, '0'));
            return arrSource[dIndex - 1];
        } catch (NumberFormatException e) {
            return "";
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * 根据词典的代码返回名称
     *
     * @param code       选择的时候按顺序的代码 01，02，03等
     * @param arrayResId 字典数组id
     * @return 名称
     */
    public static String getDictionaryName(String code, int arrayResId, Context mContext) {
        try {
            int dIndex = Integer.parseInt(StringUtils.leftTrim(code, '0'));
            return mContext.getResources().getStringArray(arrayResId)[dIndex - 1];
        } catch (NumberFormatException e) {
            return "";
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
