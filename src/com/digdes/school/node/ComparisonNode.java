package com.digdes.school.node;

import com.digdes.school.enums.Columns;


import java.util.Map;
import java.util.function.BiFunction;

public class ComparisonNode extends Node{
    BiFunction func;
    Columns column;
    Object parsedValue;

    public ComparisonNode(BiFunction func, Columns column,Object value) {
        super(null,null);
        this.func = func;
        this.column = column;
        this.parsedValue = value;
    }

    @Override
    public boolean compute(Map<String,Object> row) {
        if (!row.containsKey(column.name())) {
            return false;
        }

        Object columnValue = row.get(column.name());
        return (boolean) func.apply(columnValue,parsedValue);
    }

}
