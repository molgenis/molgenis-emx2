package org.molgenis.emx2.web;

import static org.molgenis.emx2.graphql.GraphqlTableFieldFactory.convertMapToFilterArray;
import static org.molgenis.emx2.io.emx2.Emx2.getHeaders;
import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;
import static org.molgenis.emx2.web.DownloadApiUtils.includeSystemColumns;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlConstants;
import org.molgenis.emx2.io.ImportTableTask;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.tasks.Task;

public class CsvApi {
  private CsvApi() {
    // hide constructor
  }

  public static void create(Javalin app) {

    // schema level operations
    final String schemaPath = "/{schema}/api/csv";
    app.get(schemaPath, CsvApi::getMetadata);
    app.post(schemaPath, CsvApi::mergeMetadata);
    app.delete(schemaPath, CsvApi::discardMetadata);

    // table level operations
    final String tablePath = "/{schema}/api/csv/{table}";
    app.get(tablePath, CsvApi::tableRetrieve);
    app.post(tablePath, CsvApi::tableUpdate);
    app.delete(tablePath, CsvApi::tableDelete);
  }

  private static void discardMetadata(Context ctx) {
    SchemaMetadata schema = Emx2.fromRowList(getRowList(ctx));
    getSchema(ctx).discard(schema);
    ctx.status(200);
    ctx.result("remove metadata items success");
  }

  static void mergeMetadata(Context ctx) {
    String fileName = ctx.header("fileName");
    boolean fileNameMatchesTable = getSchema(ctx).hasTableWithNameOrIdCaseInsensitive(fileName);

    if (fileNameMatchesTable) { // so we assume it isn't meta data
      Table table = MolgenisWebservice.getTableByIdOrName(ctx, fileName);
      if (ctx.queryParam("async") != null) {
        TableStoreForCsvInMemory tableStore = new TableStoreForCsvInMemory();
        tableStore.setCsvString(table.getName(), ctx.body());
        Task task = new ImportTableTask(tableStore, table, false);
        String parentTaskId = ctx.queryParam("parentJob");
        String id = TaskApi.submit(task, parentTaskId);
        ctx.result(new TaskReference(id, table.getSchema()).toString());
      } else {
        int count = table.save(getRowList(ctx));
        ctx.status(200);
        ctx.contentType(ACCEPT_CSV);
        ctx.result("{ \"message\": \"imported number of rows: \" + " + count + " }");
      }
    } else {
      SchemaMetadata schema = Emx2.fromRowList(getRowList(ctx));
      getSchema(ctx).migrate(schema);
      ctx.status(200);
      ctx.result("{ \"message\": \"add/update metadata success\" }");
    }
  }

  static void getMetadata(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    StringWriter writer = new StringWriter();
    CsvTableWriter.write(
        Emx2.toRowList(schema.getMetadata()),
        getHeaders(schema.getMetadata()),
        writer,
        getSeparator(ctx));
    ctx.contentType(ACCEPT_CSV);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".csv\"");
    ctx.status(200);
    ctx.result(writer.toString());
  }

  private static void tableRetrieve(Context ctx) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory(getSeparator(ctx));
    store.writeTable(table.getName(), getDownloadColumns(ctx, table), getDownloadRows(ctx, table));
    ctx.contentType(ACCEPT_CSV);
    ctx.header("Content-Disposition", "attachment; filename=\"" + table.getName() + ".csv\"");
    ctx.status(200);
    ctx.res().setCharacterEncoding("UTF-8");
    ctx.result(store.getCsvString(table.getName()));
  }

  public static List<String> getDownloadColumns(Context ctx, Table table) {
    boolean includeSystem = includeSystemColumns(ctx);
    return table.getMetadata().getDownloadColumnNames().stream()
        .map(Column::getName)
        .filter(name -> !name.startsWith("mg_") || includeSystem)
        .toList();
  }

  public static List<Row> getDownloadRows(Context ctx, Table table) throws JsonProcessingException {
    Query q = table.query();
    // extract filter argument if exists
    if (ctx.queryParam(GraphqlConstants.FILTER_ARGUMENT) != null) {
      // gonna use the graphql filter parser so we can easily reuse graphql table level filter
      // expressions
      q.where(
          convertMapToFilterArray(
              null,
              table.getMetadata(),
              new ObjectMapper()
                  .readValue(ctx.queryParam(GraphqlConstants.FILTER_ARGUMENT), Map.class)));
    }
    return q.retrieveRows();
  }

  private static void tableUpdate(Context ctx) {
    int count = MolgenisWebservice.getTableByIdOrName(ctx).save(getRowList(ctx));
    ctx.status(200);
    ctx.contentType(ACCEPT_CSV);
    ctx.result(String.valueOf(count));
  }

  private static Iterable<Row> getRowList(Context ctx) {
    return CsvTableReader.read(new StringReader(ctx.body()));
  }

  private static void tableDelete(Context ctx) {
    int count = MolgenisWebservice.getTableByIdOrName(ctx).delete(getRowList(ctx));
    ctx.contentType(ACCEPT_CSV);
    ctx.status(200);
    ctx.result(String.valueOf(count));
  }

  private static Character getSeparator(Context ctx) {
    char separator = ',';
    if ("TAB".equals(ctx.queryParam("separator"))) {
      separator = '\t';
    }
    return separator;
  }
}
