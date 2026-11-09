package com.gsoft.inventory.const_data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户导入权限常量与导入模式映射
 *
 * @author 10904
 */
public class UserRoleConst {

    /**
     * 角色：个人导入
     */
    public static final String ROLE_IMPORT_PERSONAL = "3";
    /**
     * 角色：部门导入
     */
    public static final String ROLE_IMPORT_DEPARTMENT = "2";
    /**
     * 角色：全部部门导入（最高权限）
     */
    public static final String ROLE_IMPORT_ALL = "1";

    /**
     * 导入模式：个人导入
     */
    public static final int MODE_PERSONAL = 0;
    /**
     * 导入模式：部门导入
     */
    public static final int MODE_DEPARTMENT = 1;
    /**
     * 导入模式：全部部门导入
     */
    public static final int MODE_ALL = 2;

    /**
     * 角色取值归一化：去除首尾空格与前导0，兼容"1"与"01"两种存量写法
     *
     * @param role 账号ROLE字段
     * @return 归一化后的角色值，空值返回空串
     */
    private static String normalizeRole(String role) {
        String value = role == null ? "" : role.trim();
        while (value.length() > 1 && value.charAt(0) == '0') {
            value = value.substring(1);
        }
        return value;
    }

    /**
     * 根据角色获取可见的导入模式（高权限向下兼容）
     * 1：个人 + 部门 + 全部；2：个人 + 部门；3：仅个人；空或未知：仅全部部门导入（保持现状）
     *
     * @param role 账号ROLE字段
     * @return 可见的导入模式集合，顺序即Tab顺序
     */
    public static List<Integer> getVisibleModes(String role) {
        List<Integer> modes = new ArrayList<>();
        String value = normalizeRole(role);
        if (ROLE_IMPORT_ALL.equals(value)) {
            modes.add(MODE_PERSONAL);
            modes.add(MODE_DEPARTMENT);
            modes.add(MODE_ALL);
        } else if (ROLE_IMPORT_DEPARTMENT.equals(value)) {
            modes.add(MODE_PERSONAL);
            modes.add(MODE_DEPARTMENT);
        } else if (ROLE_IMPORT_PERSONAL.equals(value)) {
            modes.add(MODE_PERSONAL);
        } else {
            modes.add(MODE_ALL);
        }
        return modes;
    }

    /**
     * 是否只有个人权限（仅个人导入模式可见）
     *
     * @param role 账号ROLE字段
     * @return true=个人权限
     */
    public static boolean isPersonalOnly(String role) {
        List<Integer> modes = getVisibleModes(role);
        return modes.size() == 1 && modes.get(0) == MODE_PERSONAL;
    }

    /**
     * 顶部统计分组标题：最高权限→"单位"，部门权限→"部门"，个人权限→null（整组不显示）
     * <p>
     * 复用导入模式映射判断层级，避免在页面中重复比较ROLE值
     *
     * @param role 账号ROLE字段
     * @return 分组标题；null表示不显示该组
     */
    public static String getScopeTitle(String role) {
        List<Integer> modes = getVisibleModes(role);
        if (modes.contains(MODE_ALL)) {
            return "单位";
        }
        if (modes.contains(MODE_DEPARTMENT)) {
            return "部门";
        }
        return null;
    }
}