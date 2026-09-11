package com.gsoft.inventory.service.rfid;

/**
 * 统一RFID参数模型：各适配器负责把统一取值映射到自家SDK的参数体系
 */
public class RfidConfig {

    /** 盘点区域：EPC */
    public static final int MODE_EPC = 1;
    /** 盘点区域：EPC+TID */
    public static final int MODE_EPC_TID = 2;
    /** 频率区域：中国920~925MHz */
    public static final int REGION_CHN = 1;
    /** 频率区域：欧洲865~868MHz */
    public static final int REGION_EU = 2;
    /** 频率区域：美国902~928MHz */
    public static final int REGION_USA = 3;

    /** UHF功率(dBm) */
    public int power = 26;
    /** 盘点区域 */
    public int mode = MODE_EPC;
    /** 盘点会话：0=S0 1=S1 */
    public int session = 1;
    /** 频率区域 */
    public int region = REGION_CHN;
}