package com.gsoft.inventory.utils;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public abstract class StringCallback implements Callback {

    /**
     * 返回可能为NULL
     *
     * @param result
     */
    public abstract void onResponseSuccess(String result);

    public abstract void onResponseFailure(String errMessage);

    @Override
    public void onFailure(Call call, IOException e) {
        String error = "";
        if (e.getMessage() != null) {
            error = e.getMessage();
        } else {
            error  = "（异常对象为空）";
        }
        onResponseFailure("ERROR:".concat(error));
    }

    @Override
    public void onResponse(Call call, Response response) {
        if (response.isSuccessful()) {
            try {
                String responseString = null;
                if (response.body() != null) {
                    responseString = response.body().string();
                }
                onResponseSuccess(responseString);
            } catch (IOException e) {
                e.printStackTrace();
                onResponseFailure("ERROR:".concat(Objects.requireNonNull(e.getMessage())));
            }
        } else {
            onResponseFailure("ERROR:网络请求失败");
        }
    }
}
