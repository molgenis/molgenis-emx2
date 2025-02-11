package org.molgenis.emx2.web;

import static org.molgenis.emx2.io.FileUtils.getTempFile;
import static org.molgenis.emx2.web.CsvApi.getDownloadColumns;
import static org.molgenis.emx2.web.CsvApi.getDownloadRows;
import static org.molgenis.emx2.web.DownloadApiUtils.includeSystemColumns;
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
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportExcelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.tasks.Task;

public class ExcelApi {
  private ExcelApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    // schema level operations
    final String schemaPath = "/{schema}/api/excel"; // NOSONAR
    app.get(schemaPath, ExcelApi::getExcel);
    app.post(schemaPath, ExcelApi::postExcel);

    // table level operations
    final String tablePath = "{schema}/api/excel/{table}"; // NOSONAR
    app.get(tablePath, ExcelApi::getExcelTable);

    // report operations
    final String reportPath = "/{schema}/api/reports/excel"; // NOSONAR
    app.get(reportPath, ExcelApi::getExcelReport);
  }

  static void postExcel(Context ctx) throws IOException, ServletException {
    long start = System.currentTimeMillis();
    Schema schema = getSchema(ctx);

    // get uploaded file
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

  static void getExcel(Context ctx) throws IOException {
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

    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      ctx.header(
          "Content-Disposition",
          "attachment; filename="
              + schema.getMetadata().getName()
              + System.currentTimeMillis()
              + ".xlsx");
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
