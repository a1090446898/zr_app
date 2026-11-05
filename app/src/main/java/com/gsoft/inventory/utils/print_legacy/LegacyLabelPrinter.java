
package com.gsoft.inventory.utils.print_legacy;

import com.gsoft.inventory.utils.domain.Assets;

/**
 * 老打印机标签打印唯一入口：按打印效果分发到对应版式。
 * <p>
 * 新增一种老打印机标签只需两步：
 * 1. 在 strings.xml 的 printe_effect 中登记效果名并定义本类常量；
 * 2. 在 print 方法里加一个 case。
 * PrintCompatActivity 与各业务页面无需改动。
 *
 * @author 10904
 */
public final class LegacyLabelPrinter {

    public static final String EFFECT_BIG_QR = "大二维码";
    public static final String EFFECT_BIG_BARCODE = "大一维码";
    public static final String EFFECT_SMALL_BARCODE = "小一维码";
    public static final String EFFECT_SMALL_QR = "小二维码";
    public static final String EFFECT_WEB_QR_4 = "网页二维码-四字段";
    public static final String EFFECT_WEB_QR_2 = "网页二维码-二字段";
    public static final String EFFECT_WEB_QR_TEMPLATE_3 = "网页二维码-模板三";
    public static final String EFFECT_FISCAL_QR = "财政二维码";
    public static final String EFFECT_BIG_QR_FIVE = "大二维码-五字段";
    public static final String EFFECT_DENSE = "大二维码-密级标签";
    public static final String EFFECT_REAGENT = "试剂二维码-四字段";
    public static final String EFFECT_BIG_FIVE = "大标签-五字段";
    public static final String EFFECT_POLICE_WARNING = "公安警告标签";

    private LegacyLabelPrinter() {
    }

    /**
     * 按打印效果打印资产标签
     *
     * @return true 表示已匹配到版式并发起打印；false 表示该效果未适配
     */
    public static boolean print(LegacyPrintHost host, String effect, String accountName,
                                Assets assets, String[] fieldArray) {
        if (host == null || host.getLegacyPrinter() == null || assets == null || effect == null) {
            return false;
        }
        String[] fields = fieldArray == null ? new String[0] : fieldArray;
        LegacyAssetLabels assetLabels = new LegacyAssetLabels(host);
        LegacyBusinessLabels businessLabels = new LegacyBusinessLabels(host);

        switch (effect) {
            case EFFECT_BIG_QR:
                assetLabels.printBigQr(accountName, assets, fields);
                return true;
            case EFFECT_BIG_BARCODE:
                assetLabels.printBigBarcode(accountName, assets, fields);
                return true;
            case EFFECT_SMALL_BARCODE:
                assetLabels.printSmallBarcode(assets.getBARCODEID());
                return true;
            case EFFECT_SMALL_QR:
                assetLabels.printSmallQr(assets, fields);
                return true;
            case EFFECT_WEB_QR_4:
                assetLabels.printWebQr4(accountName, assets, fields);
                return true;
            case EFFECT_WEB_QR_2:
                assetLabels.printWebQr2(accountName, assets, fields);
                return true;
            case EFFECT_WEB_QR_TEMPLATE_3:
                businessLabels.printWebQrTemplate3(accountName, assets, fields);
                return true;
            case EFFECT_FISCAL_QR:
                businessLabels.printFiscalQr(accountName, assets);
                return true;
            case EFFECT_BIG_QR_FIVE:
                assetLabels.printBigQrFive(accountName, assets, fields);
                return true;
            case EFFECT_DENSE:
                businessLabels.printDense(accountName, assets);
                return true;
            case EFFECT_REAGENT:
                businessLabels.printReagent(accountName, assets,
                        new String[]{"试剂名称", "配置浓度", "配  置  人", "配置日期", "有效日期"},
                        new String[]{"ASSETSNAME", "CONCENTRATION", "PREPARER", "ASSETSUSEDATE", "VALIDDATE"});
                return true;
            case EFFECT_BIG_FIVE:
                assetLabels.printBigFive(accountName, assets, fields);
                return true;
            case EFFECT_POLICE_WARNING:
                businessLabels.printPoliceWarning(accountName);
                return true;
            default:
                return false;
        }
    }

    /** 只打二维码（PrintLabelActivity 手工测试用） */
    public static void printRawQr(LegacyPrintHost host, String content) {
        if (host == null || host.getLegacyPrinter() == null) {
            return;
        }
        new LegacyAssetLabels(host).printSmallQrContent(content);
    }

    /** 只打一维码（PrintLabelActivity 手工测试用） */
    public static void printRawBarcode(LegacyPrintHost host, String barCode) {
        if (host == null || host.getLegacyPrinter() == null) {
            return;
        }
        new LegacyAssetLabels(host).printSmallBarcode(barCode);
    }


}