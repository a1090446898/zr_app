package com.gsoft.inventory.service.rfid_stats;

import com.gsoft.inventory.adapter.RfidDeptStatsAdapter;

import java.util.List;

/**
 * 盘点统计数据载体
 * <p>
 * 承载一次统计的全部结果：总数、已盘数、个人已盘/全部数、分组明细列表
 */
public class DeptStatsData {
    /** 全部资产总数 */
    public int totalCount;
    /** 已盘数量 */
    public int countedTotal;
    /** 个人已盘数量（管理人=当前登录人） */
    public int personalCounted;
    /** 个人全部数量（管理人=当前登录人） */
    public int personalTotal;
    /** 分组明细列表（部门层为部门列表，存放地点层为地点列表） */
    public List<RfidDeptStatsAdapter.DeptStats> deptList;
}
