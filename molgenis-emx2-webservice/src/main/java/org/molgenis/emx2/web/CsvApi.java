package org.molgenis.emx2.web;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;
import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.*;
import static spark.Spark.delete;

public class CsvApi {
  private CsvApi() {
    // hide constructor
  }

  public static void create() {

    // schema level operations
    final String schemaPath = "/api/csv/:schema"; // NOSONAR
    get(schemaPath, CsvApi::getTables);
    post(schemaPath, CsvApi::postTables);

    // table level operations
    final String tablePath = "/api/csv/:schema/:table"; // NOSONAR
    get(tablePath, CsvApi::getRows);
    post(tablePath, CsvApi::postRows);
    delete(tablePath, CsvApi::deleteRows);
  }

  static String postTables(Request request, Response response) throws IOException {
    return "TODO";
  }

  static String getTables(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    StringWriter writer = new StringWriter();
    Emx2.toCsv(schema.getMetadata(), writer, ',');
    response.status(200);
    return writer.toString();
  }

  private static String getRows(Request request, Response response) throws IOException {
    List<Row> rows = getTable(request).retrieve();
    StringWriter writer = new StringWriter();
    CsvTableWriter.rowsToCsv(rows, writer, ',');
    response.type(ACCEPT_CSV);
    response.status(200);
    return writer.toString();
  }

  private static String postRows(Request request, Response response) {
    int count = getTable(request).insert(csvToRows(request));
    response.status(200);
    response.type(ACCEPT_CSV);
    return "" + count;
  }

  private static String deleteRows(Request request, Response response) {
    Table table = getTable(request);
    Iterable<Row> rows = csvToRows(request);
    int count = table.delete(rows);
    response.type(ACCEPT_CSV);
    response.status(200);
    return "" + count;
  }

  private static Iterable<Row> csvToRows(Request request) {
    return CsvTableReader.read(new StringReader(request.body()), ',');
  }
}
