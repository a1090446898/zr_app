package com.gsoft.inventory.pojo;

import com.gsoft.inventory.entities.DisposeAsset;

import java.util.List;

public class DisposePojo {

    private String code;
    private String name;
    private String date;
    private String depart;
    private String remark;


    private List<DisposeAsset> disposeAssetList;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDepart() {
        return depart;
    }

    public void setDepart(String depart) {
        this.depart = depart;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<DisposeAsset> getDisposeAssetList() {
        return disposeAssetList;
    }

    public void setDisposeAssetList(List<DisposeAsset> disposeAssetList) {
        this.disposeAssetList = disposeAssetList;
    }
}
