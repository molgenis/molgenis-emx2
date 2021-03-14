package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.OpenApiYamlGenerator.*;
import static spark.Spark.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.MultipartConfigElement;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import spark.Request;
import spark.Response;

public class CsvApi {

  public static final String MUTATION_REQUEST = "mutationRequestType";
  public static final String ERROR_MESSAGE = "errorMessageType";
  public static final String SUCCESS_MESSAGE = "successMessageType";
  public static final String CSV_OUTPUT = "csvOutputType";
  public static final String META = "_meta";

  private CsvApi() {
    // hide constructor
  }

  public static void create() {

    // schema level operations
    final String schemaPath = "/:schema/api/csv";
    get(schemaPath, CsvApi::getMetadata);
    post(schemaPath, CsvApi::mergeMetadata);
    delete(schemaPath, CsvApi::discardMetadata);

    // table level operations
    final String tablePath = "/:schema/api/csv/:table";
    get(tablePath, CsvApi::tableRetrieve);
    post(tablePath, CsvApi::tableUpdate);
    delete(tablePath, CsvApi::tableDelete);
  }

  private static String discardMetadata(Request request, Response response) throws IOException {
    SchemaMetadata schema = Emx2.loadEmx2File(getUploadedFile(request), getSeperator(request));
    getSchema(request).discard(schema);
    response.status(200);
    return "remove metadata items success";
  }

  static String mergeMetadata(Request request, Response response) throws IOException {
    SchemaMetadata schema = Emx2.loadEmx2File(getUploadedFile(request), getSeperator(request));
    getSchema(request).migrate(schema);
    response.status(200);
    return "add/update metadata success";
  }

  static String getMetadata(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    StringWriter writer = new StringWriter();
    CsvTableWriter.write(Emx2.toRowList(schema.getMetadata()), writer, getSeperator(request));
    response.type(ACCEPT_CSV);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    response.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".csv\"");
    response.status(200);
    return writer.toString();
  }

  private static String tableRetrieve(Request request, Response response) throws IOException {
    List<Row> rows = MolgenisWebservice.getTable(request).retrieveRows();
    StringWriter writer = new StringWriter();
    CsvTableWriter.write(rows, writer, getSeperator(request));
    response.type(ACCEPT_CSV);
    response.status(200);
    return writer.toString();
  }

  private static String tableInsert(Request request, Response response) throws IOException {
    // check if uploaded file
    File uploadFile = getUploadedFile(request);
    int count = MolgenisWebservice.getTable(request).insert(csvToRows(uploadFile, request));
    response.status(200);
    response.type(ACCEPT_CSV);
    return "" + count;
  }

  private static String tableUpdate(Request request, Response response) throws IOException {
    // check if uploaded file
    File uploadFile = getUploadedFile(request);
    int count = MolgenisWebservice.getTable(request).update(csvToRows(uploadFile, request));
    response.status(200);
    response.type(ACCEPT_CSV);
    return "" + count;
  }

  private static String tableDelete(Request request, Response response) throws IOException {
    File uploadFile = getUploadedFile(request);
    int count = MolgenisWebservice.getTable(request).delete(csvToRows(uploadFile, request));
    response.type(ACCEPT_CSV);
    response.status(200);
    return "" + count;
  }

  private static Iterable<Row> csvToRows(File file, Request request) throws IOException {
    return CsvTableReader.read(file, getSeperator(request));
  }

  private static Character getSeperator(Request request) {
    Character separator = ',';
    if ("TAB".equals(request.queryParams("separator"))) {
      separator = '\t';
    }
    return separator;
  }

  private static File getUploadedFile(Request request) {

    try {
      File tempFile = File.createTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
      tempFile.deleteOnExit();
      request.attribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(tempFile.getAbsolutePath()));
      if (request.raw().getPart("file") == null) {
        return null;
      }
      try (InputStream input = request.raw().getPart("file").getInputStream()) {
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
      } catch (Exception e) {
        throw new MolgenisException("File upload failed", e);
      }
    } catch (Exception e) {
      throw new MolgenisException("File upload failed", e);
    }
  }
}
