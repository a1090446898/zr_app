package com.gsoft.inventory.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * @author 10904
 */
public class ClipboardUtils {
    public static void copy(Context context, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("clip", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "已复制到剪切板", Toast.LENGTH_SHORT).show();
    }
}
