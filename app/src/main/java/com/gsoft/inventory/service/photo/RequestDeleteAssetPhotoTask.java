package com.gsoft.inventory.service.photo;

import android.net.Uri;

import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.concurrent.Callable;

/**
 * 删除服务器上的资产照片
 * <p>
 * 由 AsyncManager 统一调度，替代原 PhotoActivity 内部的 AsyncTask。
 *
 * @author 10904
 */
public class RequestDeleteAssetPhotoTask implements Callable<Boolean> {

    private final String imageUrl;

    public RequestDeleteAssetPhotoTask(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public Boolean call() throws Exception {
        String imageId = Uri.parse(imageUrl).getQueryParameter("IMGID");
        if (StringUtils.isNullOrEmpty(imageId)) {
            return false;
        }
        String result = OkHttpUtils.getInstance().get(
                String.format(SysDefine.SERVER_ASSET_PHOTO_DELETE(), imageId));
        return "true".equals(result);
    }
}