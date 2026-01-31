package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.io.FileUtils.getTempFile;
import static org.molgenis.emx2.web.CsvApi.getDownloadColumns;
import static org.molgenis.emx2.web.CsvApi.getDownloadRows;
import static org.molgenis.emx2.web.DownloadApiUtils.includeSystemColumns;
import static org.molgenis.emx2.web.DownloadApiUtils.isManagerOrOwnerOfSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.ZipApi.generateReportsToStore;

import io.javalin.Javalin;
import io.javalin.http.Context;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportExcelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Changelog;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.tasks.Task;

public class ExcelApi {
  private static final int DEFAULT_CHANGELOG_LIMIT = 100;
  private static final int DEFAULT_CHANGELOG_OFFSET = 0;

  private ExcelApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    final String schemaPath = "/{schema}/api/excel/_schema";
    app.get(schemaPath, ExcelApi::getMetadata);
    app.post(schemaPath, ExcelApi::postMetadata);
    app.delete(schemaPath, ExcelApi::deleteMetadata);

    app.get("/{schema}/api/excel/_all", ExcelApi::getAll);
    app.post("/{schema}/api/excel/_all", ExcelApi::postAll);
    app.get("/{schema}/api/excel/_data", ExcelApi::getData);
    app.post("/{schema}/api/excel/_data", ExcelApi::postData);
    app.get("/{schema}/api/excel/_members", ExcelApi::getMembers);
    app.get("/{schema}/api/excel/_settings", ExcelApi::getSettings);
    app.get("/{schema}/api/excel/_changelog", ExcelApi::getChangelog);

    final String tablePath = "{schema}/api/excel/{table}";
    app.get(tablePath, ExcelApi::getExcelTable);

    final String reportPath = "/{schema}/api/reports/excel";
    app.get(reportPath, ExcelApi::getExcelReport);
  }

  static void getMetadata(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore store = new TableStoreForXlsxFile(excelFile);
    Emx2.outputMetadata(store, schema);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename=\"" + schema.getName() + "_schema_" + date + ".xlsx\"");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }

  static void postMetadata(Context ctx) throws IOException, ServletException {
    Schema schema = getSchema(ctx);
    File tempFile = getTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    ctx.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = ctx.req().getPart("file").getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    TableStore store = new TableStoreForXlsxFile(tempFile.toPath());
    SchemaMetadata metadata = Emx2.fromRowList(store.readTable("molgenis"));
    schema.migrate(metadata);
    ctx.status(200);
    ctx.result("Metadata import success");
  }

  static void deleteMetadata(Context ctx) throws IOException, ServletException {
    Schema schema = getSchema(ctx);
    File tempFile = getTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    ctx.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = ctx.req().getPart("file").getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    TableStore store = new TableStoreForXlsxFile(tempFile.toPath());
    SchemaMetadata metadata = Emx2.fromRowList(store.readTable("molgenis"));
    schema.discard(metadata);
    ctx.status(200);
    ctx.result("Metadata discard success");
  }

  static void getAll(Context ctx) throws IOException {
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
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename=\"" + schema.getName() + "_" + date + ".xlsx\"");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }

  static void postAll(Context ctx) throws IOException, ServletException {
    long start = System.currentTimeMillis();
    Schema schema = getSchema(ctx);
    File tempFile = getTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    ctx.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = ctx.req().getPart("file").getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    if (ctx.queryParam("async") != null) {
      Task task = new ImportExcelTask(tempFile.toPath(), schema, false);
      String parentTaskId = ctx.queryParam("parentJob");
      String taskId = TaskApi.submit(task, parentTaskId);
      ctx.json(new TaskReference(taskId, schema));
    } else {
      MolgenisIO.importFromExcelFile(tempFile.toPath(), schema, false);
      ctx.status(200);
      ctx.result("Import success in " + (System.currentTimeMillis() - start) + "ms");
    }
  }

  static void getData(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    boolean includeSystemColumns = includeSystemColumns(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore store = new TableStoreForXlsxFile(excelFile);
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      store.writeTable(tableName, getDownloadColumns(ctx, table), getDownloadRows(ctx, table));
    }
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename=\"" + schema.getName() + "_data_" + date + ".xlsx\"");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }

  static void postData(Context ctx) throws IOException, ServletException {
    long start = System.currentTimeMillis();
    Schema schema = getSchema(ctx);
    File tempFile = getTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    ctx.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = ctx.req().getPart("file").getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    if (ctx.queryParam("async") != null) {
      Task task = new ImportExcelTask(tempFile.toPath(), schema, false);
      String parentTaskId = ctx.queryParam("parentJob");
      String taskId = TaskApi.submit(task, parentTaskId);
      ctx.json(new TaskReference(taskId, schema));
    } else {
      MolgenisIO.importFromExcelFile(tempFile.toPath(), schema, false);
      ctx.status(200);
      ctx.result("Data import success in " + (System.currentTimeMillis() - start) + "ms");
    }
  }

  static void getMembers(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema members");
    }
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore store = new TableStoreForXlsxFile(excelFile);
    Emx2Members.outputRoles(store, schema);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename=\"" + schema.getName() + "_members_" + date + ".xlsx\"");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }

  static void getSettings(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore store = new TableStoreForXlsxFile(excelFile);
    Emx2Settings.outputSettings(store, schema);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename=\"" + schema.getName() + "_settings_" + date + ".xlsx\"");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }

  static void getChangelog(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema changelog");
    }
    int limit = DownloadApiUtils.parseIntParam(ctx, "limit").orElse(DEFAULT_CHANGELOG_LIMIT);
    int offset = DownloadApiUtils.parseIntParam(ctx, "offset").orElse(DEFAULT_CHANGELOG_OFFSET);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore store = new TableStoreForXlsxFile(excelFile);
    Emx2Changelog.outputChangelog(store, schema, limit, offset);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename=\"" + schema.getName() + "_changelog_" + date + ".xlsx\"");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }

  static void getExcelTable(Context ctx) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore excelStore = new TableStoreForXlsxFile(excelFile);
    excelStore.writeTable(
        table.getName(), getDownloadColumns(ctx, table), getDownloadRows(ctx, table));
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename="
              + table.getSchema().getMetadata().getName()
              + "_"
              + table.getName()
              + System.currentTimeMillis()
              + ".xlsx");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }

  static void getExcelReport(Context ctx) throws IOException {
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path excelFile = tempDir.resolve("download.xlsx");
    TableStore excelStore = new TableStoreForXlsxFile(excelFile);
    generateReportsToStore(ctx, excelStore);
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header("Content-Disposition", "attachment; filename=report.xlsx");
      outputStream.write(Files.readAllBytes(excelFile));
      ctx.result("Export success");
    }
  }
}
