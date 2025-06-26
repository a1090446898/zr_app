package com.gsoft.inventory.utils;

import android.text.TextUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by gscli on 2017/6/3.
 */

public class DataConvert {

    public static DecimalFormat decimalFormat_T = new DecimalFormat("0.###");
    public static DecimalFormat decimalFormat_Five = new DecimalFormat("0.#####");
    public static DecimalFormat decimalFormat_S = new DecimalFormat("0.##");
    public static DecimalFormat decimalFormat_F = new DecimalFormat("0.#");
    private static String timeSpanStrFormat = "%02d:00-%02d:00";
    private static String datetimeSpanStrFormat = "%s %02d:00&%s %02d:00";
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat dateHMSFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat dateHMFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static SimpleDateFormat dateDayFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat barCodeTimeFormat = new SimpleDateFormat("yyMMddHHmmss");

    public static float convertFloat(String number) {
        try {
            float fNumber = Float.parseFloat(number);
            return fNumber;
        } catch (NumberFormatException ne) {
            return 999999;
        }
    }

    public static Date format2Date(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String formatTimeSpan(int sTime, int eTime) {
        return String.format(timeSpanStrFormat, sTime, eTime);
    }

    public static String formatDateTimeSpan(String dateStr, int sTime, int eTime) {
        return String.format(datetimeSpanStrFormat, sTime, eTime);
    }

    // 获取当前时间所在年的周数
    public static int getWeekOfYear(Date date) {
        Calendar c = new GregorianCalendar();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setMinimalDaysInFirstWeek(7);
        c.setTime(date);

        return c.get(Calendar.WEEK_OF_YEAR);
    }

    // 获取当前时间所在年的最大周数
    public static int getMaxWeekNumOfYear(int year) {
        Calendar c = new GregorianCalendar();
        c.set(year, Calendar.DECEMBER, 31, 23, 59, 59);

        return getWeekOfYear(c.getTime());
    }

    // 获取某年的第几周的开始日期
    public static Date getFirstDayOfWeek(int year, int week) {
        Calendar c = new GregorianCalendar();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        Calendar cal = (GregorianCalendar) c.clone();
        cal.add(Calendar.DATE, (week - (c.get(Calendar.DAY_OF_WEEK) == 2 ? 1 : 0)) * 7);
        return getFirstDayOfWeek(cal.getTime());
    }

    // 获取某年的第几周的结束日期
    public static Date getLastDayOfWeek(int year, int week) {
        Calendar c = new GregorianCalendar();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        Calendar cal = (GregorianCalendar) c.clone();
        cal.add(Calendar.DATE, (week - (c.get(Calendar.DAY_OF_WEEK) == 2 ? 1 : 0)) * 7);

        return getLastDayOfWeek(cal.getTime());
    }

    // 获取当前时间所在周的开始日期
    public static Date getFirstDayOfWeek(Date date) {
        Calendar c = new GregorianCalendar();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTime(date);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek()); // Monday
        return c.getTime();
    }

    // 获取当前时间所在周的结束日期
    public static Date getLastDayOfWeek(Date date) {
        Calendar c = new GregorianCalendar();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTime(date);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek() + 6); // Sunday
        return c.getTime();
    }

    public static String FormatDate2Day(Date date) {
        return dateDayFormat.format(date);
    }

    public static String FormatDate2Day() {
        Date date = new Date(System.currentTimeMillis());
        return FormatDate2Day(date);
    }

    public static String FormatDate2HM() {
        Date date = new Date(System.currentTimeMillis());
        return FormatDate2HM(date);
    }

    public static String FormatDate2HM(Date date) {
        return dateHMFormat.format(date);
    }

    public static String FormatDate2HMS() {
        Date date = new Date(System.currentTimeMillis());
        return FormatDate2HMS(date);
    }

    public static String FormatDate2HMS(Date date) {
        return dateHMSFormat.format(date);
    }

    public static String FormatDate2HMS(long timeMillis) {
        if (timeMillis < 1539698078) return "";
        Date date = new Date(timeMillis);
        return dateHMSFormat.format(date);
    }

    public static String formatTimeBarCode() {
        Date date = new Date(System.currentTimeMillis());
        return barCodeTimeFormat.format(date);
    }
}
