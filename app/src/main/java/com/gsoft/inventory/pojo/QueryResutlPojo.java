package com.gsoft.inventory.pojo;

import com.gsoft.inventory.utils.Assets;

import java.util.List;

public class QueryResutlPojo {

    private long total;
    private List<Assets> assetsList;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<Assets> getAssetsList() {
        return assetsList;
    }

    public void setAssetsList(List<Assets> assetsList) {
        this.assetsList = assetsList;
    }
}
