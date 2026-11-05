
package com.gsoft.inventory.utils.print_legacy;

import android.text.TextUtils;

import com.dothantech.lpapi.LPAPI;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.SysDefine;
import com.gsoft.inventory.utils.domain.Assets;

import java.lang.reflect.Field;

/**
 * 老打印机标签版式基类：统一持有打印机与打印字段数组，并提供反射取值、字段翻译、
 * 二维码地址、提交任务等公共能力。
 * <p>
 * 所有坐标沿用原有毫米单位，绘制结果与原实现保持一致。
 *
 * @author 10904
 */
abstract class LegacyLabelBase {

    protected final LegacyPrintHost host;
    protected final LPAPI printer;
    /** 打印字段编码（与设置页打印字段一致） */
    protected final String[] fieldCodes;
    /** 打印字段中文名（与 fieldCodes 一一对应） */
    protected final String[] fieldTitles;

    LegacyLabelBase(LegacyPrintHost host) {
        this.host = host;
        this.printer = host.getLegacyPrinter();
        this.fieldCodes = host.getHostContext().getResources().getStringArray(R.array.printfieldsCode);
        this.fieldTitles = host.getHostContext().getResources().getStringArray(R.array.printfieldsTitle);
    }

    /** 字段在打印字段配置中的下标，未配置返回 -1 */
    protected int indexOfField(String fieldName) {
        if (TextUtils.isEmpty(fieldName) || fieldCodes == null) {
            return -1;
        }
        for (int i = 0; i < fieldCodes.length; i++) {
            if (fieldName.equals(fieldCodes[i])) {
                return i;
            }
        }
        return -1;
    }

    /** 反射读取字段原始值，取不到返回空串（父类字段一并查找） */
    protected String readFieldValue(Assets assets, String fieldName) {
        Object value = readField(assets, fieldName);
        return value == null ? "" : String.valueOf(value);
    }

    /** 反射读取字段值并做业务转换：部门编码转名称、是否标志转文字 */
    protected String readDisplayValue(Assets assets, String fieldName) {
        String value = readFieldValue(assets, fieldName);
        if ("GROUNDMANAGEDEPT".equals(fieldName)) {
            value = AccountHelper.getDanweiName(host.getAccountBookCode(), value);
        } else if ("ISCZ".equals(fieldName)) {
            value = "02".equals(value) ? "是" : "否";
        }
        return value;
    }

    private Object readField(Assets assets, String fieldName) {
        if (assets == null || TextUtils.isEmpty(fieldName)) {
            return "";
        }
        Class<?> clazz = assets.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(assets);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                return "";
            }
        }
        return "";
    }

    /** 资产网页二维码地址（编辑查询页） */
    protected String queryAssetUrl(Assets assets) {
        return String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s",
                SysDefine.SERVERHOST, assets.getACCTSUITEID(), assets.getBARCODEID());
    }

    /** 提交打印任务 */
    protected boolean commit() {
        return printer.commitJob();
    }

    /** 提交并提示结果（大多数版式的收尾一致） */
    protected boolean commitWithTip() {
        boolean success = commit();
        host.showPrintTip(success ? "打印任务提交成功，等待打印机执行" : "打印任务提交失败，请检查打印机状态");
        return success;
    }

    /** 字段标签补空格到四字宽（大二维码-五字段版式专用） */
    protected String padLabel(String input) {
        if (input == null || input.length() > 3) {
            return input;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(input.charAt(i));
            if (i < 2) {
                sb.append("  ");
            }
        }
        if (input.length() == 1 || input.length() == 2) {
            sb.append("    ");
        }
        return sb.toString();
    }
}