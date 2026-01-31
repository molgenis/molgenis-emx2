package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.io.ImportMetadataTask.MOLGENIS;
import static org.molgenis.emx2.settings.ReportUtils.getReportAsRows;
import static org.molgenis.emx2.web.Constants.TABLE;
import static org.molgenis.emx2.web.DownloadApiUtils.includeSystemColumns;
import static org.molgenis.emx2.web.DownloadApiUtils.isManagerOrOwnerOfSchema;
import static org.molgenis.emx2.web.DownloadApiUtils.parseIntParam;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

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
import java.util.*;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.FileUtils;
import org.molgenis.emx2.io.ImportCsvZipTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Changelog;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.tasks.Task;

public class ZipApi {
  private ZipApi() {
    // hide constructor
  }

  static final String APPLICATION_ZIP_MIME_TYPE = "application/zip";
  static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final int DEFAULT_CHANGELOG_LIMIT = 100;
  private static final int DEFAULT_CHANGELOG_OFFSET = 0;

  public static void create(Javalin app) {
    final String schemaAllPath = "/{schema}/api/zip/_all";
    app.get(schemaAllPath, ZipApi::getAllZip);
    app.post(schemaAllPath, ZipApi::postAllZip);

    final String schemaMetadataPath = "/{schema}/api/zip/_schema";
    app.get(schemaMetadataPath, ZipApi::getMetadata);
    app.post(schemaMetadataPath, ZipApi::mergeMetadata);
    app.delete(schemaMetadataPath, ZipApi::discardMetadata);

    final String dataPath = "/{schema}/api/zip/_data";
    app.get(dataPath, ZipApi::getData);
    app.post(dataPath, ZipApi::postData);

    final String membersPath = "/{schema}/api/zip/_members";
    app.get(membersPath, ZipApi::getMembers);

    final String settingsPath = "/{schema}/api/zip/_settings";
    app.get(settingsPath, ZipApi::getSettings);

    final String changelogPath = "/{schema}/api/zip/_changelog";
    app.get(changelogPath, ZipApi::getChangelog);

    final String tablePath = "/{schema}/api/zip/{table}";
    app.get(tablePath, ZipApi::getZipTable);

    final String reportPath = "/{schema}/api/reports/zip";
    app.get(reportPath, ZipApi::getZippedReports);
  }

