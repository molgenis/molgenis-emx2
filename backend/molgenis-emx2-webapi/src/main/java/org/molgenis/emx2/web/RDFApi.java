package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.get;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.rdf4j.rio.RDFFormat;
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
    get("/api/jsonld", RDFApi::jslonldForDatabase);
    get("/api/ttl", RDFApi::ttlForDatabase);
    final String schemaPath = "/:schema" + RDF_API_LOCATION;
    get(schemaPath, RDFApi::rdfForSchema);
    // FIXME: rdfForTable also handles requests for a specific row if there is a composite key
    // TODO: probably best to merge these two methods and always use query string to encode the row
    get(schemaPath + "/:table", RDFApi::rdfForTable);
    get(schemaPath + "/:table/:row", RDFApi::rdfForRow);
    get(schemaPath + "/:table/column/:column", RDFApi::rdfForColumn);
    get("/:schema/api/jsonld", RDFApi::jsonldForSchema);
    get("/:schema/api/ttl", RDFApi::ttlForSchema);
    get("/:schema/api/jsonld/:table", RDFApi::jsonldForTable);
    get("/:schema/api/ttl/:table", RDFApi::ttlForTable);
  }

  private static int jslonldForDatabase(Request request, Response response) throws IOException {
    return rdfForDatabase(request, response, RDFFormat.JSONLD);
  }

  private static int ttlForDatabase(Request request, Response response) throws IOException {
    return rdfForDatabase(request, response, RDFFormat.TURTLE);
  }

  private static int rdfForDatabase(Request request, Response response) throws IOException {
    final RDFFormat format = selectFormat(request);
    return rdfForDatabase(request, response, format);
  }

  private static int rdfForDatabase(Request request, Response response, RDFFormat format)
      throws IOException {
    Collection<String> schemaNames = new ArrayList<>();
    if (request.queryParams("schemas") != null) {
      List<String> selectedSchemas =
          Arrays.stream(request.queryParams("schemas").split(",")).toList();
      for (String name : MolgenisWebservice.getSchemaNames(request)) {
        if (selectedSchemas.contains(name)) {
          schemaNames.add(name);
        }
      }
    } else {
      schemaNames = MolgenisWebservice.getSchemaNames(request);
    }
    String[] schemaNamesArr = schemaNames.toArray(new String[schemaNames.size()]);
    Schema[] schemas = new Schema[schemaNames.size()];

    Database db = sessionManager.getSession(request).getDatabase();
    final String baseURL = extractBaseURL(request);

    final RDFService rdf = new RDFService(request.url(), baseURL, format);
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

  private static int ttlForSchema(Request request, Response response) throws IOException {
    return rdfForSchema(request, response, RDFFormat.TURTLE);
  }

  private static int jsonldForSchema(Request request, Response response) throws IOException {
    return rdfForSchema(request, response, RDFFormat.JSONLD);
  }

  private static int rdfForSchema(Request request, Response response) throws IOException {
    final RDFFormat format = selectFormat(request);
    return rdfForSchema(request, response, format);
  }

  private static int rdfForSchema(Request request, Response response, RDFFormat format)
      throws IOException {
    Schema schema = getSchema(request);
    if (schema == null) {
      throw new MolgenisException("Schema " + request.params("schema") + " was not found");
    }
    final String baseURL = extractBaseURL(request);

    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, null, null, null, schema);
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int jsonldForTable(Request request, Response response) throws IOException {
    return rdfForTable(request, response, RDFFormat.JSONLD);
  }

  private static int ttlForTable(Request request, Response response) throws IOException {
    return rdfForTable(request, response, RDFFormat.TURTLE);
  }

  private static int rdfForTable(Request request, Response response) throws IOException {
    final RDFFormat format = selectFormat(request);
    return rdfForTable(request, response, format);
  }

  private static int rdfForTable(Request request, Response response, RDFFormat format)
      throws IOException {
    Table table = getTableById(request);
    String rowId = null;
    if (request.queryString() != null && !request.queryString().isBlank()) {
      rowId = request.queryString();
    }
    final String baseURL = extractBaseURL(request);

    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForRow(Request request, Response response) throws IOException {
    Table table = getTableById(request);
    String rowId = sanitize(request.params("row"));

    final String baseURL = extractBaseURL(request);
    final RDFFormat format = selectFormat(request);
    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForColumn(Request request, Response response) throws IOException {
    Table table = getTableById(request);
    String columnName = sanitize(request.params("column"));

    final String baseURL = extractBaseURL(request);
    final RDFFormat format = selectFormat(request);

    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, table, null, columnName, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static String extractBaseURL(Request request) {
    // NOTE: The request.host() already includes the server port!
    String scheme = request.scheme();
    String port = null;
    var parts = request.host().split(":", 2);
    String host = parts[0];
    if (parts.length == 2) {
      if (!isWellKnownPort(scheme, parts[1])) {
        port = parts[1];
      }
    }
    return scheme
        + "://"
        + host
        + (port != null ? ":" + port : "")
        + (StringUtils.isNotEmpty(request.servletPath()) ? "/" + request.servletPath() + "/" : "/");
  }

  private static boolean isWellKnownPort(String scheme, String port) {
    return (scheme.equals("http") && port.equals("80"))
        || (scheme.equals("https") && port.equals("443"));
  }

  public static RDFFormat selectFormat(Request request) {
    var accept = request.headers("Accept");
    // Accept header gives a list of comma separated mime types, optionally with a weight
    // Mime types can be exact or wildcard (e.g. text/* or */*).
    // To simplify our use case we ignore weight and wildcards
    for (var type : accept.split(",")) {
      if (type.contains(";")) {
        // Strip everything after a semicolon
        type = type.split(";")[0];
      }
      var formats =
          List.of(
              RDFFormat.TURTLE,
              RDFFormat.N3,
              RDFFormat.NTRIPLES,
              RDFFormat.NQUADS,
              RDFFormat.RDFXML,
              RDFFormat.TRIG,
              RDFFormat.JSONLD);
      for (var format : formats) {
        if (format.hasDefaultMIMEType(type)) {
          return format;
        }
      }
    }
    // Default to TURTLE
    return RDFFormat.TURTLE;
  }
}
