package com.digdes.school;

import com.digdes.school.enums.*;

import java.util.*;
import java.util.function.BiFunction;

import static com.digdes.school.utils.ParseUtils.*;

public class JavaSchoolStarter {
    private final List<Map<String, Object>> data;

    public JavaSchoolStarter() {
        data = new ArrayList<>();
    }

    //На вход запрос, на выход результат выполнения запроса
    public List<Map<String, Object>> execute(String request) throws Exception {
        request = request.replaceAll("\\s+", " "); // deleting all whitespaces that occur consecutively more than 1 time

        request = request.replaceAll("(?<=[^a-zA-Z0-9а-яА-Я%])'", " '");
        request = request.replaceAll("'(?=[^a-zA-Z0-9а-яА-Я%])", "'  "); // formatting request in more convenient form
        request = request.replaceAll(",", " , ");
        request = request.replaceAll("=(?=\\w|')","= ");
        Scanner sc = new Scanner(request);

        String statementToken = sc.next();
        Statements statement = Statements.valueOf(statementToken);
        try (sc) {
            switch (statement) {
                case INSERT -> {
                    return executeInsertStatement(sc);
                }
                case UPDATE -> {
                    return executeUpdateStatement(sc);
                }
                case DELETE -> {
                    return executeDeleteStatement(sc);
                }
                case SELECT -> {
                    return executeSelectStatement(sc);
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage() + " request: " + request,e);
        }

        throw new Exception("something went wrong");
    }

    private <T extends Comparable<T>> List<Map<String, Object>> executeSelectStatement(Scanner sc) throws Exception {
        List<Map<String,Object>> resultSet = new ArrayList<>();

        List<BiFunction<Boolean, Boolean, Boolean>> logicalOperatorsList = new ArrayList<>();
        List<Columns> columnsList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<BiFunction> functionList = new ArrayList<>();

        if (sc.hasNext() && sc.next().toLowerCase(Locale.ROOT).equals("where")) {
            parseWhere(sc, logicalOperatorsList, columnsList, values, functionList);

            for (Map<String, Object> currentRow : data) {
                boolean finalResult = getResultOfWhereComparison(logicalOperatorsList, columnsList, values, functionList, currentRow);
                if (finalResult) {
                    resultSet.add(currentRow);
                }
            }

        } else {
            return data;
        }
        return resultSet;
    }

    private <T extends Comparable<T>> List<Map<String, Object>> executeDeleteStatement(Scanner sc) throws Exception {
        List<Map<String,Object>> deletedRows = new ArrayList<>();

        List<BiFunction<Boolean, Boolean, Boolean>> logicalOperatorsList = new ArrayList<>();
        List<Columns> columnsList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<BiFunction> functionList = new ArrayList<>();

        if (sc.hasNext() && sc.next().toLowerCase(Locale.ROOT).equals("where")) {
            parseWhere(sc, logicalOperatorsList, columnsList, values, functionList);

            for (Map<String, Object> currentRow : data) {
                boolean finalResult = getResultOfWhereComparison(logicalOperatorsList, columnsList, values, functionList, currentRow);
                if (finalResult) {
                    deletedRows.add(currentRow);
                }
            }

        } else {
          data.clear();
          System.out.println("All data wiped");
        }
        for (Map<String,Object> deletedRow : deletedRows) {
            data.remove(deletedRow);
        }
        return deletedRows;
    }

    private <T extends Comparable<T>> List<Map<String, Object>> executeUpdateStatement(Scanner sc) throws Exception {
        Map<String, Object> row = new HashMap<>();

        List<Map<String,Object>> updatedRows = new ArrayList<>();

        List<BiFunction<Boolean, Boolean, Boolean>> logicalOperatorsList = new ArrayList<>();
        List<Columns> columnsList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<BiFunction> functionList = new ArrayList<>();

        if (sc.hasNext() && sc.next().toLowerCase(Locale.ROOT).equals("values")) {
            String lastToken = parseValues(sc, row);
            if (lastToken.equals("where")) {
                parseWhere(sc, logicalOperatorsList, columnsList, values, functionList);

                for (Map<String, Object> currentRow : data) {
                    boolean finalResult = getResultOfWhereComparison(logicalOperatorsList, columnsList, values, functionList, currentRow);
                    if (finalResult) {
                        currentRow.putAll(row);
                        updatedRows.add(currentRow);
                    }
                }


            } else {
                for (Map<String, Object> currentRow : data) {
                    currentRow.putAll(row);
                }
                return data;
            }
            return updatedRows;
        } else {
            throw new Exception("wrong syntax");
        }
    }

    private List<Map<String, Object>> executeInsertStatement(Scanner sc) throws Exception {
        Map<String, Object> row = new HashMap<>();
        if (sc.next().toLowerCase(Locale.ROOT).equals("values")) {
            parseValues(sc, row);
            data.add(row);
            return data;
        } else {
            throw new Exception("wrong syntax");
        }
    }

}
