package org.molgenis.emx2.fairmapper.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import okhttp3.*;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.web.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteDataLoader implements DataLoader {

  private static final Logger logger = LoggerFactory.getLogger(RemoteDataLoader.class);
  private static final MediaType ZIP = MediaType.parse(Constants.ACCEPT_ZIP);

  private static final OkHttpClient OK_HTTP_CLIENT =
      new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(60)).build();

  private final String schema;
  private final String[] tables;
  private final URL endpoint;
  private final String token;

  public RemoteDataLoader(String endpoint, String token, String schema, String[] tables)
      throws MalformedURLException {
    this.schema = schema;
    this.tables = tables;
    this.endpoint = URI.create(endpoint).resolve(schema + "/api/zip").toURL();
    this.token = token;
  }

  @Override
  public void load(InMemoryTableStore tableStore) {
    try {
      // Suppressing because the directory creaFiles.createTempDirectory is owner-only
      @SuppressWarnings("java:S5443")
      Path tempDir = Files.createTempDirectory("remote-data-loader-" + schema);
      Path zipPath = tempDir.resolve(schema + ".zip");
      try {
        writeTableStoreToZip(tableStore, zipPath);
        upload(zipPath);
      } finally {
        deleteTempFiles(zipPath, tempDir);
      }
    } catch (IOException e) {
      throw new MolgenisException("Unable to stage zip file for upload", e);
    }
  }

  private void deleteTempFiles(Path zipPath, Path tempDir) throws IOException {
    Files.deleteIfExists(zipPath);
    Files.deleteIfExists(tempDir);
  }

  private void upload(Path zipPath) {
    Request request =
        new Request.Builder()
            .url(endpoint)
            .header(Constants.X_MOLGENIS_TOKEN, token)
            .post(requestBody(zipPath))
            .build();

    logger.info("Uploading data to table store: {}", zipPath);
    try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new MolgenisException("Unexpected code " + response);
      }
    } catch (IOException e) {
      throw new MolgenisException("Something went wrong when uploading zip data", e);
    }
  }

  private RequestBody requestBody(Path zipPath) {
    return new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file", zipPath.getFileName().toString(), RequestBody.create(zipPath.toFile(), ZIP))
        .build();
  }

  private void writeTableStoreToZip(InMemoryTableStore store, Path zipPath) {
    TableStoreForCsvInZipFile zip = new TableStoreForCsvInZipFile(zipPath);
    for (String tableName : tables) {
      List<String> columnNames =
          StreamSupport.stream(store.readTable(tableName).spliterator(), false)
              .flatMap(row -> row.getColumnNames().stream())
              .distinct()
              .toList();

      zip.writeTable(tableName, columnNames, store.readTable(tableName));
    }
  }
}
