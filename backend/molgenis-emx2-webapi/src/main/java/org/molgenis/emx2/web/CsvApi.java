package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.graphql.GraphqlTableFieldFactory.convertMapToFilterArray;
import static org.molgenis.emx2.io.emx2.Emx2.getHeaders;
import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;
import static org.molgenis.emx2.web.DownloadApiUtils.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlConstants;
import org.molgenis.emx2.io.ImportTableTask;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Changelog;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.SqlTypeUtils;
import org.molgenis.emx2.tasks.Task;

public class CsvApi {

  private static final int DEFAULT_CHANGELOG_LIMIT = 100;
  private static final int DEFAULT_CHANGELOG_OFFSET = 0;

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  private CsvApi() {}

  public static void create(Javalin app) {
    final String apiPath = "/{schema}/api/csv/";
    app.get(apiPath + "_schema", CsvApi::getMetadata);
    app.post(apiPath + "_schema", CsvApi::mergeMetadata);
    app.delete(apiPath + "_schema", CsvApi::discardMetadata);
    app.get(apiPath + "_members", CsvApi::getMembers);
    app.get(apiPath + "_settings", CsvApi::getSettings);
    app.get(apiPath + "_changelog", CsvApi::getChangelog);
    app.get(apiPath + "{table}", CsvApi::tableRetrieve);
    app.post(apiPath + "{table}", CsvApi::tableUpdate);
    app.delete(apiPath + "{table}", CsvApi::tableDelete);

    final String legacyPath = "/{schema}/api/csv";
    app.get(legacyPath, CsvApi::getMetadata);
    app.post(legacyPath, CsvApi::mergeMetadata);
    app.delete(legacyPath, CsvApi::discardMetadata);
    app.get(legacyPath + "/members", CsvApi::getMembers);
    app.get(legacyPath + "/settings", CsvApi::getSettings);
    app.get(legacyPath + "/changelog", CsvApi::getChangelog);
  }

  private static void getChangelog(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema changelog");
    }

    int limit = parseIntParam(ctx, "limit").orElse(DEFAULT_CHANGELOG_LIMIT);
    int offset = parseIntParam(ctx, "offset").orElse(DEFAULT_CHANGELOG_OFFSET);

    StringWriter writer = new StringWriter();
    Character separator = getSeparator(ctx);
    TableStore store = new TableStoreForCsvInMemory();

    Emx2Changelog.outputChangelog(store, schema, limit, offset);

    CsvTableWriter.write(
        store.readTable(CHANGELOG_TABLE),
        List.of(
            CHANGELOG_OPERATION,
            CHANGELOG_STAMP,
            CHANGELOG_USERID,
            CHANGELOG_TABLENAME,
            CHANGELOG_OLD,
            CHANGELOG_NEW),
        writer,
        separator);

    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_changelog_" + date + ".csv\"");
    ctx.contentType(ACCEPT_CSV);
    ctx.status(200);
    ctx.result(writer.toString());
  }

  private static void discardMetadata(Context ctx) throws Exception {
    timedOperation(
        ctx,
        "remove metadata items success",
        () -> {
          SchemaMetadata schema = Emx2.fromRowList(getRowList(ctx));
          getSchema(ctx).discard(schema);
        });
  }

  static void mergeMetadata(Context ctx) throws Exception {
    String fileName = ctx.header("fileName");
    boolean fileNameMatchesTable = getSchema(ctx).hasTableWithNameOrIdCaseInsensitive(fileName);

    if (fileNameMatchesTable) {
      Table table = MolgenisWebservice.getTableByIdOrName(ctx, fileName);
      if (isAsync(ctx)) {
        TableStoreForCsvInMemory tableStore = new TableStoreForCsvInMemory();
        tableStore.setCsvString(table.getName(), ctx.body());
        Task task = new ImportTableTask(tableStore, table, false);
        String parentTaskId = ctx.queryParam("parentJob");
        String id = TaskApi.submit(task, parentTaskId);
        ctx.result(new TaskReference(id, table.getSchema()).toString());
      } else {
        long start = System.currentTimeMillis();
        int count = table.save(getRowList(ctx));
        ctx.status(200);
        ctx.contentType(ACCEPT_CSV);
        ctx.result(
            "imported number of rows: "
                + count
                + " in "
                + (System.currentTimeMillis() - start)
                + "ms");
      }
    } else {
      timedOperation(
          ctx,
          "{ \"message\": \"add/update metadata success\" }",
          () -> {
            SchemaMetadata schema = Emx2.fromRowList(getRowList(ctx));
            getSchema(ctx).migrate(schema);
          });
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
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".csv\"");
    ctx.status(200);
    ctx.result(writer.toString());
  }

  private static void getMembers(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema members");
    }

    StringWriter writer = new StringWriter();
    Character separator = getSeparator(ctx);
    TableStore tableStore = new TableStoreForCsvInMemory(separator);

    Emx2Members.outputRoles(tableStore, schema);

    CsvTableWriter.write(
        tableStore.readTable(Emx2Members.ROLES_TABLE),
        List.of(Emx2Members.USER, Emx2Members.ROLE),
        writer,
        separator);

    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_members_" + date + ".csv\"");
    ctx.contentType(ACCEPT_CSV);
    ctx.status(200);
    ctx.result(writer.toString());
  }

  private static void getSettings(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);

    StringWriter writer = new StringWriter();
    Character separator = getSeparator(ctx);
    TableStore tableStore = new TableStoreForCsvInMemory(separator);

    Emx2Settings.outputSettings(tableStore, schema);

    CsvTableWriter.write(
        tableStore.readTable(SETTINGS_TABLE),
        List.of(TABLE, SETTINGS_NAME, SETTINGS_VALUE),
        writer,
        separator);
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_settings_" + date + ".csv\"");
    ctx.contentType(ACCEPT_CSV);
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
        .filter(name -> name.equals(MG_DRAFT) || !name.startsWith("mg_") || includeSystem)
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
              table.getMetadata(),
              new ObjectMapper()
                  .readValue(ctx.queryParam(GraphqlConstants.FILTER_ARGUMENT), Map.class)));
    }
    List<Row> rows = q.retrieveRows();
    SqlTypeUtils.applyComputed(table.getMetadata().getColumns(), rows);

    return rows;
  }

  private static void tableUpdate(Context ctx) {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    long start = System.currentTimeMillis();
    TableStoreForCsvInMemory tableStore = new TableStoreForCsvInMemory();
    tableStore.setCsvString(table.getName(), ctx.body());
    Task task = new ImportTableTask(tableStore, table, false);
    task.run();
    ctx.status(200);
    ctx.contentType(ACCEPT_CSV);
    ctx.result(
        String.valueOf(task.getProgress()) + " in " + (System.currentTimeMillis() - start) + "ms");
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
