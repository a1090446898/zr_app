package com.gsoft.inventory.view.rfidfind;

import android.text.TextUtils;

import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RFID找资产条件（扫描目标过滤条件）。
 * <p>
 * 三种查找方式：
 * 1. 找个人资产：管理人/使用人为当前登录人；
 * 2. 找他人资产：管理人/使用人为指定人员（手动输入或从人员列表选择，仅按姓名匹配）；
 * 3. 找特定资产：只认事先从资产列表选中的若干资产。
 * <p>
 * 查找范围为全部资产，不受当前盘点部门/存放地点限制；
 * 扫描命中后只有目标资产才继续走标记、卡片与提示音流程，非目标静默忽略。
 * <p>
 * 本类可序列化，随 Intent 在扫描页与找资产设置页之间传递。
 */
public class FindAssetsCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 未开启查找（普通盘点，扫到什么记什么） */
    public static final int MODE_NONE = 0;
    /** 找个人资产：当前登录人 */
    public static final int MODE_SELF = 1;
    /** 找他人资产：指定人员 */
    public static final int MODE_OTHER = 2;
    /** 找特定资产：从资产列表选择 */
    public static final int MODE_TARGET = 3;

    /** 匹配管理人（CUSTODIAN） */
    public static final int MATCH_CUSTODIAN = 1;
    /** 匹配使用人（ASSETSUSER） */
    public static final int MATCH_ASSETS_USER = 1 << 1;
    /** 管理人与使用人都匹配 */
    public static final int MATCH_ALL = MATCH_CUSTODIAN | MATCH_ASSETS_USER;

    /** 查找方式 */
    private int mode = MODE_NONE;
    /** 人员姓名（找个人/找他人方式使用，仅按姓名匹配） */
    private String personName = "";
    /** 匹配字段：管理人/使用人/全部 */
    private int matchField = MATCH_ALL;
    /** 指定资产Id（找特定资产方式使用） */
    private long[] targetAssetIds = new long[0];
    /** 指定资产中可通过条码扫描命中的数量（有条码的资产数） */
    private int scannableTargetCount = 0;

    /** 指定资产Id集合：避免每次命中都遍历数组，反序列化后按需重建 */
    private transient Set<Long> targetIdSet;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName == null ? "" : StringUtils.trim(personName);
        if (this.personName == null) {
            this.personName = "";
        }
    }

    public int getMatchField() {
        return matchField;
    }

    public void setMatchField(int matchField) {
        this.matchField = matchField & MATCH_ALL;
        if (this.matchField == 0) {
            this.matchField = MATCH_ALL;
        }
    }

    public long[] getTargetAssetIds() {
        return targetAssetIds.clone();
    }

    public void setTargetAssetIds(long[] targetAssetIds) {
        this.targetAssetIds = targetAssetIds == null ? new long[0] : targetAssetIds.clone();
        this.targetIdSet = null;
    }

    public int getScannableTargetCount() {
        return scannableTargetCount;
    }

    public void setScannableTargetCount(int scannableTargetCount) {
        this.scannableTargetCount = Math.max(scannableTargetCount, 0);
    }

    /** 查找是否已开启（未开启时按普通盘点处理） */
    public boolean isEnabled() {
        return mode != MODE_NONE;
    }

    /**
     * 判断资产是否为本次查找的目标（扫描过程主线程高频调用，人员方式只做字符串比较，
     * 指定资产方式走Set查找，不做数据库操作）
     */
    public boolean matches(Assets assets) {
        if (assets == null) {
            return false;
        }
        switch (mode) {
            case MODE_SELF:
            case MODE_OTHER:
                return matchesPerson(assets);
            case MODE_TARGET:
                return assets.getId() != null && targetIdSet().contains(assets.getId());
            default:
                return true;
        }
    }

    /** 人员方式匹配：按所选字段比较管理人或使用人（去首尾空格） */
    private boolean matchesPerson(Assets assets) {
        if (StringUtils.isNullOrEmpty(personName)) {
            return false;
        }
        if ((matchField & MATCH_CUSTODIAN) != 0
                && personName.equals(StringUtils.trim(assets.getCUSTODIAN()))) {
            return true;
        }
        return (matchField & MATCH_ASSETS_USER) != 0
                && personName.equals(StringUtils.trim(assets.getASSETSUSER()));
    }

    /**
     * 统计目标资产总数（用于扫描页“目标 N 项”展示）：人员方式走count查询，
     * 指定资产方式直接取选择时统计的有条码数量。需在子线程调用。
     */
    public long countTargets() {
        if (mode == MODE_TARGET) {
            return scannableTargetCount;
        }
        if (mode == MODE_NONE || StringUtils.isNullOrEmpty(personName)) {
            return 0;
        }
        List<String> whereParts = new ArrayList<>();
        List<String> args = new ArrayList<>();
        if ((matchField & MATCH_CUSTODIAN) != 0) {
            whereParts.add("TRIM(CUSTODIAN) = ?");
            args.add(personName);
        }
        if ((matchField & MATCH_ASSETS_USER) != 0) {
            whereParts.add("TRIM(ASSETSUSER) = ?");
            args.add(personName);
        }
        if (whereParts.isEmpty()) {
            return 0;
        }
        return Assets.count(Assets.class, TextUtils.join(" OR ", whereParts),
                args.toArray(new String[0]));
    }

    /** 查找方式名称：个人资产 / 他人资产 / 指定资产 */
    public String getModeName() {
        switch (mode) {
            case MODE_SELF:
                return "个人资产";
            case MODE_OTHER:
                return "他人资产";
            case MODE_TARGET:
                return "指定资产";
            default:
                return "未开启";
        }
    }

    /** 匹配字段名称：管理人 / 使用人 / 全部 */
    public String getMatchFieldName() {
        return matchFieldName(matchField);
    }

    public static String matchFieldName(int matchField) {
        switch (matchField & MATCH_ALL) {
            case MATCH_CUSTODIAN:
                return "管理人";
            case MATCH_ASSETS_USER:
                return "使用人";
            default:
                return "全部";
        }
    }

    /**
     * 条件摘要：用于设置菜单与扫描页状态行
     * 例：个人资产（张三 · 全部） / 指定资产（5 项）
     */
    public String getSummary() {
        switch (mode) {
            case MODE_SELF:
            case MODE_OTHER:
                String name = StringUtils.isNullOrEmpty(personName) ? "未设置人员" : personName;
                return getModeName() + "（" + name + " · " + getMatchFieldName() + "）";
            case MODE_TARGET:
                return getModeName() + "（" + scannableTargetCount + " 项）";
            default:
                return "未开启";
        }
    }

    /**
     * 条件标识：判断两次设置是否完全一致，避免用户只查看设置未做修改时误清空扫描列表
     */
    public String configKey() {
        return mode + "|" + personName + "|" + matchField + "|" + Arrays.toString(targetAssetIds);
    }

    private Set<Long> targetIdSet() {
        if (targetIdSet == null) {
            Set<Long> ids = new HashSet<>();
            for (long id : targetAssetIds) {
                ids.add(id);
            }
            targetIdSet = ids;
        }
        return targetIdSet;
    }
}
