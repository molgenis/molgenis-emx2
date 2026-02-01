package org.molgenis.emx2.web;

import static org.molgenis.emx2.io.ImportMetadataTask.MOLGENIS;
import static org.molgenis.emx2.settings.ReportUtils.getReportAsRows;
import static org.molgenis.emx2.web.Constants.ACCEPT_ZIP;
import static org.molgenis.emx2.web.Constants.TABLE;
import static org.molgenis.emx2.web.DownloadApiUtils.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportCsvZipTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Changelog;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;

public class ZipApi {
  private ZipApi() {
    // hide constructor
  }

  private static void sendZip(Context ctx, String filename, ThrowingConsumer<TableStore> writer)
      throws Exception {
    sendFileDownload(
        ctx, filename + ".zip", ZIP_CONTENT_TYPE, TableStoreForCsvInZipFile::new, writer);
  }

  private static TableStore uploadedZip(Context ctx) throws Exception {
    return uploadedStore(ctx, TableStoreForCsvInZipFile::new);
  }

  static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final int DEFAULT_CHANGELOG_LIMIT = 100;
  private static final int DEFAULT_CHANGELOG_OFFSET = 0;

  public static void create(Javalin app) {
    final String apiPath = "/{schema}/api/zip/";
    app.get(apiPath + "_schema", ZipApi::getMetadata);
    app.post(apiPath + "_schema", ZipApi::mergeMetadata);
    app.delete(apiPath + "_schema", ZipApi::discardMetadata);
    app.get(apiPath + "_all", ZipApi::getAllZip);
    app.post(apiPath + "_all", ZipApi::postAllZip);
    app.get(apiPath + "_data", ZipApi::getData);
    app.post(apiPath + "_data", ZipApi::postData);
    app.get(apiPath + "_members", ZipApi::getMembers);
    app.get(apiPath + "_settings", ZipApi::getSettings);
    app.get(apiPath + "_changelog", ZipApi::getChangelog);
    app.get(apiPath + "{table}", ZipApi::getZipTable);
    app.post(apiPath + "{table}", ZipApi::postZipTable);
    app.delete(apiPath + "{table}", ZipApi::deleteZipTable);

    app.get("/{schema}/api/reports/zip", ZipApi::getZippedReports);
  }

  static void getAllZip(Context ctx) throws IOException {
    boolean includeSystemColumns = includeSystemColumns(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      Schema schema = getSchema(ctx);
      String fileName = schema.getMetadata().getName() + System.currentTimeMillis() + ".zip";

      ctx.contentType(ACCEPT_ZIP);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=" + fileName);

      Path zipFile = tempDir.resolve("download.zip");
      MolgenisIO.toZipFile(zipFile, schema, includeSystemColumns);
      outputStream.write(Files.readAllBytes(zipFile));

      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void getMetadata(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    sendZip(ctx, schema.getName() + "_schema", store -> Emx2.outputMetadata(store, schema));
  }

  static void mergeMetadata(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    TableStore store = uploadedZip(ctx);
    SchemaMetadata metadata = Emx2.fromRowList(store.readTable(MOLGENIS));
    timedOperation(ctx, "Merge metadata success", () -> schema.migrate(metadata));
  }

  static void discardMetadata(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    TableStore store = uploadedZip(ctx);
    SchemaMetadata metadata = Emx2.fromRowList(store.readTable(MOLGENIS));
    timedOperation(ctx, "Discard metadata success", () -> schema.discard(metadata));
  }

  static void getData(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    boolean includeSystemColumns = includeSystemColumns(ctx);
    sendZip(
        ctx,
        schema.getName() + "_data",
        store -> {
          boolean hasViewPermission =
              schema.getInheritedRolesForActiveUser().contains(Privileges.VIEWER.toString());
          for (String tableName : schema.getTableNames()) {
            Table table = schema.getTable(tableName);
            if (hasViewPermission
                || table.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
              if (includeSystemColumns) {
                org.molgenis.emx2.io.emx2.Emx2Tables.outputTableWithSystemColumns(store, table);
              } else {
                org.molgenis.emx2.io.emx2.Emx2Tables.outputTable(store, table);
              }
            }
          }
        });
  }

  static void postData(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    Path tempFile = uploadFileToTemp(ctx).toPath();
    if (isAsync(ctx)) {
      submitAsyncTask(ctx, schema, new ImportCsvZipTask(tempFile, schema, false));
    } else {
      timedOperation(
          ctx, "Import data success", () -> MolgenisIO.fromZipFile(tempFile, schema, false));
    }
  }

  static void getMembers(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema members");
    }
    sendZip(ctx, schema.getName() + "_members", store -> Emx2Members.outputRoles(store, schema));
  }

  static void getSettings(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    sendZip(
        ctx, schema.getName() + "_settings", store -> Emx2Settings.outputSettings(store, schema));
  }

  static void getChangelog(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema changelog");
    }
    int limit = parseIntParam(ctx, "limit").orElse(DEFAULT_CHANGELOG_LIMIT);
    int offset = parseIntParam(ctx, "offset").orElse(DEFAULT_CHANGELOG_OFFSET);
    sendZip(
        ctx,
        schema.getName() + "_changelog",
        store -> Emx2Changelog.outputChangelog(store, schema, limit, offset));
  }

