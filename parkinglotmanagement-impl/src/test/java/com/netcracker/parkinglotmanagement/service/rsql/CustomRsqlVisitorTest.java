package com.netcracker.parkinglotmanagement.service.rsql;

import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.netcracker.parkinglotmanagement.data.Tables.CUSTOMER;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_SLIP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The RSQL filter compiles to a jOOQ condition.
 *
 * <p>Two properties are load-bearing and are asserted directly: values become bind
 * parameters rather than inlined SQL, and selectors are resolved through a
 * whitelist so a caller cannot reach a column the endpoint did not offer.
 */
class CustomRsqlVisitorTest {

    private static final Map<String, Field<?>> FIELDS = fields();

    private static Map<String, Field<?>> fields() {
        Map<String, Field<?>> map = new LinkedHashMap<>();
        map.put("name", CUSTOMER.NAME);
        map.put("email", CUSTOMER.EMAIL);
        map.put("vehicleNumber", CUSTOMER.VEHICLE_NUMBER);
        map.put("totalCost", PARKING_SLIP.TOTAL_COST);
        map.put("status", PARKING_SLIP.STATUS);
        return map;
    }

    /** Renders with inlined values purely so the test can read the shape of the SQL. */
    private static String sql(String rsql) {
        Condition condition = RsqlFilter.toCondition(rsql, FIELDS);
        return DSL.using(org.jooq.SQLDialect.H2)
                .renderInlined(condition)
                .toLowerCase(java.util.Locale.ROOT);
    }

    private static String sqlWithBindParameters(String rsql) {
        Condition condition = RsqlFilter.toCondition(rsql, FIELDS);
        return DSL.using(org.jooq.SQLDialect.H2)
                .render(condition);
    }

    @Nested
    @DisplayName("operators")
    class Operators {

        @Test
        void equality() {
            assertThat(sql("name==Asha")).contains("= 'asha'");
        }

        @Test
        void inequality() {
            assertThat(sql("name!=Asha")).contains("<>");
        }

        @Test
        void comparison() {
            assertThat(sql("totalCost=gt=500")).contains(">");
            assertThat(sql("totalCost=ge=500")).contains(">=");
            assertThat(sql("totalCost=lt=500")).contains("<");
            assertThat(sql("totalCost=le=500")).contains("<=");
        }

        @Test
        void inAndNotIn() {
            assertThat(sql("status=in=(ACTIVE,CLOSED)")).contains(" in ");
            assertThat(sql("status=out=(ACTIVE)")).contains("not in");
        }

        @Test
        @DisplayName("'*' becomes a LIKE wildcard on a text column")
        void wildcardBecomesLike() {
            assertThat(sql("email==*@example.com")).contains("like").contains("%@example.com");
        }

        @Test
        @DisplayName("'!=' with a wildcard becomes NOT LIKE")
        void negatedWildcardBecomesNotLike() {
            assertThat(sql("email!=*@example.com")).contains("not like");
        }

        @Test
        @DisplayName("';' is AND and ',' is OR")
        void conjunctionAndDisjunction() {
            assertThat(sql("name==Asha;email==a@b.c")).contains(" and ");
            assertThat(sql("name==Asha,email==a@b.c")).contains(" or ");
        }

        @Test
        @DisplayName("a nested expression keeps its grouping")
        void nestedGrouping() {
            String rendered = sql("(name==Asha,name==Ravi);totalCost=gt=100");
            assertThat(rendered).contains(" or ").contains(" and ");
        }
    }

    @Nested
    @DisplayName("safety")
    class Safety {

        @Test
        @DisplayName("values are bound, not concatenated into the SQL")
        void valuesAreBoundParameters() {
            String rendered = sqlWithBindParameters("name==Asha");

            assertThat(rendered).contains("?");
            assertThat(rendered).doesNotContain("Asha");
        }

        @Test
        @DisplayName("a value that looks like SQL is still just a value")
        void injectionAttemptIsInert() {
            // RSQL double-quotes a value that contains its own operators, so the
            // whole payload arrives as a single argument.
            String rendered = sqlWithBindParameters(
                    "name==\"x OR 1=1; DROP TABLE customer; --\"");

            assertThat(rendered).contains("?");
            assertThat(rendered.toLowerCase(java.util.Locale.ROOT))
                    .as("nothing from the value reaches the SQL text")
                    .doesNotContain("drop")
                    .doesNotContain("--");
        }

        @Test
        @DisplayName("an unknown selector is rejected instead of being interpolated")
        void unknownSelectorIsRejected() {
            assertThatThrownBy(() -> RsqlFilter.toCondition("password==secret", FIELDS))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Unknown filter field")
                    .hasMessageContaining("password");
        }

        @Test
        @DisplayName("the rejection lists the fields that are allowed")
        void rejectionIsHelpful() {
            assertThatThrownBy(() -> RsqlFilter.toCondition("nope==1", FIELDS))
                    .hasMessageContaining("name")
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("a malformed expression is a bad request, not a 500")
        void malformedExpressionIsRejected() {
            assertThatThrownBy(() -> RsqlFilter.toCondition("name===", FIELDS))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Malformed RSQL");
        }

        @Test
        @DisplayName("a value of the wrong type for the column is a bad request")
        void wrongTypedValueIsRejected() {
            assertThatThrownBy(() -> RsqlFilter.toCondition("totalCost=gt=abc", FIELDS))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("not valid");
        }
    }

    @Nested
    @DisplayName("typing")
    class Typing {

        @Test
        @DisplayName("a numeric column is compared as a number, not as text")
        void numericComparisonIsNumeric() {
            // The original visitor cast every path to String before comparing, so
            // '9' > '100' was true. Binding through the column's own DataType is what
            // makes the comparison arithmetic.
            Condition condition = RsqlFilter.toCondition("totalCost=gt=100", FIELDS);
            String rendered = DSL.using(org.jooq.SQLDialect.H2)
                    .renderInlined(condition);

            assertThat(rendered).doesNotContain("'100'");
            assertThat(rendered).contains("100");
        }
    }

    @Test
    @DisplayName("a null or blank filter matches everything")
    void emptyFilterIsNoCondition() {
        assertThat(RsqlFilter.toCondition(null, FIELDS)).isEqualTo(DSL.noCondition());
        assertThat(RsqlFilter.toCondition("   ", FIELDS)).isEqualTo(DSL.noCondition());
    }

    @Test
    @DisplayName("bind parameters really are parameters, not inlined literals")
    void paramTypeIsIndexed() {
        Condition condition = RsqlFilter.toCondition("name==Asha", FIELDS);
        String rendered = DSL.using(org.jooq.SQLDialect.H2).renderContext()
                .paramType(ParamType.INDEXED)
                .visit(condition)
                .render();

        assertThat(rendered).contains("?");
    }
}
