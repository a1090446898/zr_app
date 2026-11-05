package com.gsoft.inventory.data;

import java.io.Serializable;

/**
 * 批量复制的批量设置项：只覆盖非空字段，留空表示沿用源资产原值
 *
 * @author 10904
 */
public class BatchCopyOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 复制次数 */
    private int copyTimes = 1;

    /** 是否复制图片 */
    private boolean copyImg;

    /** 存放地点，空表示沿用源资产 */
    private String assetsLayAdd = "";

    /** 使用部门名称，空表示沿用源资产 */
    private String assetsDept = "";

    /** 管理部门单位编号 DWBH，空表示沿用源资产 */
    private String groundManageDept = "";

    /** 管理人姓名，空表示沿用源资产 */
    private String custodian = "";

    /** 使用人姓名，空表示沿用源资产 */
    private String assetsUser = "";

    public int getCopyTimes() {
        return copyTimes;
    }

    public void setCopyTimes(int copyTimes) {
        this.copyTimes = copyTimes;
    }

    public boolean isCopyImg() {
        return copyImg;
    }

    public void setCopyImg(boolean copyImg) {
        this.copyImg = copyImg;
    }

    public String getAssetsLayAdd() {
        return assetsLayAdd;
    }

    public void setAssetsLayAdd(String assetsLayAdd) {
        this.assetsLayAdd = assetsLayAdd;
    }

    public String getAssetsDept() {
        return assetsDept;
    }

    public void setAssetsDept(String assetsDept) {
        this.assetsDept = assetsDept;
    }

    public String getGroundManageDept() {
        return groundManageDept;
    }

    public void setGroundManageDept(String groundManageDept) {
        this.groundManageDept = groundManageDept;
    }

    public String getCustodian() {
        return custodian;
    }

    public void setCustodian(String custodian) {
        this.custodian = custodian;
    }

    public String getAssetsUser() {
        return assetsUser;
    }

    public void setAssetsUser(String assetsUser) {
        this.assetsUser = assetsUser;
    }
}