package com.gsoft.inventory.utils.print_router;

import android.content.Context;

import com.gsoft.inventory.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 打印标签目录：按打印机品牌把"打印效果(标签名称)"分组，供标签选择页的横向Tab使用。
 * <p>
 * 分组规则与 PrintEffectRouter 的品牌映射保持一致（单一数据源）：
 * 1. 登记为 BRAND_BW（宝威）→ "宝威"Tab；
 * 2. 登记为 BRAND_DS（得实新型号）→ "得实"Tab（当前暂无标签，空态展示）；
 * 3. 其余（含未登记，默认老得实/精臣）→ "通用"Tab。
 * <p>
 * 新增标签仍只需在 strings.xml 的 printe_effect 中加一项，并在 PrintEffectRouter 登记品牌，
 * 本类与标签选择页均无需修改。
 */
public final class PrintLabelCatalog {

    /** Tab：通用（老打印机标签，除RFID外全部） */
    public static final String GROUP_COMMON = "通用";
    /** Tab：宝威（当前为RFID标签） */
    public static final String GROUP_BW = "宝威";
    /** Tab：得实（新型号RFID标签，暂未适配） */
    public static final String GROUP_DS = "得实";

    /** Tab顺序，与标签选择页的页签下标一一对应 */
    public static final String[] GROUPS = {GROUP_COMMON, GROUP_BW, GROUP_DS};

    private PrintLabelCatalog() {
    }

    /** 标签名称 → 所属Tab：按打印品牌归组，未登记默认归"通用" */
    public static String getGroup(String effectName) {
        String brand = PrintEffectRouter.getBrand(effectName);
        if (PrintEffectRouter.BRAND_BW.equals(brand)) {
            return GROUP_BW;
        }
        if (PrintEffectRouter.BRAND_DS.equals(brand)) {
            return GROUP_DS;
        }
        return GROUP_COMMON;
    }

    /** 标签名称 → Tab下标；名称无效时默认"通用"（新增标签后页签位置无需改代码） */
    public static int getGroupIndex(String effectName) {
        String group = getGroup(effectName);
        for (int i = 0; i < GROUPS.length; i++) {
            if (GROUPS[i].equals(group)) {
                return i;
            }
        }
        return 0;
    }

    /** 查询某Tab下的全部标签（按 strings.xml 中 printe_effect 数组顺序） */
    public static List<String> getLabels(Context context, String group) {
        String[] all = context.getResources().getStringArray(R.array.printe_effect);
        List<String> result = new ArrayList<>();
        for (String name : all) {
            if (group.equals(getGroup(name))) {
                result.add(name);
            }
        }
        return result;
    }
}