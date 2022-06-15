package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import spark.Request;
import spark.Response;

public class CsvApi {

  public static final String MUTATION_REQUEST = "mutationRequestType";
  public static final String ERROR_MESSAGE = "errorMessageType";
  public static final String SUCCESS_MESSAGE = "successMessageType";
  public static final String CSV_OUTPUT = "csvOutputType";
  public static final String META = "_meta";

  private CsvApi() {
    // hide constructor
  }

  public static void create() {

    // schema level operations
    final String schemaPath = "/:schema/api/csv";
    get(schemaPath, CsvApi::getMetadata);
    post(schemaPath, CsvApi::mergeMetadata);
    delete(schemaPath, CsvApi::discardMetadata);

    // table level operations
    final String tablePath = "/:schema/api/csv/:table";
    get(tablePath, CsvApi::tableRetrieve);
    post(tablePath, CsvApi::tableUpdate);
    delete(tablePath, CsvApi::tableDelete);
  }

  private static String discardMetadata(Request request, Response response) {
    SchemaMetadata schema = Emx2.fromRowList(getRowList(request));
    getSchema(request).discard(schema);
    response.status(200);
    return "remove metadata items success";
  }

  static String mergeMetadata(Request request, Response response) {
    String fileName = request.headers("fileName");
    boolean fileNameMatchesTable = getSchema(request).getTableNames().contains(fileName);

    if (fileNameMatchesTable) {
      // so we assume it isn't meta data
      return tableUpdate(request, response);
    } else {
      SchemaMetadata schema = Emx2.fromRowList(getRowList(request));
      getSchema(request).migrate(schema);
      response.status(200);
      return "add/update metadata success";
    }
  }

  /**
   * Intersect a list of column names with the column names of tables of a specified schema. If we
   * find exactly one table that contains all of these columns, that table is returned.
   *
   * @param checkColNames
   * @param schema
   * @return
   */
  static Table findTableByColumns(Set<String> checkColNames, Schema schema) {
    List<Table> candidateTables = new ArrayList<>();
    for (String tableName : schema.getTableNames()) {
      Table t = schema.getTable(tableName);
      List<String> intersect = new ArrayList<>();
      for (String colName : t.getMetadata().getColumnNames()) {
        if (checkColNames.contains(colName)) {
          intersect.add(colName);
        }
      }
      if (checkColNames.size() == intersect.size()) {
        candidateTables.add(t);
      }
    }
    if (candidateTables.size() == 1) {
      return candidateTables.get(0);
    } else {
      return null;
    }
  }

  static String getMetadata(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    StringWriter writer = new StringWriter();
    CsvTableWriter.write(Emx2.toRowList(schema.getMetadata()), writer, getSeperator(request));
    response.type(ACCEPT_CSV);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    response.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".csv\"");
    response.status(200);
    return writer.toString();
  }

  private static String tableRetrieve(Request request, Response response) throws IOException {
    Table table = MolgenisWebservice.getTable(request);
    List<Row> rows = table.retrieveRows();
    StringWriter writer = new StringWriter();
    CsvTableWriter.write(rows, writer, getSeperator(request));
    response.type(ACCEPT_CSV);
    response.header("Content-Disposition", "attachment; filename=\"" + table.getName() + ".csv\"");
    response.status(200);
    return writer.toString();
  }

  private static String tableUpdate(Request request, Response response) {
    int count = MolgenisWebservice.getTable(request).save(getRowList(request));
    response.status(200);
    response.type(ACCEPT_CSV);
    return "" + count;
  }

  private static Iterable<Row> getRowList(Request request) {
    return CsvTableReader.read(new StringReader(request.body()));
  }

  private static String tableDelete(Request request, Response response) {
    int count = MolgenisWebservice.getTable(request).delete(getRowList(request));
    response.type(ACCEPT_CSV);
    response.status(200);
    return "" + count;
  }

  private static Character getSeperator(Request request) {
    Character separator = ',';
    if ("TAB".equals(request.queryParams("separator"))) {
      separator = '\t';
    }
    return separator;
  }
}
