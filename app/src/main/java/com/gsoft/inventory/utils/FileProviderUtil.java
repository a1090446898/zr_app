package com.gsoft.inventory.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * @author 10904
 */
public class FileProviderUtil {
    private static final String AUTHORITY = "com.gsoft.inventory.provider";

    public static Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(context, AUTHORITY, file);
    }
}
