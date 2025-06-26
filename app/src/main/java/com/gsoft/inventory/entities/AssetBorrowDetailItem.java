package com.gsoft.inventory.entities;

/**
 * @author 10904
 */
public class AssetBorrowDetailItem {
    /**
     * 资产编号
     */
    private String assetId;

    /**
     * 资产名称
     */
    private String assetsName;

    /**
     * 条码编号
     */
    private String barcodeId;

    /**
     *  规格
     */
    private String assetsStandard;

    /**
     * 使用人
     */
    private String assetsUser;

    /**
     * 存放位置
     */
    private String assetsLayAdd;

    /**
     * 当前价值
     */
    private String assetsCurrPrice;

    /**
     * 选中状态
     */
    private boolean isSelected;


    public AssetBorrowDetailItem() {

    }

    public AssetBorrowDetailItem(String assetId, String assetsName, String barcodeId,
                                 String assetsStandard, String assetsUser, String assetsLayAdd,
                                 String assetsCurrPrice) {
        this.assetId = assetId;
        this.assetsName = assetsName;
        this.barcodeId = barcodeId;
        this.assetsStandard = assetsStandard;
        this.assetsUser = assetsUser;
        this.assetsLayAdd = assetsLayAdd;
        this.assetsCurrPrice = assetsCurrPrice;
    }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }


    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetsName() {
        return assetsName;
    }

    public void setAssetsName(String assetsName) {
        this.assetsName = assetsName;
    }

    public String getBarcodeId() {
        return barcodeId;
    }

    public void setBarcodeId(String barcodeId) {
        this.barcodeId = barcodeId;
    }

    public String getAssetsStandard() {
        return assetsStandard;
    }

    public void setAssetsStandard(String assetsStandard) {
        this.assetsStandard = assetsStandard;
    }

    public String getAssetsUser() {
        return assetsUser;
    }

    public void setAssetsUser(String assetsUser) {
        this.assetsUser = assetsUser;
    }

    public String getAssetsLayAdd() {
        return assetsLayAdd;
    }

    public void setAssetsLayAdd(String assetsLayAdd) {
        this.assetsLayAdd = assetsLayAdd;
    }

    public String getAssetsCurrPrice() {
        return assetsCurrPrice;
    }

    public void setAssetsCurrPrice(String assetsCurrPrice) {
        this.assetsCurrPrice = assetsCurrPrice;
    }
}
