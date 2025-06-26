package com.gsoft.inventory.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkUtils {

    public static boolean checkNetConnect(Context context) {
        ConnectivityManager cwjManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cwjManager != null) {
            NetworkInfo info = cwjManager.getActiveNetworkInfo();
            return info != null && info.isAvailable() && info.isConnected();
        }
        return false;
    }
}
