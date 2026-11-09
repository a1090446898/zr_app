package com.gsoft.inventory.view.common.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 资产选择页的受控查询条件。
 * <p>
 * 只描述“字段 + 操作符 + 参数”，SQL 由 {@link AssetsQueryBuilder} 统一拼装；
 * 调用方不需要、也不允许直接传原始 SQL，避免漏加账套条件或出现拼接错误。
 */
public class AssetsQueryCondition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条件操作符
     */
    public enum Operator {
        /** 等于 */
        EQ,
        /** 不等于（NULL 不参与比较） */
        NOT_EQ,
        /** 不等于，NULL 按空串处理（IFNULL(field,'') <> ?） */
        NOT_EQ_IGNORING_NULL,
        /** 模糊匹配，自动补 % */
        LIKE,
        /** IN */
        IN,
        /** NOT IN */
        NOT_IN,
        /** 同一字段的多个值取 OR，例如管理部门兼容编码/名称/单位编码 */
        ANY_OF,
        /** 字段为 NULL 或空串 */
        IS_NULL_OR_EMPTY
    }

    private final String field;
    private final Operator operator;
    private final List<String> values;

    private AssetsQueryCondition(String field, Operator operator, List<String> values) {
        this.field = field;
        this.operator = operator;
        this.values = values;
    }

    public static AssetsQueryCondition eq(String field, String value) {
        return new AssetsQueryCondition(field, Operator.EQ, single(value));
    }

    public static AssetsQueryCondition notEq(String field, String value) {
        return new AssetsQueryCondition(field, Operator.NOT_EQ, single(value));
    }

    /**
     * 不等于，但把 NULL 当作空串参与比较。
     * <p>
     * 用于盘点状态这类字段：历史数据可能是 NULL，业务上应视为“未盘”，
     * 直接写 {@code PDZT <> '01'} 会把 NULL 行排除掉。
     */
    public static AssetsQueryCondition notEqIgnoringNull(String field, String value) {
        return new AssetsQueryCondition(field, Operator.NOT_EQ_IGNORING_NULL, single(value));
    }

    public static AssetsQueryCondition like(String field, String value) {
        return new AssetsQueryCondition(field, Operator.LIKE, single(value));
    }

    public static AssetsQueryCondition in(String field, String... values) {
        return in(field, values == null ? null : Arrays.asList(values));
    }

    public static AssetsQueryCondition in(String field, Collection<String> values) {
        return new AssetsQueryCondition(field, Operator.IN, normalize(values));
    }

    public static AssetsQueryCondition notIn(String field, String... values) {
        return notIn(field, values == null ? null : Arrays.asList(values));
    }

    public static AssetsQueryCondition notIn(String field, Collection<String> values) {
        return new AssetsQueryCondition(field, Operator.NOT_IN, normalize(values));
    }

    public static AssetsQueryCondition anyOf(String field, String... values) {
        return anyOf(field, values == null ? null : Arrays.asList(values));
    }

    public static AssetsQueryCondition anyOf(String field, Collection<String> values) {
        return new AssetsQueryCondition(field, Operator.ANY_OF, normalize(values));
    }

    public static AssetsQueryCondition isNullOrEmpty(String field) {
        return new AssetsQueryCondition(field, Operator.IS_NULL_OR_EMPTY, Collections.<String>emptyList());
    }

    public String getField() {
        return field;
    }

    public Operator getOperator() {
        return operator;
    }

    public List<String> getValues() {
        return values;
    }

    /**
     * 把当前条件追加到构建器；空值条件由构建器自动忽略
     */
    void appendTo(AssetsQueryBuilder builder) {
        if (builder == null) {
            return;
        }
        switch (operator) {
            case EQ:
                builder.andEquals(field, firstValue());
                break;
            case NOT_EQ:
                builder.andNotEquals(field, firstValue());
                break;
            case NOT_EQ_IGNORING_NULL:
                builder.andNotEqualsIgnoringNull(field, firstValue());
                break;
            case LIKE:
                builder.andLike(field, firstValue());
                break;
            case IN:
                builder.andIn(field, values);
                break;
            case NOT_IN:
                builder.andNotIn(field, values);
                break;
            case ANY_OF:
                builder.andAnyOf(field, values);
                break;
            case IS_NULL_OR_EMPTY:
                builder.andIsNullOrEmpty(field);
                break;
            default:
                break;
        }
    }

    private String firstValue() {
        return values.isEmpty() ? null : values.get(0);
    }

    private static List<String> single(String value) {
        String normalized = normalizeValue(value);
        if (normalized == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(normalized);
    }

    private static List<String> normalize(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
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

    private static String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
