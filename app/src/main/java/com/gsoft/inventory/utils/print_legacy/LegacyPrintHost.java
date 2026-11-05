
package com.gsoft.inventory.utils.print_legacy;

import android.content.Context;

import com.dothantech.lpapi.LPAPI;

/**
 * 老打印机标签打印的宿主能力（由 PrintCompatActivity 实现）
 * <p>
 * 版式类只依赖本接口，不直接依赖 Activity，便于单独维护与替换。
 *
 * @author 10904
 */
public interface LegacyPrintHost {

    /** 老打印机实例（精臣/得实 LPAPI，异步打印队列） */
    LPAPI getLegacyPrinter();

    /** 打印过程提示（Toast） */
    void showPrintTip(String message);

    /** 登录账套编号（部门字段翻译用，未登录返回空串） */
    String getAccountBookCode();

    /** 打印位置设置："居中"时标题居中，其他按居左 */
    String getPrintPosition();

    /** 宿主上下文（读打印字段、公安警告文案等设置） */
    Context getHostContext();
}