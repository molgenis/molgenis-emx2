package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.get;

import java.io.*;
import java.util.Collection;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.RDFService;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class RDFApi {
  public static final String FORMAT = "format";
  private static MolgenisSessionManager sessionManager;
  public static final String RDF_API_LOCATION = "/api/rdf";

  public static void create(MolgenisSessionManager sm) {
    // ideally, we estimate/calculate the content length and inform the client using
    // response.raw().setContentLengthLong(x) but since the output is streaming and the triples
    // created on-the-fly, there is no way of knowing (or is there?)
    sessionManager = sm;
    get(RDF_API_LOCATION, RDFApi::rdfForDatabase);
    final String schemaPath = "/:schema" + RDF_API_LOCATION;
    get(schemaPath, RDFApi::rdfForSchema);
    // FIXME: rdfForTable also handles requests for a specific row if there is a composite key
    // TODO: probably best to merge these two methods and always use query string to encode the row
    get(schemaPath + "/:table", RDFApi::rdfForTable);
    get(schemaPath + "/:table/:row", RDFApi::rdfForRow);
    get(schemaPath + "/:table/column/:column", RDFApi::rdfForColumn);
  }

  private static int rdfForDatabase(Request request, Response response) throws IOException {
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(request);
    String[] schemaNamesArr = schemaNames.toArray(new String[schemaNames.size()]);
    Schema[] schemas = new Schema[schemaNames.size()];

    Database db = sessionManager.getSession(request).getDatabase();
    final String baseURL = extractBaseURL(request);
    final RDFService rdf = new RDFService(request.url(), baseURL, request.queryParams(FORMAT));
    response.type(rdf.getMimeType());
    OutputStream outputStream = response.raw().getOutputStream();
    db.tx(
        database -> {
          for (int i = 0; i < schemas.length; i++) {
            schemas[i] = (db.getSchema(schemaNamesArr[i]));
          }
          rdf.describeAsRDF(outputStream, null, null, null, schemas);
        });

    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForSchema(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    if (schema == null) {
      throw new MolgenisException("Schema " + request.params("schema") + " was not found");
    }
    final String baseURL = extractBaseURL(request);
    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, request.queryParams(FORMAT));
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, null, null, null, schema);
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForTable(Request request, Response response) throws IOException {
    Table table = getTable(request);
    String rowId = null;
    if (request.queryString() != null && !request.queryString().isBlank()) {
      rowId = request.queryString();
    }
    final String baseURL = extractBaseURL(request);
    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, request.queryParams(FORMAT));
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForRow(Request request, Response response) throws IOException {
    Table table = getTable(request);
    String rowId = sanitize(request.params("row"));

    final String baseURL = extractBaseURL(request);
    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, request.queryParams(FORMAT));
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForColumn(Request request, Response response) throws IOException {
    Table table = getTable(request);
    String columnName = sanitize(request.params("column"));

    final String baseURL = extractBaseURL(request);
    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, request.queryParams(FORMAT));
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, table, null, columnName, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static String extractBaseURL(Request request) {
    // NOTE: The request.host() already includes the server port!
    return request.scheme()
        + "://"
        + request.host()
        + (StringUtils.isNotEmpty(request.servletPath()) ? "/" + request.servletPath() + "/" : "/");
  }
}
