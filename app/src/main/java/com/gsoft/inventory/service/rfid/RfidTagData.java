package com.gsoft.inventory.service.rfid;

/**
 * 统一标签数据模型：各厂家适配器把SDK标签数据归一化到该结构
 */
public class RfidTagData {
    /** EPC十六进制字符串（大写，可能带PC字头，业务层解码时自行处理） */
    public String epc;
    /** 信号强度RSSI（无有效值时0） */
    public int rssi;
    /** TID十六进制字符串（不支持TID的厂家为null） */
    public String tid;
}