package com.gsoft.inventory.utils;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 10904
 */
public class DateFormatUtils {
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");

    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat YYYYMMDD_HHMMSS = new SimpleDateFormat("yyyyMMdd_HHmmss");


    public static String formatTime(long time) {
        return YYYY_MM_DD_HH_MM_SS.format(time);
    }

    public static String formatDate(Date date) {
        return YYYY_MM_DD.format(date);
    }
}
