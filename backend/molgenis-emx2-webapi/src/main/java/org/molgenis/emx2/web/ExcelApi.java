package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.TABLE;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.getTable;
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
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.ImportExcelTask;
import org.molgenis.emx2.io.MolgenisIO;
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

    //    final String _metaPath = "/:schema/api/excel/_meta"; // NOSONAR
    //    get(_metaPath, ExcelApi::getExcelMetadata);
    //    post(_metaPath, ExcelApi::postExcelMetadata);

    // table level operations
    final String tablePath = ":schema/api/excel/:table"; // NOSONAR
    get(tablePath, ExcelApi::getExcelTable);
  }

  static Object postExcel(Request request, Response response) throws IOException, ServletException {
    Long start = System.currentTimeMillis();
    Schema schema = getSchema(request);

    // get uploaded file
    File tempFile = File.createTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
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

    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Path excelFile = tempDir.resolve("download.xlsx");
      if (request.queryParams("emx1") != null) {
        MolgenisIO.toEmx1ExcelFile(excelFile, schema);
      } else {
        MolgenisIO.toExcelFile(excelFile, schema);
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
    Table table = getTable(request);
    if (table == null) throw new MolgenisException("Table " + request.params(TABLE) + " unknown");
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Path excelFile = tempDir.resolve("download.xlsx");
      MolgenisIO.toExcelFile(excelFile, table);
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
}
