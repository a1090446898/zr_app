package com.gsoft.inventory.view.home;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.gsoft.inventory.abs.WebFragment;
import com.gsoft.inventory.utils.StringUtils;

import static com.gsoft.inventory.utils.SysDefine.WEB_URL;

/**
 * @author 10904
 */
public class WebHomeFragment extends WebFragment {

    private  String webUrl = WEB_URL + "/pages/404/index";


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            // 键名需与传值时一致
            String tempUrl = args.getString("targetWebUrl");
            if (!StringUtils.isNullOrEmpty(tempUrl)) {
                // 使用传递的参数覆盖默认值
                webUrl = WEB_URL + tempUrl;
            }
        }
        // 加载目标网页
        webView.loadUrl(webUrl);
    }

    public void loadUrl(String url) {
        if (webView != null) {
            webUrl = WEB_URL + url;
            webView.loadUrl(webUrl);
        }
    }


    public boolean handleBackPress() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }



}
