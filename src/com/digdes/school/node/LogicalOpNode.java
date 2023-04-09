package com.digdes.school.node;

import com.digdes.school.enums.LogicalOps;


import java.util.Map;

public class LogicalOpNode extends Node{
    LogicalOps LogicalOperator;

    public LogicalOpNode(LogicalOps logicalOperator,Node left, Node right) {
        super(left, right);
        LogicalOperator = logicalOperator;
    }

    @Override
    public boolean compute(Map<String,Object> row) {
        boolean leftResult = getLeft().compute(row);
        boolean rightResult = getRight().compute(row);
        switch (LogicalOperator) {
            case and -> {
                return leftResult && rightResult;
            }
            case or -> {
                return leftResult || rightResult;
            }
            default -> throw new RuntimeException("something went wrong " + LogicalOperator.name());
        }
    }

}
