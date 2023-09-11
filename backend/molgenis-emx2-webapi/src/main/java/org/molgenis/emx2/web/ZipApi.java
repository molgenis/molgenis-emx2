package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.TABLE;
import static org.molgenis.emx2.web.DownloadApiUtils.includeSystemColumns;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.getTable;
import static spark.Spark.get;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.FileUtils;
import org.molgenis.emx2.io.ImportCsvZipTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import spark.Request;
import spark.Response;

public class ZipApi {
  private ZipApi() {
    // hide constructor
  }

  static final String APPLICATION_ZIP_MIME_TYPE = "application/zip";
  static final String CONTENT_DISPOSITION = "Content-Disposition";

  public static void create() {
    // schema level operations
    final String schemaPath = "/:schema/api/zip"; // NOSONAR
    get(schemaPath, ZipApi::getZip);
    post(schemaPath, ZipApi::postZip);

    // table level operations
    final String tablePath = "/:schema/api/zip/:table"; // NOSONAR
    get(tablePath, ZipApi::getZipTable);

    // report operations
    final String reportPath = "/:schema/api/reports/zip"; // NOSONAR
    get(reportPath, ZipApi::getZippedReports);
  }

  static String getZip(Request request, Response response) throws IOException {
    boolean includeSystemColumns = includeSystemColumns(request);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Schema schema = getSchema(request);
      String fileName = schema.getMetadata().getName() + System.currentTimeMillis() + ".zip";

      response.type(APPLICATION_ZIP_MIME_TYPE);
      response.header(CONTENT_DISPOSITION, "attachment; filename=" + fileName);

      Path zipFile = tempDir.resolve("download.zip");
      MolgenisIO.toZipFile(zipFile, schema, includeSystemColumns);
      outputStream.write(Files.readAllBytes(zipFile));

      return "Export success";
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static String postZip(Request request, Response response)
      throws MolgenisException, IOException, ServletException {
    Long start = System.currentTimeMillis();
    Schema schema = getSchema(request);
    // get uploaded file
    File tempFile = File.createTempFile("temp_", ".tmp");
    try {
      request.attribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(tempFile.getAbsolutePath()));
      try (InputStream input = request.raw().getPart("file").getInputStream()) {
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      // depending on file extension use proper importer
      String fileName = request.raw().getPart("file").getSubmittedFileName();

      if (fileName.endsWith(".zip")) {
        if (request.queryParams("async") != null) {
          String id = TaskApi.submit(new ImportCsvZipTask(tempFile.toPath(), schema, false));
          return new TaskReference(id, schema).toString();
        } else {
          MolgenisIO.fromZipFile(tempFile.toPath(), schema, false);
        }
      } else if (fileName.endsWith(".xlsx")) {
        MolgenisIO.importFromExcelFile(tempFile.toPath(), schema, false);
      } else {
        throw new IOException(
            "File upload failed: extension "
                + fileName.substring(fileName.lastIndexOf('.'))
                + " not supported");
      }

      response.status(200);
      return "Import success in " + (System.currentTimeMillis() - start) + "ms";
    } finally {
      if (request.queryParams("async") == null) {
        Files.delete(tempFile.toPath());
      }
    }
  }

  static String getZipTable(Request request, Response response) throws IOException {
    Table table = getTable(request);
    boolean includeSystemColumns = includeSystemColumns(request);
    if (table == null) throw new MolgenisException("Table " + request.params(TABLE) + " unknown");
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      String tableName =
          table.getSchema().getMetadata().getName()
              + "_"
              + table.getName()
              + System.currentTimeMillis()
              + ".zip";
      response.type(APPLICATION_ZIP_MIME_TYPE);
      response.header(CONTENT_DISPOSITION, "attachment; filename=" + tableName);

      Path zipFile = tempDir.resolve("download.zip");
      MolgenisIO.toZipFile(zipFile, table, includeSystemColumns);
      outputStream.write(Files.readAllBytes(zipFile));

      return "Export success";
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static String getZippedReports(Request request, Response response) throws IOException {
    Path tempDir =
        Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT); // NOSONAR
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      response.type(APPLICATION_ZIP_MIME_TYPE);
      response.header(CONTENT_DISPOSITION, "attachment; filename=reports.zip");

      FileUtils.getTempFile("download", ".zip");
      Path zipFile = tempDir.resolve("download.zip");
      TableStoreForCsvInZipFile store = new TableStoreForCsvInZipFile(zipFile);

      // take all the queries
      generateReportsToStore(request, store);

      // copy the zip to output
      outputStream.write(Files.readAllBytes(zipFile));
      return "Export success";
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void generateReportsToStore(Request request, TableStore store)
      throws JsonProcessingException {
    String reports = request.queryParams("id");
    Schema schema = getSchema(request);
    Map<String, Object> parameters = getReportParameters(request);
    String reportsJson = schema.getMetadata().getSetting("reports");
    List<Map<String, String>> reportList = new ObjectMapper().readValue(reportsJson, List.class);
    for (String reportId : reports.split(",")) {
      Map reportObject = reportList.get(Integer.parseInt(reportId));
      String sql = (String) reportObject.get("sql");
      String name = (String) reportObject.get("name");
      List<Row> rows = schema.retrieveSql(sql, parameters);
      store.writeTable(name, new ArrayList<>(rows.get(0).getColumnNames()), rows);
    }
  }

  @NotNull
  static Map<String, Object> getReportParameters(Request request) {
    Map<String, Object> parameters = new LinkedHashMap<>();
    request
        .queryParams()
        .forEach(
            param -> {
              if ("id".equals(param)) {
                return;
              } else if (request.queryParamsValues(param).length > 1) {
                parameters.put(param, List.of(request.queryParamsValues(param)));
              } else {
                parameters.put(param, request.queryParams(param));
              }
            });
    return parameters;
  }
}
