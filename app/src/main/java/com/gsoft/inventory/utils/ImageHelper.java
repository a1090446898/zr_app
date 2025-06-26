package com.gsoft.inventory.utils;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageHelper {
    public static File compressPhotoImage(String barCode, int photoIndex) {
        String fileName = String.format("%1$s_%2$d.jpg", barCode, photoIndex);
        File photoFile = new File(SysConfig.photoDirectory + fileName);
        while (photoFile.exists()) {
            photoIndex++;
            photoFile = new File(SysConfig.photoDirectory + String.format("%1$s_%2$d.jpg", barCode, photoIndex));
        }
        BitmapHelper.compressBitmap(SysConfig.photo_temp_file, 800, 600, true, photoFile);
        return photoFile;
    }

    public static File compressPhotoImage(String fileSource, String barCode, int photoIndex) {
        String fileName = String.format("%1$s_%2$d.jpg", barCode, photoIndex);
        File photoFile = new File(SysConfig.photoDirectory + fileName);
        while (photoFile.exists()) {
            photoIndex++;
            photoFile = new File(SysConfig.photoDirectory + String.format("%1$s_%2$d.jpg", barCode, photoIndex));
        }
        BitmapHelper.compressBitmap(fileSource, 800, 600, true, photoFile);
        return photoFile;
    }


    public static File savePhotoImage(Bitmap photo, String barCode, int photoIndex) {
        String fileName = String.format("%1$s_%2$d.jpg", barCode, photoIndex);
        File photoFile = new File(SysConfig.photoDirectory + fileName);
        try {
            //FileOutputStream outputStream = new FileOutputStream(photoFile);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(photoFile));
            photo.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            Log.e("SAVE PHOTO IMAGE", "err" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("SAVE PHOTO IMAGE", "err" + e.getMessage());
            e.printStackTrace();
        }
        return photoFile;
    }
}
