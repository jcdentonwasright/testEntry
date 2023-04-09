package com.digdes.school.utils;

import com.digdes.school.enums.Columns;
import com.digdes.school.enums.LogicalOps;
import com.digdes.school.node.ComparisonNode;
import com.digdes.school.node.LogicalOpNode;
import com.digdes.school.node.Node;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

public final class ParseUtils {
    public static class Wrapper {
        Integer value;

        public Wrapper(Integer value) {
            this.value = value;
        }
    }

    private ParseUtils() {
    }

    public static Node whereExpression(List<String> request, Wrapper wrapper) {
        Node result = parseAnd(request, wrapper);
        if (wrapper.value >= request.size()) {
            return result;
        }
        String token = request.get(wrapper.value);
        while (token.toLowerCase(Locale.ROOT).equals("or")) {
            wrapper.value++;
            result = new LogicalOpNode(LogicalOps.valueOf(token.toLowerCase(Locale.ROOT)), result, parseAnd(request, wrapper));
            if (wrapper.value >= request.size()) {
                break;
            }
            token = request.get(wrapper.value);
        }
        return result;
    }

    private static Node parseAnd(List<String> request, Wrapper wrapper) {
        Node result = parseBraceOrComparison(request, wrapper);
        if (wrapper.value >= request.size()) {
            return result;
        }
        String token = request.get(wrapper.value);
        while (token.toLowerCase(Locale.ROOT).equals("and")) {
            wrapper.value++;
            result = new LogicalOpNode(LogicalOps.valueOf(token.toLowerCase(Locale.ROOT)), result, parseBraceOrComparison(request, wrapper));
            if (wrapper.value >= request.size()) {
                break;
            }
            token = request.get(wrapper.value);
        }
        return result;
    }

    private static Node parseBraceOrComparison(List<String> request, Wrapper wrapper) {
        Node result;
        String token = request.get(wrapper.value);
        if (token.equals("(")) {
            ++wrapper.value;
            result = whereExpression(request, wrapper);
            if (wrapper.value >= request.size() || !request.get(wrapper.value).equals(")")) {
                throw new RuntimeException("incorrect syntax : no closing bracket");
            }
            ++wrapper.value;
        } else {
            result = parseComparison(request, wrapper);
            wrapper.value++;
        }
        return result;
    }

    private static Node parseComparison(List<String> request, Wrapper wrapper) {
        Columns column = Columns.valueOf(request.get(wrapper.value).replaceAll("^'|'$", "").toLowerCase(Locale.ROOT));

        switch (column) {
            case id, age -> {
                BiFunction function = parseIntOperatorToBiFunc(request.get(++wrapper.value));
                Long value = Long.parseLong(request.get(++wrapper.value));
                return new ComparisonNode(function, column, value);
            }
            case cost -> {
                BiFunction function = parseIntOperatorToBiFunc(request.get(++wrapper.value));
                Double value = Double.parseDouble(request.get(++wrapper.value));
                return new ComparisonNode(function, column, value);
            }
            case active -> {
                BiFunction function = parseBooleanToBiFunc(request.get(++wrapper.value));
                Boolean value = Boolean.parseBoolean(request.get(++wrapper.value));
                return new ComparisonNode(function, column, value);
            }
            case lastname -> {
                BiFunction function = parseStrOperatorToBiFunc(request.get(++wrapper.value));
                String value = request.get(++wrapper.value);
                if (!value.matches("'%?[A-zА-д]*%?'")) {
                    throw new RuntimeException("not a lastname column : " + value);
                }
                value = value.replaceAll("^'|'$", "");
                return new ComparisonNode(function, column, value);
            }
            default -> throw new RuntimeException("not a statement :" + column.name() + " ");
        }
    }

