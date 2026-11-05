package com.gsoft.inventory.const_data;

/**
 * @author 10904
 */
public interface Const {

    /**
     * 未上传
     */
    String NOT_UPLOAD = "02";

    /**
     * 已上传
     */
    String UPLOADED = "01";


    /**
     * 未打印
     */
    String NOT_PRINT = "01";

    /**
     * 已打印
     */
    String PRINTED = "02";

    /**
     * 空白地点：盘点统计页每个部门存放地点列表首位固定展示的虚拟地点入口，
     * 扫描不限制部门与存放地点，扫到资产后可手动指定实际存放地点
     */
    String BACKUP_LOCATION_NAME = "空白地点";

    /*--------------------------------- 批量复制 ---------------------------------*/

    /**
     * 批量复制标识：仅本地使用，不参与上传。
     * "1" 表示由批量复制生成，清空列表时置为空字符串。
     * 注意：该字段不能加入 R.array.assets_fields，否则会导致上传报文错位。
     */
    String IS_COPY_DATA = "1";

    /**
     * 批量复制标识被清空后的值
     */
    String IS_COPY_DATA_CLEARED = "";

    /**
     * 单条资产最大复制次数
     */
    int BATCH_COPY_TIMES_MAX = 999;

    /**
     * 单次复制生成总量上限
     */
    int BATCH_COPY_TOTAL_MAX = 2000;

    /**
     * 单次查询源资产上限
     */
    int BATCH_COPY_QUERY_LIMIT = 500;

    /**
     * 批量复制列表分页大小
     */
    int BATCH_COPY_PAGE_SIZE = 50;

    /**
     * 批量复制分批落库大小
     */
    int BATCH_COPY_SAVE_BATCH_SIZE = 200;

    /**
     * 单次批量标记删除的最大条数
     */
    int BATCH_COPY_DELETE_MAX = 1000;

    /**
     * 复制数据默认盘点情况：03 盘盈
     */
    String PDZT_PROFIT = "03";

    /**
     * 盘点状态：未盘
     */
    String ZT_NOT_CHECKED = "0";

    /**
     * 复制数据默认备注
     */
    String REMARKS_PROFIT = "盘盈";

    /**
     * 删除标记备注：复用项目既有删除协议，同步由服务端确认后再物理删除
     */
    String REMARKS_TO_DELETE = "待删除";

    /**
     * 是否复制图片：是
     */
    String COPY_IMG_YES = "是";

    /**
     * 是否复制图片：否
     */
    String COPY_IMG_NO = "否";

    /**
     * 图片复制标记：需要复制
     */
    Integer ISUPIMG_NEED = 1;

    /**
     * 图片复制标记：不需要复制
     */
    Integer ISUPIMG_NONE = 0;
}
