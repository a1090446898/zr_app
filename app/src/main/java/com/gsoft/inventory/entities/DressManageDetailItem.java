package com.gsoft.inventory.entities;

/**
 * @author 10904
 */
public class DressManageDetailItem {

    /**
     * 选中状态
     */
    private boolean isSelected;


    private String docId;

    /**
     * 服饰名称
     */
    private String dressName;


    /**
     * 领用数量
     */
    private String receiveNum;


    /**
     * 库存数量
     */
    private String inventoryQuantity;

    /**
     * 领用人
     */
    private String receiveUser;


    /**
     * 领用部门
     */
    private String receiveDepart;


    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getDressName() {
        return dressName;
    }

    public void setDressName(String dressName) {
        this.dressName = dressName;
    }

    public String getReceiveNum() {
        return receiveNum;
    }

    public void setReceiveNum(String receiveNum) {
        this.receiveNum = receiveNum;
    }

    public String getInventoryQuantity() {
        return inventoryQuantity;
    }

    public void setInventoryQuantity(String inventoryQuantity) {
        this.inventoryQuantity = inventoryQuantity;
    }

    public String getReceiveUser() {
        return receiveUser;
    }

    public void setReceiveUser(String receiveUser) {
        this.receiveUser = receiveUser;
    }

    public String getReceiveDepart() {
        return receiveDepart;
    }

    public void setReceiveDepart(String receiveDepart) {
        this.receiveDepart = receiveDepart;
    }
}
