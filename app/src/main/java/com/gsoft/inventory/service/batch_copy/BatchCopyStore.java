package com.gsoft.inventory.service.batch_copy;

import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量复制数据仓库：列表查询、清空标识、标记删除
 *
 * @author 10904
 */
public class BatchCopyStore {

    /** 列表查询条件：本地标识 + 账套 */
    private static final String WHERE_COPY_LIST = "IS_COPY_DATA=? AND ACCTSUITEID=?";

    /** 固定倒序：最新复制的排在最前 */
    private static final String ORDER_BY_DESC = "Id DESC";

    /** 补列只做一次，避免每次查询都触发异常 */
    private static boolean copyColumnReady = false;

    private BatchCopyStore() {
    }

    /**
     * 自愈：确保 ASSETS 表存在 IS_COPY_DATA 列，并把旧列名里的数据迁移过来。
     * <p>
     * 该列依赖 v65 升级脚本，历史库若没有执行到升级脚本，查询会报
     * "no such column: IS_COPY_DATA"，前端表现为列表加载失败；
     * 列已存在时 SQLite 抛 duplicate column name，忽略即可。
     * <p>
     * 早期版本没有给字段指定列名，SugarORM 会把 IS_COPY_DATA 映射成 ISCOPYDATA，
     * 那一列里存着已经复制出来的数据，这里搬回 IS_COPY_DATA。
     */
    public static synchronized void ensureCopyColumn() {
        if (copyColumnReady) {
            return;
        }
        try {
            Assets.executeQuery("ALTER TABLE ASSETS ADD COLUMN IS_COPY_DATA varchar(10) default ''");
        } catch (Exception ignored) {
            // 列已存在，属于正常情况
        }
        try {
            // 一次性搬运：同时清空旧列，避免后续“清空列表”后又被旧值覆盖
            Assets.executeQuery("UPDATE ASSETS SET IS_COPY_DATA = ISCOPYDATA, ISCOPYDATA = '' "
                    + "WHERE ISCOPYDATA IS NOT NULL AND ISCOPYDATA <> ''");
        } catch (Exception ignored) {
            // 没有 ISCOPYDATA 列属于正常情况：新装库或从未用旧列名写过数据
        }
        try {
            Assets.executeQuery("CREATE INDEX IF NOT EXISTS idx_assets_copy_data "
                    + "ON ASSETS(IS_COPY_DATA, ACCTSUITEID, Id)");
        } catch (Exception ignored) {
        }
        copyColumnReady = true;
    }

    /**
     * 分页查询当前账套的复制数据，按 Id 倒序
     */
    public static List<Assets> queryCopyAssets(String ztbH, int offset, int limit) {
        if (StringUtils.isNullOrEmpty(ztbH) || limit <= 0) {
            return new ArrayList<>();
        }
        ensureCopyColumn();
        String limitClause = Math.max(0, offset) + ", " + limit;
        List<Assets> result = Assets.find(Assets.class, WHERE_COPY_LIST,
                new String[]{Const.IS_COPY_DATA, ztbH}, null, ORDER_BY_DESC, limitClause);
        return result == null ? new ArrayList<>() : result;
    }

    /**
     * 统计当前账套的复制数据条数
     */
    public static long countCopyAssets(String ztbH) {
        if (StringUtils.isNullOrEmpty(ztbH)) {
            return 0;
        }
        ensureCopyColumn();
        return Assets.count(Assets.class, WHERE_COPY_LIST,
                new String[]{Const.IS_COPY_DATA, ztbH});
    }

    /**
     * 清空当前账套的复制标识：只改标识，不删除数据，也不影响任何状态字段
     *
     * @return 被清空的条数
     */
    public static synchronized int clearCopyFlag(String ztbH) {
        if (StringUtils.isNullOrEmpty(ztbH)) {
            return 0;
        }
        long count = countCopyAssets(ztbH);
        if (count <= 0) {
            return 0;
        }
        Assets.executeQuery("UPDATE ASSETS SET IS_COPY_DATA = ? WHERE IS_COPY_DATA = ? AND ACCTSUITEID = ?",
                Const.IS_COPY_DATA_CLEARED, Const.IS_COPY_DATA, ztbH);
        return (int) count;
    }

    /**
     * 批量标记删除：REMARKS 置为"待删除"、ISUPLOAD 置为未上传，
     * 由同步任务统一提交，服务端确认后本地才物理删除
     *
     * @return 提交的条数
     */
    public static synchronized int markDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("UPDATE ASSETS SET REMARKS = ?, ISUPLOAD = ? WHERE Id IN (");
        String[] args = new String[ids.size() + 2];
        args[0] = Const.REMARKS_TO_DELETE;
        args[1] = Const.NOT_UPLOAD;
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
            args[i + 2] = String.valueOf(ids.get(i));
        }
        sql.append(")");
        Assets.executeQuery(sql.toString(), args);
        return ids.size();
    }
}