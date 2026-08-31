package com.gsoft.inventory.entities;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author 10904
 */
public class AssetReceiveDetailItem implements Parcelable {
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
     * 品牌
     */
    private String brand;

    /**
     *  规格
     */
    private String assetsStandard;



    /**
     * 使用人
     */
    private String assetsUser;

    /**
     * 存放地点
     */
    private String assetsLayAdd;

    /**
     * 资产原值
     */
    private String assetsCurrPrice;

    /**
     * 资产数量
     */
    private String assetsNum;


    /**
     * 取得日期
     */
    private String acquisitionDate;

    /**
     * 成新度
     */
    private String assetsNewDegree;

    /**
     * 选中状态
     */
    private boolean isSelected;


    public AssetReceiveDetailItem() {

    }


    protected AssetReceiveDetailItem(Parcel in) {
        assetId = in.readString();
        assetsName = in.readString();
        barcodeId = in.readString();
        brand = in.readString();
        assetsStandard = in.readString();
        assetsLayAdd = in.readString();
        assetsCurrPrice = in.readString();
        assetsNum = in.readString();
        acquisitionDate = in.readString();
        assetsNewDegree = in.readString();
        // boolean 读取为 byte
        isSelected = in.readByte() != 0;
    }

    public static final Creator<AssetReceiveDetailItem> CREATOR = new Creator<AssetReceiveDetailItem>() {
        @Override
        public AssetReceiveDetailItem createFromParcel(Parcel in) {
            return new AssetReceiveDetailItem(in);
        }

        @Override
        public AssetReceiveDetailItem[] newArray(int size) {
            return new AssetReceiveDetailItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(assetId);
        dest.writeString(assetsName);
        dest.writeString(barcodeId);
        dest.writeString(brand);
        dest.writeString(assetsStandard);
        dest.writeString(assetsLayAdd);
        dest.writeString(assetsCurrPrice);
        dest.writeString(assetsNum);
        dest.writeString(acquisitionDate);
        dest.writeString(assetsNewDegree);
        // boolean 写入为 byte
        dest.writeByte((byte) (isSelected ? 1 : 0));
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


    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getAssetsNum() {
        return assetsNum;
    }

    public void setAssetsNum(String assetsNum) {
        this.assetsNum = assetsNum;
    }

    public String getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(String acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getAssetsNewDegree() {
        return assetsNewDegree;
    }

    public void setAssetsNewDegree(String assetsNewDegree) {
        this.assetsNewDegree = assetsNewDegree;
    }
}
