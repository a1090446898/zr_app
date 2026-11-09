package com.gsoft.inventory.utils;

import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;

import java.util.Locale;

/**
 * 盘点统计数值显示辅助
 * <p>
 * 1. 数值统一千分位格式（如 123456 → 123,456）
 * 2. 按一组数值中的最大位数统一计算字号：顶部卡片与分组卡片各一套档位，
 *    同一次刷新的三个数值使用同一字号，保证三列对齐美观
 * 3. 叠加AutoSize兜底（上限=档位字号，下限见常量）：常态下三列字号即档位值且完全一致，
 *    仅系统字体放大等极端情况自动微缩，保证数值完整显示不截断
 */
public class CountValueHelper {

    /** 顶部统计卡片数值字号下限(sp) */
    private static final int TOP_CARD_MIN_SP = 10;
    /** 顶部六列并排数值字号下限(sp) */
    private static final int TOP_CARD_SIX_MIN_SP = 8;
    /** 分组卡片数值字号下限(sp) */
    private static final int GROUP_CARD_MIN_SP = 8;

    /**
     * 顶部统计卡片：按位数取字号（基准28sp，千分位后逐档下调）
     *
     * @param digits 一组数值中的最大位数
     * @return 字号(sp)
     */
    public static int topCardFontSizeSp(int digits) {
        if (digits <= 4) {
            return 28;
        }
        if (digits == 5) {
            return 26;
        }
        if (digits == 6) {
            return 22;
        }
        if (digits == 7) {
            return 18;
        }
        return 16;
    }

    /**
     * 顶部六列并排统计卡片：按位数取字号（基准18sp，列宽更窄逐档下调）
     *
     * @param digits 一组数值中的最大位数
     * @return 字号(sp)
     */
    public static int topCardSixFontSizeSp(int digits) {
        if (digits <= 4) {
            return 18;
        }
        if (digits == 5) {
            return 16;
        }
        if (digits == 6) {
            return 14;
        }
        if (digits == 7) {
            return 12;
        }
        return 10;
    }

    /**
     * 分组卡片：按位数取字号（基准13sp，列宽更窄逐档下调）
     *
     * @param digits 一组数值中的最大位数
     * @return 字号(sp)
     */
    public static int groupCardFontSizeSp(int digits) {
        if (digits <= 4) {
            return 13;
        }
        if (digits == 5) {
            return 12;
        }
        if (digits == 6) {
            return 10;
        }
        return 9;
    }

    /**
     * 取一组数值中的最大位数（用于三个数值统一字号）
     */
    public static int maxDigitsOf(int... values) {
        int maxDigits = 1;
        for (int value : values) {
            int digits = digitsOf(value);
            if (digits > maxDigits) {
                maxDigits = digits;
            }
        }
        return maxDigits;
    }

    /**
     * 顶部统计卡片数值显示：千分位文本 + 统一字号 + AutoSize兜底
     */
    public static void showTopCardCount(TextView textView, int value, int maxDigits) {
        showCount(textView, value, topCardFontSizeSp(maxDigits), TOP_CARD_MIN_SP);
    }

    /**
     * 顶部六列并排统计卡片数值显示：千分位文本 + 统一字号 + AutoSize兜底
     */
    public static void showTopCardSixCount(TextView textView, int value, int maxDigits) {
        showCount(textView, value, topCardSixFontSizeSp(maxDigits), TOP_CARD_SIX_MIN_SP);
    }

    /**
     * 分组卡片数值显示：千分位文本 + 统一字号 + AutoSize兜底
     */
    public static void showGroupCardCount(TextView textView, int value, int maxDigits) {
        showCount(textView, value, groupCardFontSizeSp(maxDigits), GROUP_CARD_MIN_SP);
    }

    /** 数值位数（0按1位计） */
    private static int digitsOf(int value) {
        int digits = 1;
        long absValue = Math.abs((long) value);
        while (absValue >= 10) {
            absValue /= 10;
            digits++;
        }
        return digits;
    }

    /** 设置千分位文本并应用"统一字号 + AutoSize兜底" */
    private static void showCount(TextView textView, int value, int sizeSp, int minSp) {
        textView.setText(String.format(Locale.CHINA, "%,d", value));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                textView, minSp, sizeSp, 1, TypedValue.COMPLEX_UNIT_SP);
    }
}