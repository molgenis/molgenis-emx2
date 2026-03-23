package org.molgenis.emx2.web;

import static org.molgenis.emx2.io.FileUtils.getTempFile;
import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.sql.SqlSchemaMetadata;
import org.molgenis.emx2.tasks.Task;

public class DownloadApiUtils {

  static final String MULTIPART_CONFIG = "org.eclipse.jetty.multipartConfig";
  static final String FILE_PARAM = "file";
  static final String EXCEL_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  static final String ZIP_CONTENT_TYPE = "application/zip";

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  private DownloadApiUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  static boolean includeSystemColumns(Context ctx) {
    return String.valueOf(ctx.queryParam(INCLUDE_SYSTEM_COLUMNS)).equalsIgnoreCase("true");
  }

  static boolean isManagerOrOwnerOfSchema(Context ctx, Schema schema) {
    String currentUser = new MolgenisSessionHandler(ctx.req()).getCurrentUser();
    SqlSchemaMetadata sqlSchemaMetadata =
        new SqlSchemaMetadata(schema.getDatabase(), schema.getName());
    List<String> roles = sqlSchemaMetadata.getInheritedRolesForUser(currentUser);
    return roles.contains(Privileges.MANAGER.toString())
        || roles.contains(Privileges.OWNER.toString());
  }

  static Optional<Integer> parseIntParam(Context ctx, String param) {
    return Optional.ofNullable(ctx.queryParam(param))
        .map(
            arg -> {
              try {
                return Integer.valueOf(arg);
              } catch (NumberFormatException e) {
                throw new MolgenisException(
                    "Invalid " + param + " provided, should be a number", e);
              }
            });
  }

  static File uploadFileToTemp(Context ctx) throws IOException, ServletException {
    File tempFile = getTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    ctx.attribute(MULTIPART_CONFIG, new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = ctx.req().getPart(FILE_PARAM).getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    return tempFile;
  }

  static int processTableOperation(Context ctx, Table table, TableStore store, boolean isDelete)
      throws IOException {
    Iterable<Row> rows = store.readTable(table.getName());
    return isDelete ? table.delete(rows) : table.save(rows);
  }

  static String extractIdFromPath(Context ctx, Table table) {
    String tablePath = "/" + table.getName() + "/";
    int tableIndex = ctx.path().indexOf(tablePath);
    if (tableIndex == -1) {
      throw new MolgenisException("Invalid path for table " + table.getName());
    }
    return ctx.path().substring(tableIndex + tablePath.length());
  }

  static void validatePrimaryKeyCount(List<String> primaryKeyNames) {
    if (primaryKeyNames.isEmpty()) {
      throw new MolgenisException("Table has no primary key");
    }
  }

  static void validateCompositeKeyParts(String[] parts, List<String> primaryKeyNames) {
    if (parts.length != primaryKeyNames.size()) {
      throw new MolgenisException(
          "Composite primary key requires "
              + primaryKeyNames.size()
              + " values separated by /, got "
              + parts.length);
    }
  }

  @FunctionalInterface
  interface ThrowingRunnable {
    void run() throws Exception;
  }

  @FunctionalInterface
  interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;
  }

  static void submitAsyncTask(Context ctx, Schema schema, Task task) {
    String parentTaskId = ctx.queryParam("parentJob");
    String taskId = TaskApi.submit(task, parentTaskId);
    ctx.json(new TaskReference(taskId, schema));
  }

  static void timedOperation(Context ctx, String message, ThrowingRunnable action)
      throws Exception {
    long start = System.currentTimeMillis();
    action.run();
    ctx.status(200);
    ctx.result(message + " in " + (System.currentTimeMillis() - start) + "ms");
  }

  static boolean isAsync(Context ctx) {
    return ctx.queryParam("async") != null;
  }

  static void sendFileResponse(Context ctx, Path file, String contentType, String filename)
      throws IOException {
    String date = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    try (OutputStream outputStream = ctx.outputStream()) {
      ctx.contentType(contentType);
      ctx.header("Content-Disposition", "attachment; filename=\"" + filename + "_" + date + "\"");
      outputStream.write(Files.readAllBytes(file));
    }
  }

  static void sendFileDownload(
      Context ctx,
      String filename,
      String contentType,
      Function<Path, TableStore> storeFactory,
      ThrowingConsumer<TableStore> writer)
      throws Exception {
    Path tempDir = Files.createTempDirectory(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    Path file = tempDir.resolve("download");
    TableStore store = storeFactory.apply(file);
    writer.accept(store);
    sendFileResponse(ctx, file, contentType, filename);
  }

  static TableStore uploadedStore(Context ctx, Function<Path, TableStore> storeFactory)
      throws IOException, ServletException {
    File tempFile = uploadFileToTemp(ctx);
    return storeFactory.apply(tempFile.toPath());
  }
}
