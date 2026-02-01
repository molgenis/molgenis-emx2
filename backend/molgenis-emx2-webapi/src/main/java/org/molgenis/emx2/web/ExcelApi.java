package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.CsvApi.getDownloadColumns;
import static org.molgenis.emx2.web.CsvApi.getDownloadRows;
import static org.molgenis.emx2.web.DownloadApiUtils.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.ZipApi.generateReportsToStore;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.ImportExcelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Changelog;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;

public class ExcelApi {
  private static final int DEFAULT_CHANGELOG_LIMIT = 100;
  private static final int DEFAULT_CHANGELOG_OFFSET = 0;

  private ExcelApi() {
    // hide constructor
  }

  private static void sendExcel(Context ctx, String filename, ThrowingConsumer<TableStore> writer)
      throws Exception {
    sendFileDownload(
        ctx, filename + ".xlsx", EXCEL_CONTENT_TYPE, TableStoreForXlsxFile::new, writer);
  }

  private static TableStore uploadedExcel(Context ctx) throws Exception {
    return uploadedStore(ctx, TableStoreForXlsxFile::new);
  }

  public static void create(Javalin app) {
    final String apiPath = "/{schema}/api/excel/";
    app.get(apiPath + "_schema", ExcelApi::getMetadata);
    app.post(apiPath + "_schema", ExcelApi::postMetadata);
    app.delete(apiPath + "_schema", ExcelApi::deleteMetadata);
    app.get(apiPath + "_all", ExcelApi::getAll);
    app.post(apiPath + "_all", ExcelApi::postAll);
    app.get(apiPath + "_data", ExcelApi::getData);
    app.post(apiPath + "_data", ExcelApi::postData);
    app.get(apiPath + "_members", ExcelApi::getMembers);
    app.get(apiPath + "_settings", ExcelApi::getSettings);
    app.get(apiPath + "_changelog", ExcelApi::getChangelog);
    app.get(apiPath + "{table}", ExcelApi::getExcelTable);
    app.post(apiPath + "{table}", ExcelApi::postExcelTable);
    app.delete(apiPath + "{table}", ExcelApi::deleteExcelTable);

    app.get("/{schema}/api/reports/excel", ExcelApi::getExcelReport);
  }

  static void getMetadata(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    sendExcel(ctx, schema.getName() + "_schema", store -> Emx2.outputMetadata(store, schema));
  }

  static void postMetadata(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    TableStore store = uploadedExcel(ctx);
    SchemaMetadata metadata = Emx2.fromRowList(store.readTable("molgenis"));
    timedOperation(ctx, "Metadata import success", () -> schema.migrate(metadata));
  }

  static void deleteMetadata(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    TableStore store = uploadedExcel(ctx);
    SchemaMetadata metadata = Emx2.fromRowList(store.readTable("molgenis"));
    timedOperation(ctx, "Metadata discard success", () -> schema.discard(metadata));
  }

  static void getAll(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    boolean includeSystemColumns = includeSystemColumns(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    if (ctx.queryParam("emx1") != null) {
      MolgenisIO.toEmx1ExcelFile(excelFile, schema);
    } else {
      MolgenisIO.toExcelFile(excelFile, schema, includeSystemColumns);
    }
    sendFileResponse(
        ctx,
        excelFile,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        schema.getName());
  }

  static void postAll(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    Path tempFile = uploadFileToTemp(ctx).toPath();
    if (isAsync(ctx)) {
      submitAsyncTask(ctx, schema, new ImportExcelTask(tempFile, schema, false));
    } else {
      timedOperation(
          ctx, "Import success", () -> MolgenisIO.importFromExcelFile(tempFile, schema, false));
    }
  }

  static void getData(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    sendExcel(
        ctx,
        schema.getName() + "_data",
        store -> {
          for (String tableName : schema.getTableNames()) {
            Table table = schema.getTable(tableName);
            store.writeTable(
                tableName, getDownloadColumns(ctx, table), getDownloadRows(ctx, table));
          }
        });
  }

  static void postData(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    Path tempFile = uploadFileToTemp(ctx).toPath();
    if (isAsync(ctx)) {
      submitAsyncTask(ctx, schema, new ImportExcelTask(tempFile, schema, false));
    } else {
      timedOperation(
          ctx,
          "Data import success",
          () -> MolgenisIO.importFromExcelFile(tempFile, schema, false));
    }
  }

  static void getMembers(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema members");
    }
    sendExcel(ctx, schema.getName() + "_members", store -> Emx2Members.outputRoles(store, schema));
  }

  static void getSettings(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    sendExcel(
        ctx, schema.getName() + "_settings", store -> Emx2Settings.outputSettings(store, schema));
  }

  static void getChangelog(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema changelog");
    }
    int limit = parseIntParam(ctx, "limit").orElse(DEFAULT_CHANGELOG_LIMIT);
    int offset = parseIntParam(ctx, "offset").orElse(DEFAULT_CHANGELOG_OFFSET);
    sendExcel(
        ctx,
        schema.getName() + "_changelog",
        store -> Emx2Changelog.outputChangelog(store, schema, limit, offset));
  }

  static void getExcelTable(Context ctx) throws Exception {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    sendExcel(
        ctx,
        table.getSchema().getMetadata().getName() + "_" + table.getName(),
        store ->
            store.writeTable(
                table.getName(), getDownloadColumns(ctx, table), getDownloadRows(ctx, table)));
  }

  static void postExcelTable(Context ctx) throws Exception {
    processExcelTable(ctx, false);
  }

  static void deleteExcelTable(Context ctx) throws Exception {
    processExcelTable(ctx, true);
  }

  private static void processExcelTable(Context ctx, boolean isDelete) throws Exception {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    TableStore store = uploadedExcel(ctx);
    int count = processTableOperation(ctx, table, store, isDelete);
    ctx.status(200);
    ctx.result((isDelete ? "Deleted " : "Imported ") + count + " rows");
  }

  static void getExcelReport(Context ctx) throws Exception {
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore excelStore = new TableStoreForXlsxFile(excelFile);
    generateReportsToStore(ctx, excelStore);
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header("Content-Disposition", "attachment; filename=report.xlsx");
      outputStream.write(Files.readAllBytes(excelFile));
    }
  }
}
