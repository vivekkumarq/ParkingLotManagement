package com.netcracker.parkinglotmanagement.service.rsql;

import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns an RSQL expression into a jOOQ {@link Condition}.
 *
 * <p>The previous implementation produced a JPA {@code Specification} over a DTO
 * class that is not an entity, against repositories that are written in jOOQ - so
 * it could never have been executed, and the caller silently discarded it. This
 * version targets the query language the repositories actually speak.
 *
 * <p>Two properties matter for safety:
 *
 * <ul>
 *   <li>Selectors are resolved through a caller-supplied whitelist of
 *       {@link Field}s. A name that is not in the map is rejected rather than
 *       interpolated, so an attacker cannot reach a column - or a SQL fragment -
 *       that the endpoint did not intend to expose.</li>
 *   <li>Values are always attached with {@link DSL#val}, i.e. as bind parameters.
 *       Nothing from the request is ever concatenated into SQL text.</li>
 * </ul>
 *
 * <p>Supported operators: {@code == != =gt= =ge= =lt= =le= =in= =out=}, plus
 * {@code ;} (and) and {@code ,} (or). In an {@code ==} or {@code !=} comparison on
 * a text column, {@code *} acts as a wildcard and the comparison becomes a
 * {@code LIKE}.
 */
public class CustomRsqlVisitor implements RSQLVisitor<Condition, Void> {

    private static final String EQUAL = "==";
    private static final String NOT_EQUAL = "!=";
    private static final String GREATER_THAN = "=gt=";
    private static final String GREATER_THAN_OR_EQUAL = "=ge=";
    private static final String LESS_THAN = "=lt=";
    private static final String LESS_THAN_OR_EQUAL = "=le=";
    private static final String IN = "=in=";
    private static final String NOT_IN = "=out=";

    private final Map<String, Field<?>> selectableFields;

    /**
     * @param selectableFields the only column names this expression may refer to,
     *                         keyed by the name clients use in the query string
     */
    public CustomRsqlVisitor(Map<String, Field<?>> selectableFields) {
        this.selectableFields = selectableFields;
    }

    @Override
    public Condition visit(AndNode node, Void param) {
        List<Condition> children = visitChildren(node.getChildren(), param);
        return children.isEmpty() ? DSL.noCondition() : DSL.and(children);
    }

    @Override
    public Condition visit(OrNode node, Void param) {
        List<Condition> children = visitChildren(node.getChildren(), param);
        return children.isEmpty() ? DSL.noCondition() : DSL.or(children);
    }

    @Override
    public Condition visit(ComparisonNode node, Void param) {
        Field<?> field = resolve(node.getSelector());
        ComparisonOperator operator = node.getOperator();
        List<String> arguments = node.getArguments();

        if (arguments.isEmpty()) {
            throw new InvalidRequestException(
                    "RSQL operator " + operator.getSymbol() + " on '" + node.getSelector()
                            + "' needs at least one argument");
        }

        String symbol = operator.getSymbol();
        switch (symbol) {
            case EQUAL:
                return isWildcard(field, arguments.get(0))
                        ? textField(field).like(toLikePattern(arguments.get(0)))
                        : anyField(field).eq(coerce(field, arguments.get(0)));
            case NOT_EQUAL:
                return isWildcard(field, arguments.get(0))
                        ? textField(field).notLike(toLikePattern(arguments.get(0)))
                        : anyField(field).ne(coerce(field, arguments.get(0)));
            case GREATER_THAN:
                return anyField(field).greaterThan(coerce(field, arguments.get(0)));
            case GREATER_THAN_OR_EQUAL:
                return anyField(field).greaterOrEqual(coerce(field, arguments.get(0)));
            case LESS_THAN:
                return anyField(field).lessThan(coerce(field, arguments.get(0)));
            case LESS_THAN_OR_EQUAL:
                return anyField(field).lessOrEqual(coerce(field, arguments.get(0)));
            case IN:
                return anyField(field).in(coerceAll(field, arguments));
            case NOT_IN:
                return anyField(field).notIn(coerceAll(field, arguments));
            default:
                throw new InvalidRequestException("Unsupported RSQL operator: " + symbol);
        }
    }

    private List<Condition> visitChildren(List<? extends cz.jirutka.rsql.parser.ast.Node> nodes, Void param) {
        List<Condition> conditions = new ArrayList<>(nodes.size());
        for (cz.jirutka.rsql.parser.ast.Node child : nodes) {
            conditions.add(child.accept(this, param));
        }
        return conditions;
    }

    private Field<?> resolve(String selector) {
        Field<?> field = selectableFields.get(selector);
        if (field == null) {
            throw new InvalidRequestException(
                    "Unknown filter field '" + selector + "'. Filterable fields: "
                            + String.join(", ", selectableFields.keySet()));
        }
        return field;
    }

    private static boolean isWildcard(Field<?> field, String argument) {
        return CharSequence.class.isAssignableFrom(field.getType()) && argument.indexOf('*') >= 0;
    }

    private static String toLikePattern(String argument) {
        return argument.replace('*', '%');
    }

    @SuppressWarnings("unchecked")
    private static Field<Object> anyField(Field<?> field) {
        return (Field<Object>) field;
    }

    @SuppressWarnings("unchecked")
    private static Field<String> textField(Field<?> field) {
        return (Field<String>) field;
    }

    /**
     * Converts the textual argument to the column's own Java type, so that a numeric
     * or timestamp comparison is not silently done as a string comparison - the bug
     * that made {@code =gt=} and {@code =lt=} meaningless in the original visitor,
     * which cast every path to String before comparing.
     */
    private static Object coerce(Field<?> field, String argument) {
        DataType<?> dataType = field.getDataType();
        Object converted;
        try {
            converted = dataType.convert(argument);
        } catch (RuntimeException e) {
            throw new InvalidRequestException(
                    "Value '" + argument + "' is not valid for field '" + field.getName() + "'");
        }
        // jOOQ's converter answers null for a value it cannot parse rather than
        // throwing. Left alone that would turn `totalCost=gt=abc` into `> null`,
        // which matches nothing and looks like an empty result rather than the bad
        // request it is.
        if (converted == null && !argument.isEmpty()) {
            throw new InvalidRequestException(
                    "Value '" + argument + "' is not valid for field '" + field.getName() + "'");
        }
        return converted;
    }

    private static List<Object> coerceAll(Field<?> field, List<String> arguments) {
        List<Object> values = new ArrayList<>(arguments.size());
        for (String argument : arguments) {
            values.add(coerce(field, argument));
        }
        return values;
    }
}
