package org.molgenis.emx2.web;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.SchemaExport;
import org.molgenis.emx2.io.SchemaImport;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static spark.Spark.get;
import static spark.Spark.post;

public class ExcelApi {

  public static void create() {
    // schema level operations
    final String schemaPath = "/api/excel/:schema"; // NOSONAR
    get(schemaPath, ExcelApi::getExcel);
    post(schemaPath, ExcelApi::postExcel);
  }

  static String postExcel(Request request, Response response) throws IOException, ServletException {
    Long start = System.currentTimeMillis();
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));
    // get uploaded file
    File tempFile = File.createTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    request.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = request.raw().getPart("file").getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // depending on file extension use proper importer
    String fileName = request.raw().getPart("file").getSubmittedFileName();
    SchemaImport.fromExcelFile(tempFile.toPath(), schema);
    response.status(200);
    return "Import success in " + (System.currentTimeMillis() - start) + "ms";
  }

  static String getExcel(Request request, Response response) throws IOException {
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Path excelFile = tempDir.resolve("download.xlsx");
      SchemaExport.toExcelFile(excelFile, schema);
      outputStream.write(Files.readAllBytes(excelFile));
      response.type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.header(
          "Content-Disposition",
          "attachment; filename="
              + schema.getMetadata().getName()
              + System.currentTimeMillis()
              + ".xlsx");
      return "Export success";
    }
  }
}
