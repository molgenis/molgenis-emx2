package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.TABLE;
import static org.molgenis.emx2.web.DownloadApiUtils.includeSystemColumns;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.FileUtils;
import org.molgenis.emx2.io.ImportCsvZipTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.tasks.Task;

public class ZipApi {
  private ZipApi() {
    // hide constructor
  }

  static final String APPLICATION_ZIP_MIME_TYPE = "application/zip";
  static final String CONTENT_DISPOSITION = "Content-Disposition";

  public static void create(Javalin app) {
    // schema level operations
    final String schemaPath = "/{schema}/api/zip"; // NOSONAR
    app.get(schemaPath, ZipApi::getZip);
    app.post(schemaPath, ZipApi::postZip);

    // table level operations
    final String tablePath = "/{schema}/api/zip/{table}"; // NOSONAR
    app.get(tablePath, ZipApi::getZipTable);

    // report operations
    final String reportPath = "/{schema}/api/reports/zip"; // NOSONAR
    app.get(reportPath, ZipApi::getZippedReports);
  }

  static void getZip(Context ctx) throws IOException {
    boolean includeSystemColumns = includeSystemColumns(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      Schema schema = getSchema(ctx);
      String fileName = schema.getMetadata().getName() + System.currentTimeMillis() + ".zip";

      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
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

  static void postZip(Context ctx) throws MolgenisException, IOException, ServletException {
    Long start = System.currentTimeMillis();
    Schema schema = getSchema(ctx);
    // get uploaded file
    File tempFile = File.createTempFile("temp_", ".tmp");
    try {
      ctx.attribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(tempFile.getAbsolutePath()));
      try (InputStream input = ctx.req().getPart("file").getInputStream()) {
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      // depending on file extension use proper importer
      String fileName = ctx.req().getPart("file").getSubmittedFileName();

      if (fileName.endsWith(".zip")) {
        Task task = new ImportCsvZipTask(tempFile.toPath(), schema, false);
        if (ctx.queryParam("async") != null) {
          String parentTaskId = ctx.queryParam("parentJob");
          String id = TaskApi.submit(task, parentTaskId);
          ctx.json(new TaskReference(id, schema));
          return;
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

      ctx.status(200);
      ctx.result("Import success in " + (System.currentTimeMillis() - start) + "ms");
    } finally {
      if (ctx.queryParam("async") == null) {
        Files.delete(tempFile.toPath());
      }
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
      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
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

  static void getZippedReports(Context ctx) throws IOException {
    Path tempDir =
        Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT); // NOSONAR
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=reports.zip");

      FileUtils.getTempFile("download", ".zip");
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

  static void generateReportsToStore(
      @org.jetbrains.annotations.NotNull Context ctx, TableStore store)
      throws JsonProcessingException {
    String reports = ctx.queryParam("id");
    Schema schema = getSchema(ctx);
    Map<String, ?> parameters = getReportParameters(ctx);
    String reportsJson = schema.getMetadata().getSetting("reports");
    List<Map<String, String>> reportList = new ObjectMapper().readValue(reportsJson, List.class);
    for (String reportId : reports.split(",")) {
      // first find report object based on id
      Optional<Map<String, String>> found =
          reportList.stream()
              .filter(reportDefinition -> reportId.equals(reportDefinition.get("id")))
              .findFirst();
      Map<String, String> reportObject = null;
      if (found.isPresent()) {
        reportObject = found.get();
      } else {
        reportObject = reportList.get(Integer.parseInt(reportId));
      }
      if (reportObject == null) {
        throw new MolgenisException("Cannot find report id=" + reportId);
      }
      String sql = reportObject.get("sql");
      String name = reportObject.get("name");
      List<Row> rows = schema.retrieveSql(sql, parameters);
      if (rows.size() > 0) {
        store.writeTable(name, new ArrayList<>(rows.get(0).getColumnNames()), rows);
      } else {
        store.writeTable(name, new ArrayList<>(), rows);
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
