package com.digdes.school.utils;

import com.digdes.school.enums.Columns;

import java.util.*;
import java.util.function.BiFunction;

public final class ParseUtils {
    private static class Wrapper {
        Integer value;

        public Wrapper(Integer value) {
            this.value = value;
        }

        public void add(Integer value) {
            this.value += value;
        }
    }

    private ParseUtils() {
    }

    public static boolean getResultOfWhereComparison(List<String> logicalOperatorsList,
                                                     List<Columns> columnsList,
                                                     List<Object> values,
                                                     List<BiFunction> functions,
                                                     Map<String, Object> currentRow) {
        List<Boolean> results = new ArrayList<>();


        for (int i = 0;i < columnsList.size();i++) {
            Columns column = columnsList.get(i);
            String columnName = column.name();
            boolean result;

            if (!currentRow.containsKey(columnName)) {
                results.add(false); // no such column in row then comparison cannot be done
                continue;
            }
            try {
                result = (boolean) functions.get(i).apply(currentRow.get(columnName), values.get(i));
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                System.out.println("cannot compare with null");
                System.out.println("exception in column : " + column
                        + " | row value : " + currentRow.get(columnName)
                        + " | passed value : " + values.get(i));
                result = false;
            }

            results.add(result);
        }
        if (logicalOperatorsList.isEmpty()) {
            return results.get(0);
        }

        return getResult(logicalOperatorsList, results);
    }

    private static boolean getResult(List<String> logicalOperatorsList, List<Boolean> results) {
        Wrapper wrapper = new Wrapper(0);
        boolean result = orend(wrapper,logicalOperatorsList,results);
        String op = logicalOperatorsList.get(wrapper.value);
        while (op.equals("or")) {
            wrapper.add(1);
            boolean a = orend(wrapper,logicalOperatorsList,results);
            result = result || a;
            if (wrapper.value == logicalOperatorsList.size()) {
                break;
            }
            op = logicalOperatorsList.get(wrapper.value);
        }
        return result;
    }

    private static boolean orend(Wrapper wrapper, List<String> logicalOperatorsList, List<Boolean> results) {
        Boolean result = results.get(wrapper.value);
        if (wrapper.value == logicalOperatorsList.size()) {
            return result;
        }
        String op = logicalOperatorsList.get(wrapper.value);
        while (op.equals("and")) {
            wrapper.add(1);
            boolean a = results.get(wrapper.value);;
            result = result && a;
            if (wrapper.value == logicalOperatorsList.size()) {
                break;
            }
            op = logicalOperatorsList.get(wrapper.value);
        }
        return result;
    }

    public static void parseWhereToResultSet(Scanner sc,
                                             List<Map<String, Object>> resultSet,
                                             List<String> logicalOperatorsList,
                                             List<Columns> columnsList,
                                             List<Object> values, List<BiFunction> functionList,
                                             List<Map<String, Object>> data) throws Exception {
        if (!sc.next().toLowerCase(Locale.ROOT).equals("where")) {
            throw new Exception("wrong syntax");
        }

        parseWhere(sc, logicalOperatorsList, columnsList, values, functionList);

        for (Map<String, Object> currentRow : data) {
            boolean finalResult = getResultOfWhereComparison(logicalOperatorsList, columnsList, values, functionList, currentRow);
            if (finalResult) {
                resultSet.add(currentRow);
            }
        }
    }

    public static <T extends Comparable<T>> void parseWhere(Scanner sc, List<String> logicalOperatorsList, List<Columns> columnsList, List<Object> values, List<BiFunction> functions) throws Exception {
        while (sc.hasNext()) {
            Columns column = Columns.valueOf(sc.next().replaceAll("^'|'$", "").toLowerCase(Locale.ROOT));
            columnsList.add(column);

            String operator = sc.next().toLowerCase(Locale.ROOT);
            switch (column) {
                case age, id, cost -> {
                    BiFunction<T, T, Boolean> function = parseIntOperatorToBiFunc(operator);
                    functions.add(function);
                }
                case lastname -> {
                    BiFunction<String, String, Boolean> function = parseStrOperatorToBiFunc(operator);
                    functions.add(function);
                }
                case active -> {
                    BiFunction<Boolean, Boolean, Boolean> function = parseBooleanToBiFunc(operator);
                    functions.add(function);
                }
            }

            values.add(parseWhereValues(column, sc.next()));

            if (sc.hasNext()) {
                logicalOperatorsList.add(sc.next().toLowerCase(Locale.ROOT));
                if (!sc.hasNext()) {
                    throw new Exception("incorrect syntax");
                }
            }
        }
    }

    public static Object parseWhereValues(Columns column, String value) throws Exception {
        switch (column) {
            case id, age -> {
                return Long.parseLong(value);
            }
            case cost -> {
                return Double.parseDouble(value);
            }
            case lastname -> {
                if (!value.matches("'.*'")) {
                    throw new Exception("incorrect value to lastname: " + value);
                }
                return value.replaceAll("^'|'$", "");
            }

            case active -> {
                return Boolean.parseBoolean(value);
            }

            default -> throw new Exception("no such column: " + column.name());
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

    public static <T extends Comparable<T>> BiFunction<T, T, Boolean> parseIntOperatorToBiFunc(String intOperator) throws Exception {
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

            default -> throw new Exception("operator is not supported by Number type: " + intOperator);
        }
    }

    public static BiFunction<Boolean, Boolean, Boolean> parseBooleanToBiFunc(String logicalOperator) throws Exception {
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
            default -> throw new Exception("operator is not supported by Boolean type:" + logicalOperator);
        }
    }

    public static BiFunction<String, String, Boolean> parseStrOperatorToBiFunc(String strOperator) throws Exception {
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

            default -> throw new Exception("operator is not supported by String type: " + strOperator);
        }
    }
}
