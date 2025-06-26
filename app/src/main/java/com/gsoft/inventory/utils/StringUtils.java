package com.gsoft.inventory.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringUtils {
    public static boolean isNullOrEmpty(String str) {
        return str == null || TextUtils.isEmpty(str);
    }

    /*左右空格都去掉*/
    public static String trim(String str) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            //return leftTrim(rightTrim(str));
            return str.replaceAll("^[　 ]+|[　 ]+$", "");
        }
    }

    /*去左空格*/
    public static String leftTrim(String str) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            return str.replaceAll("^[　 ]+", "");
        }
    }

    /*去右空格*/
    public static String rightTrim(String str) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            return str.replaceAll("[　 ]+$", "");
        }
    }

    /*左右空格都去掉*/
    public static String trim(String str, char trimChar) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            //return leftTrim(rightTrim(str));
            return str.replaceAll("^[" + trimChar + "]+|[\" + trimChar + \"]+$", "");
        }

    }

    /*去左空格*/
    public static String leftTrim(String str, char trimChar) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            return str.replaceAll("^[" + trimChar + "]+", "");
        }
    }

    /*去右空格*/
    public static String rightTrim(String str, char trimChar) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            return str.replaceAll("[" + trimChar + "]+$", "");
        }
    }

    public static String getPrintLabel(String zcmc, String syr) {
        return String.format("资产名称：%1$s  使用人：%2$s", zcmc, syr);
    }

    public static String fillPrintLine(String title) {
        if (title.length() >= 16) {
            return title;
        }
        int spanLength = (16 - title.length()) / 2;
        String fixSpace = "";
        for (int i = 0; i < spanLength; i++) {
            fixSpace += "    ";
        }
        return fixSpace + title + fixSpace;
    }

    public static String fillPrintNameLine(String title) {
        if (title.length() >= 24) {
            return title;
        }
        int spanLength = (24 - title.length()) / 2;
        String fixSpace = "";
        for (int i = 0; i < spanLength; i++) {
            fixSpace += "    ";
        }
        return fixSpace + title + fixSpace;
    }

    public static String fillPrintBarcodeLine(String barcode) {
        if (barcode.length() >= 28) {
            return barcode;
        }
        int spanLength = (28 - barcode.length()) / 2;
        String fixSpace = "";
        for (int i = 0; i < spanLength; i++) {
            fixSpace += "   ";
        }
        return fixSpace + barcode + fixSpace;
    }

    public static int getDWMCPrefix(String dwmc) {
        double prefix = (18 - dwmc.length()) / 2.0;
        return (int) (prefix * 24);
    }

    public static int getBarcodePrefix(String barcode) {
        double prefix = (39 - barcode.length()) / 2.0;
        return (int) (prefix * 11);
    }

    public static int getBottomPrefix(String bottomstr) {
        double prefix = (23 - bottomstr.length() + 1) / 2.0;
        return (int) (prefix * 20);
    }

    public static int getBarcodeStartPosition(String barcode) {
        int barLength = barcode.length();
        int startPosition = 90;
        if (barLength < 13) {
            startPosition += (13 - barLength) * 10;
        } else {
            startPosition -= (barLength - 13) * 10;
        }
        return startPosition;
    }

    public static String decodeQRCode(String str) {
        String formart = "";

        try {
            boolean ISO = StandardCharsets.ISO_8859_1.newEncoder().canEncode(str);
            if (ISO) {
                formart = new String(str.getBytes("ISO-8859-1"), "GB2312");
                Log.e("AssetsCodeScanActivity      ISO8859-1", formart);
            } else {
                formart = str;
                Log.e("AssetsCodeScanActivity      stringExtra", str);
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return formart;
    }
}
