package com.gsoft.inventory.inter;

import com.gsoft.inventory.entities.AssetPhoto;

import java.util.concurrent.CountDownLatch;

public interface OnUploadFinishListener {

    void setCountDownLatch(int size);

    CountDownLatch getCountDownLatch();
    void countDown();

    long getCount();

    void onUploadSuccess(AssetPhoto photo);
    void onUploadFailed(AssetPhoto photo, Exception e);
}