  static void postAllZip(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    Path tempFile = uploadFileToTemp(ctx).toPath();
    if (isAsync(ctx)) {
      submitAsyncTask(ctx, schema, new ImportCsvZipTask(tempFile, schema, false));
    } else {
      timedOperation(ctx, "Import success", () -> MolgenisIO.fromZipFile(tempFile, schema, false));
    }
  }

  static void getZipTable(Context ctx) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    boolean includeSystemColumns = includeSystemColumns(ctx);
    if (table == null) throw new MolgenisException("Table " + ctx.pathParam(TABLE) + " unknown");
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      String tableName =
          table.getSchema().getMetadata().getName()
              + "_"
              + table.getName()
              + System.currentTimeMillis()
              + ".zip";
      ctx.contentType(ACCEPT_ZIP);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=" + tableName);

      Path zipFile = tempDir.resolve("download.zip");
      MolgenisIO.toZipFile(zipFile, table, includeSystemColumns);
      outputStream.write(Files.readAllBytes(zipFile));

      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void postZipTable(Context ctx) throws Exception {
    processZipTable(ctx, false);
  }

  static void deleteZipTable(Context ctx) throws Exception {
    processZipTable(ctx, true);
  }

  private static void processZipTable(Context ctx, boolean isDelete) throws Exception {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    TableStore store = uploadedZip(ctx);
    int count = processTableOperation(ctx, table, store, isDelete);
    ctx.status(200);
    ctx.result((isDelete ? "Deleted " : "Imported ") + count + " rows");
  }

  static void getZippedReports(Context ctx) throws IOException {
    Path tempDir =
        Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT); // NOSONAR
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      ctx.contentType(ACCEPT_ZIP);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=reports.zip");

      Path zipFile = tempDir.resolve("download.zip");
      TableStoreForCsvInZipFile store = new TableStoreForCsvInZipFile(zipFile);

      // take all the queries
      generateReportsToStore(ctx, store);

      // copy the zip to output
      outputStream.write(Files.readAllBytes(zipFile));
      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void generateReportsToStore(Context ctx, TableStore store) {
    String reports = ctx.queryParam("id");
    Schema schema = getSchema(ctx);
    Map<String, ?> parameters = getReportParameters(ctx);
    for (String reportId : reports.split(",")) {
      List<Row> rows = getReportAsRows(reportId, schema, parameters);
      if (rows.size() > 0) {
        store.writeTable(reportId, new ArrayList<>(rows.get(0).getColumnNames()), rows);
      } else {
        store.writeTable(reportId, new ArrayList<>(), rows);
      }
    }
  }

  @NotNull
  static Map<String, Object> getReportParameters(Context ctx) {
    Map<String, Object> parameters = new LinkedHashMap<>();
    ctx.queryParamMap()
        .forEach(
            (param, values) -> {
              if (!"id".equals(param)) {
                if (values.size() > 1) {
                  parameters.put(param, values);
                } else {
                  parameters.put(param, values.get(0));
                }
              }
            });

    return parameters;
  }
}