  static void getAllZip(Context ctx) throws IOException {
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

  static void getMetadata(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
      String fileName = schema.getName() + "_schema_" + date + ".zip";

      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

      Path zipFile = tempDir.resolve("download.zip");
      TableStore store = new TableStoreForCsvInZipFile(zipFile);
      Emx2.outputMetadata(store, schema);

      outputStream.write(Files.readAllBytes(zipFile));
      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void mergeMetadata(Context ctx) throws IOException, ServletException {
    Schema schema = getSchema(ctx);
    File tempFile = File.createTempFile("temp_", ".tmp");
    try {
      ctx.attribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(tempFile.getAbsolutePath()));
      try (InputStream input = ctx.req().getPart("file").getInputStream()) {
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      String fileName = ctx.req().getPart("file").getSubmittedFileName();
      if (fileName.endsWith(".zip")) {
        TableStore store = new TableStoreForCsvInZipFile(tempFile.toPath());
        if (store.containsTable(MOLGENIS)) {
          SchemaMetadata metadata = Emx2.fromRowList(store.readTable(MOLGENIS));
          schema.migrate(metadata);
        } else {
          throw new MolgenisException("ZIP file does not contain schema metadata (molgenis table)");
        }
      } else {
        throw new IOException(
            "File upload failed: extension "
                + fileName.substring(fileName.lastIndexOf('.'))
                + " not supported");
      }

      ctx.status(200);
      ctx.result("Merge metadata success");
    } finally {
      Files.delete(tempFile.toPath());
    }
  }

  static void discardMetadata(Context ctx) throws IOException, ServletException {
    Schema schema = getSchema(ctx);
    File tempFile = File.createTempFile("temp_", ".tmp");
    try {
      ctx.attribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(tempFile.getAbsolutePath()));
      try (InputStream input = ctx.req().getPart("file").getInputStream()) {
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      String fileName = ctx.req().getPart("file").getSubmittedFileName();
      if (fileName.endsWith(".zip")) {
        TableStore store = new TableStoreForCsvInZipFile(tempFile.toPath());
        if (store.containsTable(MOLGENIS)) {
          SchemaMetadata metadata = Emx2.fromRowList(store.readTable(MOLGENIS));
          schema.discard(metadata);
        } else {
          throw new MolgenisException("ZIP file does not contain schema metadata (molgenis table)");
        }
      } else {
        throw new IOException(
            "File upload failed: extension "
                + fileName.substring(fileName.lastIndexOf('.'))
                + " not supported");
      }

      ctx.status(200);
      ctx.result("Discard metadata success");
    } finally {
      Files.delete(tempFile.toPath());
    }
  }

  static void getData(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    boolean includeSystemColumns = includeSystemColumns(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
      String fileName = schema.getName() + "_data_" + date + ".zip";

      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

      Path zipFile = tempDir.resolve("download.zip");
      TableStore store = new TableStoreForCsvInZipFile(zipFile);

      boolean hasViewPermission =
          schema.getInheritedRolesForActiveUser().contains(Privileges.VIEWER.toString());
      for (String tableName : schema.getTableNames()) {
        Table table = schema.getTable(tableName);
        if (hasViewPermission || table.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
          if (includeSystemColumns) {
            org.molgenis.emx2.io.emx2.Emx2Tables.outputTableWithSystemColumns(store, table);
          } else {
            org.molgenis.emx2.io.emx2.Emx2Tables.outputTable(store, table);
          }
        }
      }

      outputStream.write(Files.readAllBytes(zipFile));
      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void postData(Context ctx) throws IOException, ServletException {
    Schema schema = getSchema(ctx);
    File tempFile = File.createTempFile("temp_", ".tmp");
    try {
      ctx.attribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(tempFile.getAbsolutePath()));
      try (InputStream input = ctx.req().getPart("file").getInputStream()) {
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

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
      } else {
        throw new IOException(
            "File upload failed: extension "
                + fileName.substring(fileName.lastIndexOf('.'))
                + " not supported");
      }

      ctx.status(200);
      ctx.result("Import data success");
    } finally {
      if (ctx.queryParam("async") == null) {
        Files.delete(tempFile.toPath());
      }
    }
  }

  static void getMembers(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema members");
    }

    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
      String fileName = schema.getName() + "_members_" + date + ".zip";

      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

      Path zipFile = tempDir.resolve("download.zip");
      TableStore store = new TableStoreForCsvInZipFile(zipFile);
      Emx2Members.outputRoles(store, schema);

      outputStream.write(Files.readAllBytes(zipFile));
      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void getSettings(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
      String fileName = schema.getName() + "_settings_" + date + ".zip";

      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

      Path zipFile = tempDir.resolve("download.zip");
      TableStore store = new TableStoreForCsvInZipFile(zipFile);
      Emx2Settings.outputSettings(store, schema);

      outputStream.write(Files.readAllBytes(zipFile));
      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void getChangelog(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (!isManagerOrOwnerOfSchema(ctx, schema)) {
      throw new MolgenisException("Unauthorized to get schema changelog");
    }

    int limit = parseIntParam(ctx, "limit").orElse(DEFAULT_CHANGELOG_LIMIT);
    int offset = parseIntParam(ctx, "offset").orElse(DEFAULT_CHANGELOG_OFFSET);

    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = ctx.res().getOutputStream()) {
      String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
      String fileName = schema.getName() + "_changelog_" + date + ".zip";

      ctx.contentType(APPLICATION_ZIP_MIME_TYPE);
      ctx.header(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

      Path zipFile = tempDir.resolve("download.zip");
      TableStore store = new TableStoreForCsvInZipFile(zipFile);
      Emx2Changelog.outputChangelog(store, schema, limit, offset);

      outputStream.write(Files.readAllBytes(zipFile));
      ctx.result("Export success");
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  static void postAllZip(Context ctx) throws MolgenisException, IOException, ServletException {
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
