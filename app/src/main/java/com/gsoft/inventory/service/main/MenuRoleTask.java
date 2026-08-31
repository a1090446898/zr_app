package com.gsoft.inventory.service.main;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.pojo.RoleMenu;
import com.gsoft.inventory.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

public class MenuRoleTask implements Callable<List<RoleMenu>> {
    private String userId;

    public MenuRoleTask(String userId) {
        this.userId = userId;
    }

    @Override
    public List<RoleMenu> call() throws Exception {

        List<RoleMenu> roleMenus = new ArrayList<>();

        String url = String.format(SysDefine.getMenuRole(), userId);
        String result = OkHttpUtils.getInstance().get(url);

        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            return roleMenus;
        }
        try {
            List<List<String>>results = gson.fromJson(result, new TypeToken<List<List<String>>>() {
            }.getType());
            // 2. 遍历解析结果，转换为RoleMenu对象
            for (List<String> menuItem : results) {
                // 确保 inner list 至少有2个元素（菜单名称和状态）
                if (menuItem.size() >= 2) {
                    RoleMenu roleMenu = new RoleMenu();
                    roleMenu.setMenuName(menuItem.get(0));
                    // 状态："true"转为boolean
                    String status = menuItem.get(1);
                    if("false".equals(status)){
                        roleMenu.setHasRole(false);
                    }else{
                        roleMenu.setHasRole(true);
                    }
                    roleMenus.add(roleMenu);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return roleMenus;

    }
}
