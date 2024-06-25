package org.molgenis.emx2.web;

import static spark.Spark.*;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.*;
import spark.Request;
import spark.Response;

public class RestApi {

  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;

    defineRoutes();
  }

  private static void defineRoutes() {
    path(
            "/:schema/api/rest",
        () -> {
          get("/:table", RestApi::getTableData);
        });
  }

  private static Object getTableData(Request req, Response res) throws IOException {

    String schemaName = req.params(":schema");
    String tableName = req.params(":table");

    MolgenisSession session = sessionManager.getSession(req);
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);
    Table table = schema.getTable(tableName);

    GraphQL gql = session.getGraphqlForSchema(schemaName);
    String query = tableQuery(table);
    ExecutionResult result = gql.execute(query);
    return GraphqlApiFactory.convertExecutionResultToJson(result);
  }

  private static String tableQuery(Table table) throws IOException {
    String fields =
        table.getMetadata().getColumnsWithoutHeadings().stream()
            .filter(column -> !column.getName().startsWith("mg_"))
            .map(RestApi::columToQueryField)
            .collect(Collectors.joining(" "));

    return String.format("{ %s { %s } }", table.getName(), fields);
  }

  private static String columToQueryField(Column column) {
    ColumnType columnType = column.getColumnType();
    String columnName = column.getName();
    String camelColumnName = toCamel(columnName);
    if ( // who needs polymorphism :S
    columnType.equals(ColumnType.EMAIL)
        || columnType.equals(ColumnType.BOOL)
        || columnType.equals(ColumnType.DATE)
        || columnType.equals(ColumnType.INT)
        || columnType.equals(ColumnType.LONG)
        || columnType.equals(ColumnType.TEXT)
        || columnType.equals(ColumnType.DECIMAL)
        || columnType.equals(ColumnType.STRING)
        || columnType.equals(ColumnType.AUTO_ID)
        || columnType.equals(ColumnType.BOOL_ARRAY)
        || columnType.equals(ColumnType.DATE_ARRAY)
        || columnType.equals(ColumnType.DATETIME_ARRAY)
        || columnType.equals(ColumnType.DATETIME)
        || columnType.equals(ColumnType.EMAIL_ARRAY)
        || columnType.equals(ColumnType.UUID) // ect. ect , .....
    ) {
      return camelColumnName;
    } else if (columnType.isFile()) {
      return String.format(" %s { url } ", camelColumnName);
    } else if (columnType.isRef()) {
      String refKeyFields = refTableToQueryField(column.getRefTable());
      return String.format(" %s { %s } ", camelColumnName, refKeyFields);
    }
    return " ";
  }

  private static String refTableToQueryField(TableMetadata table) {
    return table.getPrimaryKeyColumns().stream()
        .map(RestApi::columToQueryField)
        .collect(Collectors.joining(" "));
  }

  private static String toCamel(String input) {
    String[] tokenArray = input.split(" ");
    List<String> tokenList = Arrays.asList(tokenArray);
    String camel =
        tokenList.stream()
            .map(token -> token.substring(0, 1).toUpperCase() + token.substring(1))
            .collect(Collectors.joining(""));
    return camel.substring(0, 1).toLowerCase() + camel.substring(1);
  }
}
