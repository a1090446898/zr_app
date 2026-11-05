package com.gsoft.inventory.utils.print_batch;

import com.gsoft.inventory.utils.domain.Assets;

/**
 * 批量打印执行器：一次打印一张，结果异步回调。
 * 新增打印机品牌时实现本接口，并在PrintCompatActivity#createBatchPrintTarget登记，
 * 队列（BatchPrintSession）与调用页无需改动。
 *
 * @author 10904
 */
public interface BatchPrintTarget {

    /**
     * 打印单张标签
     *
     * @param assets   待打印资产
     * @param callback 结果回调（成功与失败都必须回调一次）
     */
    void printOne(Assets assets, Callback callback);

    interface Callback {
        void onResult(boolean success, String message);
    }
}