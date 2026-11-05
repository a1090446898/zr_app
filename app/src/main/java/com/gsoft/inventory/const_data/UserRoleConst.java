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
     * 根据角色获取可见的导入模式（高权限向下兼容）
     * 01：仅个人；02：个人 + 部门；03：全部；空或未知：仅全部部门导入（保持现状）
     *
     * @param role 账号ROLE字段
     * @return 可见的导入模式集合，顺序即Tab顺序
     */
    public static List<Integer> getVisibleModes(String role) {
        List<Integer> modes = new ArrayList<>();
        if (ROLE_IMPORT_ALL.equals(role)) {
            modes.add(MODE_PERSONAL);
            modes.add(MODE_DEPARTMENT);
            modes.add(MODE_ALL);
        } else if (ROLE_IMPORT_DEPARTMENT.equals(role)) {
            modes.add(MODE_PERSONAL);
            modes.add(MODE_DEPARTMENT);
        } else if (ROLE_IMPORT_PERSONAL.equals(role)) {
            modes.add(MODE_PERSONAL);
        } else {
            modes.add(MODE_ALL);
        }
        return modes;
    }
}