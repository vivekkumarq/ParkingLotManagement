package com.netcracker.parkinglotmanagement.web.controller;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.List;

public class CustomRsqlVisitor<T> implements RSQLVisitor<Specification<T>, Void> {
    private static final ComparisonOperator EQUAL = new ComparisonOperator("==");
    private static final ComparisonOperator NOT_EQUAL = new ComparisonOperator("!=");
    private static final ComparisonOperator GREATER_THAN = new ComparisonOperator("=gt=");
    private static final ComparisonOperator LESS_THAN = new ComparisonOperator("=lt=");

    @Override
    public Specification<T> visit(cz.jirutka.rsql.parser.ast.AndNode node, Void param) {
        return node.getChildren().stream()
                .map(child -> child.accept(this, param))
                .reduce(Specification::and)
                .orElse(null);
    }

    @Override
    public Specification<T> visit(cz.jirutka.rsql.parser.ast.OrNode node, Void param) {
        return node.getChildren().stream()
                .map(child -> child.accept(this, param))
                .reduce(Specification::or)
                .orElse(null);
    }

    @Override
    public Specification<T> visit(cz.jirutka.rsql.parser.ast.ComparisonNode node, Void param) {
        return (root, query, criteriaBuilder) -> {
            Path<Object> path = root.get(node.getSelector());
            ComparisonOperator operator = node.getOperator();
            List<String> arguments = node.getArguments();
            String value = arguments.get(0);

            if (operator.equals(EQUAL)) {
                return criteriaBuilder.equal(path, value);
            } else if (operator.equals(NOT_EQUAL)) {
                return criteriaBuilder.notEqual(path, value);
            } else if (operator.equals(GREATER_THAN)) {
                return criteriaBuilder.greaterThan(path.as(String.class), value);
            } else if (operator.equals(LESS_THAN)) {
                return criteriaBuilder.lessThan(path.as(String.class), value);
            }
            return null;
        };
    }
}