    public static String parseValues(Scanner sc, Map<String, Object> row) throws Exception {
        boolean atLeastOneParam = false;
        while (sc.hasNext()) {
            String columnInBraces = sc.next();

            if (columnInBraces.toLowerCase(Locale.ROOT).equals("where")) {
                if (!atLeastOneParam) {
                    throw new Exception("incorrect syntax: no values to UPDATE or INSERT passed");
                }
                return columnInBraces;
            }

            if (columnInBraces.equals(",")) {
                if (!atLeastOneParam) {
                    throw new Exception("incorrect syntax: no values passed");
                }
                columnInBraces = sc.next();
            }

            if (!columnInBraces.matches("'[A-z]*'")) {
                throw new Exception("not a column: " + columnInBraces);
            }

            String column = columnInBraces.replaceAll("^'|'$", "").toLowerCase(Locale.ROOT);

            if (!sc.next().equals("=")) {
                throw new Exception("no '=' symbol");
            }

            Columns currentColumn = Columns.valueOf(column);
            atLeastOneParam = true;
            String value = sc.next().replaceAll("^'|'$", "");

            if (value.equals("null")) {
                row.put(column, null);
                continue;
            }

            if (row.containsKey(column)) {
                throw new Exception("incorrect syntax: same column twice in VALUES request");
            }
            switch (currentColumn) {
                case id -> {
                    Long id = Long.parseLong(value);
                    if (id < 0) {
                        throw new Exception("incorrect value to id: " + value);
                    }
                    row.put(column, id);
                }
                case age -> {
                    Long age = Long.parseLong(value);
                    if (age < 0 || age > 120) {
                        throw new Exception("incorrect value to age: " + value);
                    }
                    row.put(column, age);
                }
                case cost -> {
                    Double cost = Double.parseDouble(value);
                    if (cost < 0) {
                        throw new Exception("incorrect value to cost: " + value);
                    }
                    row.put(column, cost);
                }
                case lastname -> {
                    if (!value.matches("[A-z]*|[А-я]*")) {
                        throw new Exception("incorrect value to lastname: " + value);
                    }
                    row.put(column, value);
                }

                case active -> row.put(column, Boolean.parseBoolean(value));
            }
        }

        return "";
    }

    public static <T extends Comparable<T>> BiFunction<T, T, Boolean> parseIntOperatorToBiFunc(String intOperator) {
        switch (intOperator) {
            case ">" -> {
                return (a, b) -> a.compareTo(b) > 0;
            }
            case "<" -> {
                return (a, b) -> a.compareTo(b) < 0;
            }
            case ">=" -> {
                return (a, b) -> a.compareTo(b) >= 0;
            }

            case "<=" -> {
                return (a, b) -> a.compareTo(b) <= 0;
            }

            case "=" -> {
                return Object::equals;
            }
            case "!=" -> {
                return (a, b) -> {
                    if (a == null) {
                        return true;
                    }
                    return !a.equals(b);
                };
            }

            default -> throw new RuntimeException("operator is not supported by Number type: " + intOperator);
        }
    }

    public static BiFunction<Boolean, Boolean, Boolean> parseBooleanToBiFunc(String logicalOperator) {
        switch (logicalOperator) {
            case "=" -> {
                return Object::equals;
            }

            case "!=" -> {
                return (a, b) -> {
                    if (a == null) {
                        return true;
                    }
                    return !a.equals(b);
                };
            }
            default -> throw new RuntimeException("operator is not supported by Boolean type:" + logicalOperator);
        }
    }

    public static BiFunction<String, String, Boolean> parseStrOperatorToBiFunc(String strOperator) {
        switch (strOperator) {
            case "like" -> {
                return (string, regex) -> {
                    if (regex.indexOf('%') == 0) {
                        regex = regex.replaceFirst("%", ".*");
                    }
                    if (regex.lastIndexOf('%') == regex.length() - 1) {
                        regex = regex.replaceAll("%$", ".*");
                    }
                    return string.matches(regex);
                };
            }

            case "ilike" -> {
                return (string, regex) -> {
                    if (regex.indexOf('%') == 0) {
                        regex = regex.replaceFirst("%", ".*");
                    }
                    if (regex.lastIndexOf('%') == regex.length() - 1) {
                        regex = regex.replaceAll("%$", ".*");
                    }
                    return string.toLowerCase(Locale.ROOT).matches(regex.toLowerCase(Locale.ROOT));
                };
            }

            case "=" -> {
                return String::equals;
            }

            case "!=" -> {
                return (a, b) -> {
                    if (a == null) {
                        return true;
                    }
                    return !a.equals(b);
                };
            }

            default -> throw new RuntimeException("operator is not supported by String type: " + strOperator);
        }
    }
}
