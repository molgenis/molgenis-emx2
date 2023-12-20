package org.molgenis.emx2.web;

import static org.molgenis.emx2.io.FileUtils.getTempFile;
import static org.molgenis.emx2.web.CsvApi.getDownloadColumns;
import static org.molgenis.emx2.web.CsvApi.getDownloadRows;
import static org.molgenis.emx2.web.DownloadApiUtils.includeSystemColumns;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.ZipApi.generateReportsToStore;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportExcelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import spark.Request;
import spark.Response;

public class ExcelApi {
  private ExcelApi() {
    // hide constructor
  }

  public static void create() {
    // schema level operations
    final String schemaPath = "/:schema/api/excel"; // NOSONAR
    get(schemaPath, ExcelApi::getExcel);
    post(schemaPath, ExcelApi::postExcel);

    // table level operations
    final String tablePath = ":schema/api/excel/:table"; // NOSONAR
    get(tablePath, ExcelApi::getExcelTable);

    // report operations
    final String reportPath = "/:schema/api/reports/excel"; // NOSONAR
    get(reportPath, ExcelApi::getExcelReport);
  }

  static Object postExcel(Request request, Response response) throws IOException, ServletException {
    Long start = System.currentTimeMillis();
    Schema schema = getSchema(request);

    // get uploaded file
    File tempFile = getTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    request.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = request.raw().getPart("file").getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    if (request.queryParams("async") != null) {
      String id = TaskApi.submit(new ImportExcelTask(tempFile.toPath(), schema, false));
      return new TaskReference(id, schema).toString();
    } else {
      MolgenisIO.importFromExcelFile(tempFile.toPath(), schema, false);
      response.status(200);
      return "Import success in " + (System.currentTimeMillis() - start) + "ms";
    }
  }

  static String getExcel(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    boolean includeSystemColumns = includeSystemColumns(request);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Path excelFile = tempDir.resolve("download.xlsx");
      if (request.queryParams("emx1") != null) {
        MolgenisIO.toEmx1ExcelFile(excelFile, schema);
      } else {
        MolgenisIO.toExcelFile(excelFile, schema, includeSystemColumns);
      }

      response.type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.header(
          "Content-Disposition",
          "attachment; filename="
              + schema.getMetadata().getName()
              + System.currentTimeMillis()
              + ".xlsx");
      outputStream.write(Files.readAllBytes(excelFile));
      return "Export success";
    }
  }

  static String getExcelTable(Request request, Response response) throws IOException {
    Table table = MolgenisWebservice.getTableById(request);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Path excelFile = tempDir.resolve("download.xlsx");
      TableStore excelStore = new TableStoreForXlsxFile(excelFile);
      excelStore.writeTable(
          table.getName(), getDownloadColumns(request, table), getDownloadRows(request, table));
      response.type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.header(
          "Content-Disposition",
          "attachment; filename="
              + table.getSchema().getMetadata().getName()
              + "_"
              + table.getName()
              + System.currentTimeMillis()
              + ".xlsx");
      outputStream.write(Files.readAllBytes(excelFile));
      return "Export success";
    }
  }

  static String getExcelReport(Request request, Response response) throws IOException {
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Path excelFile = tempDir.resolve("download.xlsx");
      TableStore excelStore = new TableStoreForXlsxFile(excelFile);
      generateReportsToStore(request, excelStore);
      response.type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.header("Content-Disposition", "attachment; filename=report.xlsx");
      outputStream.write(Files.readAllBytes(excelFile));
      return "Export success";
    }
  }
}
