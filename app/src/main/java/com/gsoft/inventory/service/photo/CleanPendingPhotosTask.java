package com.gsoft.inventory.service.photo;

import android.content.Context;

import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.utils.PhotoStorageHelper;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 清除所有待上传照片记录及本地照片文件。
 * <p>
 * 用于首页“照片清除”功能：先删除本地照片文件，再删除数据库记录，
 * 避免只删记录导致照片文件残留在存储目录中。
 *
 * @author 10904
 */
public class CleanPendingPhotosTask implements Callable<Integer> {

    private final Context context;

    public CleanPendingPhotosTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Integer call() throws Exception {
        List<AssetPhoto> pendingPhotos = AssetPhoto.find(AssetPhoto.class, "STATUS=?", "待上传");
        if (pendingPhotos == null || pendingPhotos.isEmpty()) {
            return 0;
        }
        int cleanCount = 0;
        for (AssetPhoto photo : pendingPhotos) {
            // assets为空时按文件名递归清理历史目录下的同名残留
            PhotoStorageHelper.deleteLocalPhoto(context, null, photo);
            cleanCount++;
        }
        return cleanCount;
    }
}