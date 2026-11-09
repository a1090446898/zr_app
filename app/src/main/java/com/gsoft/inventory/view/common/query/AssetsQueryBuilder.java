package com.gsoft.inventory.view.common.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 资产查询条件构建器：统一拼装 where 与绑定参数。
 * <p>
 * 约束：
 * 1. 字段名只允许字母、数字、下划线，防止调用方把 SQL 片段塞进字段名；
 * 2. 所有值都使用占位符参数绑定，不做字符串拼接；
 * 3. 空值条件自动忽略，避免生成 {@code field = ''} 这类误过滤。
 */
public class AssetsQueryBuilder {

    private final StringBuilder where = new StringBuilder(" Id > 0 ");
    private final List<String> args = new ArrayList<>();

    /**
     * 追加一个受控条件
     */
    public AssetsQueryBuilder andCondition(AssetsQueryCondition condition) {
        if (condition != null) {
            condition.appendTo(this);
        }
        return this;
    }

    public AssetsQueryBuilder andEquals(String field, String value) {
        String normalized = normalizeValue(value);
        if (normalized == null) {
            return this;
        }
        where.append(" and ").append(safeField(field)).append(" = ? ");
        args.add(normalized);
        return this;
    }

    public AssetsQueryBuilder andNotEquals(String field, String value) {
        String normalized = normalizeValue(value);
        if (normalized == null) {
            return this;
        }
        where.append(" and ").append(safeField(field)).append(" <> ? ");
        args.add(normalized);
        return this;
    }

    /**
     * 不等于，NULL 按空串处理，避免历史 NULL 数据被错误排除
     */
    public AssetsQueryBuilder andNotEqualsIgnoringNull(String field, String value) {
        String normalized = normalizeValue(value);
        if (normalized == null) {
            return this;
        }
        where.append(" and IFNULL(").append(safeField(field)).append(",'') <> ? ");
        args.add(normalized);
        return this;
    }

    public AssetsQueryBuilder andLike(String field, String value) {
        String normalized = normalizeValue(value);
        if (normalized == null) {
            return this;
        }
        where.append(" and ").append(safeField(field)).append(" LIKE ? ");
        args.add("%" + normalized + "%");
        return this;
    }

    public AssetsQueryBuilder andIn(String field, Collection<String> values) {
        List<String> normalized = normalizeValues(values);
        if (normalized.isEmpty()) {
            return this;
        }
        where.append(" and ").append(safeField(field)).append(" in (")
                .append(placeholders(normalized.size())).append(") ");
        args.addAll(normalized);
        return this;
    }

    public AssetsQueryBuilder andNotIn(String field, Collection<String> values) {
        List<String> normalized = normalizeValues(values);
        if (normalized.isEmpty()) {
            return this;
        }
        where.append(" and ").append(safeField(field)).append(" not in (")
                .append(placeholders(normalized.size())).append(") ");
        args.addAll(normalized);
        return this;
    }

    /**
     * 同一字段多个值取 OR，用于“部门编号/部门名称/单位编号”这类历史数据兼容
     */
    public AssetsQueryBuilder andAnyOf(String field, Collection<String> values) {
        List<String> normalized = normalizeValues(values);
        if (normalized.isEmpty()) {
            return this;
        }
        String safeField = safeField(field);
        where.append(" and (").append(safeField).append(" = ? ");
        for (int i = 1; i < normalized.size(); i++) {
            where.append(" or ").append(safeField).append(" = ? ");
        }
        where.append(") ");
        args.addAll(normalized);
        return this;
    }

    public AssetsQueryBuilder andIsNullOrEmpty(String field) {
        where.append(" and IFNULL(").append(safeField(field)).append(",'') = '' ");
        return this;
    }

    /**
     * 恒假条件：账套为空等异常场景下保证查不到任何数据
     */
    public AssetsQueryBuilder andFalse() {
        where.append(" and 1=0 ");
        return this;
    }

    public String getWhere() {
        return where.toString();
    }

    public String[] getArgs() {
        return args.toArray(new String[0]);
    }

    private static String safeField(String field) {
        if (field == null || !field.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("非法的查询字段：" + field);
        }
        return field;
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> normalizeValues(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeValue(value);
            if (normalized != null) {
                unique.add(normalized);
            }
        }
        return new ArrayList<>(unique);
    }

    private static String placeholders(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("?");
        }
        return builder.toString();
    }
}
