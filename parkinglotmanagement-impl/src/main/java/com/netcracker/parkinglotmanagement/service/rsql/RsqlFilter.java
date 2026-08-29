package com.netcracker.parkinglotmanagement.service.rsql;

import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Map;

/** Parses an RSQL query string into a jOOQ {@link Condition}. */
public final class RsqlFilter {

    private static final RSQLParser PARSER = new RSQLParser();

    private RsqlFilter() {
    }

    /**
     * @param rsql             the {@code search} query parameter; null or blank means "no filter"
     * @param selectableFields the columns the expression may refer to
     * @return a condition, or {@link DSL#noCondition()} when nothing was requested
     * @throws InvalidRequestException when the expression is malformed or names an
     *                                 unknown field
     */
    public static Condition toCondition(String rsql, Map<String, Field<?>> selectableFields) {
        if (rsql == null || rsql.trim().isEmpty()) {
            return DSL.noCondition();
        }
        Node rootNode;
        try {
            rootNode = PARSER.parse(rsql);
        } catch (RSQLParserException e) {
            throw new InvalidRequestException("Malformed RSQL filter: " + rsql);
        }
        return rootNode.accept(new CustomRsqlVisitor(selectableFields));
    }
}
