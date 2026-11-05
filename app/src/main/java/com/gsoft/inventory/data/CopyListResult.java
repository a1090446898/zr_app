package com.gsoft.inventory.data;

import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量复制列表查询结果
 *
 * @author 10904
 */
public class CopyListResult {

    private long totalCount;

    private List<Assets> assets = new ArrayList<>();

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public List<Assets> getAssets() {
        return assets;
    }

    public void setAssets(List<Assets> assets) {
        this.assets = assets == null ? new ArrayList<>() : assets;
    }
}