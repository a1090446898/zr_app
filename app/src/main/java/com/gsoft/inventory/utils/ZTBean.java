package com.gsoft.inventory.utils;

/**
 * 账套信息
 */
public class ZTBean {
    public String getZTBH() {
        return ZTBH;
    }

    public void setZTBH(String ZTBH) {
        this.ZTBH = ZTBH;
    }

    public String getZTMC() {
        return ZTMC;
    }

    public void setZTMC(String ZTMC) {
        this.ZTMC = ZTMC;
    }

    /**
     * 账套编号
     */
    private String ZTBH;
    /**
     * 账套名称
     */
    private String ZTMC;
}
