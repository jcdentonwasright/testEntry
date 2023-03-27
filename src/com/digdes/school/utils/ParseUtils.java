package com.digdes.school.utils;

import com.digdes.school.enums.Columns;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

public final class ParseUtils {

    private ParseUtils () {}

    public static boolean getResultOfWhereComparison(List<BiFunction<Boolean, Boolean, Boolean>> logicalOperatorsList, List<Columns> columnsList, List<Object> values, List<BiFunction> functions, Map<String, Object> currentRow) {
        boolean finalResult = false;
        int j = 0;
        for (Columns columns : columnsList) {
            int i = columnsList.indexOf(columns);
            boolean result;

            if (!currentRow.containsKey(columns.name())) {
                continue; // no such column in row then comparison cannot be done
            }
            try {
               result = (boolean) functions.get(i).apply(currentRow.get(columns.name()), values.get(i));
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                System.out.println("cannot compare with null");
                System.out.println("exception in column : " + columns
                        + " | row value : " + currentRow.get(columns.name())
                        + " | passed value : " + values.get(i));
                result = false;
            }

            if (!logicalOperatorsList.isEmpty() && i > 0) {
                finalResult = logicalOperatorsList.get(j).apply(finalResult, result);
                j++;
                continue;
            }

            finalResult = result;
        }

        return finalResult;
    }

    public static <T extends Comparable<T>> void parseWhere(Scanner sc, List<BiFunction<Boolean, Boolean, Boolean>> logicalOperatorsList, List<Columns> columnsList, List<Object> values, List<BiFunction> functions) throws Exception {
        while (sc.hasNext()) {
            Columns column = Columns.valueOf(sc.next().replaceAll("^'|'$", "").toLowerCase(Locale.ROOT));
            columnsList.add(column);

            String operator = sc.next().toLowerCase(Locale.ROOT);
            switch (column) {
                case age, id, cost, active -> {
                    BiFunction<T, T, Boolean> function = parseIntOperatorToBiFunc(operator);
                    functions.add(function);
                }
                case lastname -> {
                    BiFunction<String, String, Boolean> function = parseStrOperatorToBiFunc(operator);
                    functions.add(function);
                }
            }

            values.add(parseWhereValue(column, sc.next()));


            if (sc.hasNext()) {
                logicalOperatorsList.add(parseLogicalOperatorToBiFunc(sc.next().toLowerCase(Locale.ROOT)));
                if (!sc.hasNext()) {
                    throw new Exception("incorrect syntax");
                }
            }
        }
    }

    public static Object parseWhereValue(Columns column, String value) throws Exception {
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

            if (sc.next().equals("=")) {
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
            } else {
                throw new Exception("no '=' symbol");
            }

        }
        return "";
    }

    public static BiFunction<Boolean, Boolean, Boolean> parseLogicalOperatorToBiFunc(String logicalOperator) throws Exception {
        switch (logicalOperator) {
            case "and" -> {
                return (a, b) -> a && b;
            }
            case "or" -> {
                return (a, b) -> a || b;
            }

            default -> throw new Exception("not a logical operator :" + logicalOperator);
        }
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
                    return !a.equals(b);};
            }

            default -> throw new Exception("incorrect operator: " + intOperator);
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
                        regex = regex.replaceAll("%", ".*");
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
                        regex = regex.replaceAll("%", ".*");
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
                    return !a.equals(b);};
            }

            default -> throw new Exception("incorrect operator: " + strOperator);
        }
    }
}
