package com.digdes.school;

import com.digdes.school.enums.Columns;
import com.digdes.school.enums.Statements;

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
        request = request.replaceAll("=(?=\\w|')", "= ");
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
            throw new Exception(e.getMessage() + " request: " + request, e);
        }

        throw new Exception("something went wrong");
    }

    private List<Map<String, Object>> executeInsertStatement(Scanner sc) throws Exception {
        Map<String, Object> row = new HashMap<>();
        if (!sc.next().toLowerCase(Locale.ROOT).equals("values")) {
            throw new Exception("wrong syntax");
        }

        parseValues(sc, row);
        data.add(row);
        return data;

    }

    private <T extends Comparable<T>> List<Map<String, Object>> executeUpdateStatement(Scanner sc) throws Exception {
        Map<String, Object> valuesRow = new HashMap<>();

        List<Map<String, Object>> updatedRows = new ArrayList<>();

        List<String> logicalOperatorsList = new ArrayList<>();
        List<Columns> columnsList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<BiFunction> functionList = new ArrayList<>();

        if (!sc.next().toLowerCase(Locale.ROOT).equals("values")) {
            throw new Exception("wrong syntax");
        }

        String lastToken = parseValues(sc, valuesRow);
        if (!lastToken.equals("where")) {
            for (Map<String, Object> currentRow : data) {
                currentRow.putAll(valuesRow);
            }
            return data;
        }

        parseWhere(sc, logicalOperatorsList, columnsList, values, functionList);
        for (Map<String, Object> currentRow : data) {
            boolean finalResult = getResultOfWhereComparison(logicalOperatorsList, columnsList, values, functionList, currentRow);
            if (finalResult) {
                currentRow.putAll(valuesRow);
                updatedRows.add(currentRow);
            }
        }

        return updatedRows;
    }

    private <T extends Comparable<T>> List<Map<String, Object>> executeDeleteStatement(Scanner sc) throws Exception {
        List<Map<String, Object>> deletedRows = new ArrayList<>();

        List<String> logicalOperatorsList = new ArrayList<>();
        List<Columns> columnsList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<BiFunction> functionList = new ArrayList<>();
        if (!sc.hasNext()) {
            deletedRows.addAll(data);
            deletedRows.clear();
            return deletedRows;
        }

        parseWhereToResultSet(sc, deletedRows, logicalOperatorsList, columnsList, values, functionList, data);

        for (Map<String, Object> deletedRow : deletedRows) {
            data.remove(deletedRow);
        }
        return deletedRows;
    }

    private <T extends Comparable<T>> List<Map<String, Object>> executeSelectStatement(Scanner sc) throws Exception {
        List<Map<String, Object>> resultSet = new ArrayList<>();

        List<String> logicalOperatorsList = new ArrayList<>();
        List<Columns> columnsList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<BiFunction> functionList = new ArrayList<>();
        if (!sc.hasNext()) {
            return data;
        }

        parseWhereToResultSet(sc, resultSet, logicalOperatorsList, columnsList, values, functionList, data);

        return resultSet;
    }
}
