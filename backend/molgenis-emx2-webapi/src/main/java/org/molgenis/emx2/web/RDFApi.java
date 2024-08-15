package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.*;

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
import spark.Route;
import spark.utils.StringUtils;

public class RDFApi {
  public static final String FORMAT = "format";
  private static MolgenisSessionManager sessionManager;
  public static final String RDF_API_LOCATION = "/api/rdf";
  public static final String TTL_API_LOCATION = "/api/ttl";
  public static final String JSONLD_API_LOCATION = "/api/jsonld";

  private static final List<RDFFormat> acceptedRdfFormats =
      List.of(
          RDFFormat.TURTLE,
          RDFFormat.N3,
          RDFFormat.NTRIPLES,
          RDFFormat.NQUADS,
          RDFFormat.RDFXML,
          RDFFormat.TRIG,
          RDFFormat.JSONLD);

  public static void create(MolgenisSessionManager sm) {
    // ideally, we estimate/calculate the content length and inform the client using
    // response.raw().setContentLengthLong(x) but since the output is streaming and the triples
    // created on-the-fly, there is no way of knowing (or is there?)
    sessionManager = sm;

    defineApiRoutes(
        RDF_API_LOCATION,
        RDFApi::rdfHead,
        RDFApi::rdfForDatabase,
        RDFApi::rdfForSchema,
        RDFApi::rdfForTable,
        RDFApi::rdfForRow,
        RDFApi::rdfForColumn);
    defineApiRoutes(
        TTL_API_LOCATION,
        RDFApi::ttlHead,
        RDFApi::ttlForDatabase,
        RDFApi::ttlForSchema,
        RDFApi::ttlForTable,
        RDFApi::ttlForRow,
        RDFApi::ttlForColumn);
    defineApiRoutes(
        JSONLD_API_LOCATION,
        RDFApi::jsonldHead,
        RDFApi::jsonldForDatabase,
        RDFApi::jsonldForSchema,
        RDFApi::jsonldForTable,
        RDFApi::jsonldForRow,
        RDFApi::jsonldForColumn);
  }

  private static void defineApiRoutes(
      String apiLocation,
      Route headerRoute,
      Route databaseRoute,
      Route schemaRoute,
      Route tableRoute,
      Route rowRoute,
      Route columnRoute) {
    get(apiLocation, databaseRoute);
    head(apiLocation, headerRoute);
    path(
        "/:schema" + apiLocation,
        () -> {
          get("", schemaRoute);
          head("", headerRoute);
          path(
              "/:table",
              () -> {
                // FIXME: rdfForTable also handles requests for a specific row if there is a
                // composite key
                // TODO: probably best to merge these two methods and always use query string to
                // encode the row
                get("", tableRoute);
                head("", headerRoute);
                get("/:row", rowRoute);
                head("/:row", headerRoute);
                get("/column/:column", columnRoute);
                head("column/:column/", headerRoute);
              });
        });
  }

  private static String jsonldHead(Request request, Response response) {
    response.type(RDFFormat.JSONLD.getDefaultMIMEType());
    return "";
  }

  private static String ttlHead(Request request, Response response) {
    response.type(RDFFormat.TURTLE.getDefaultMIMEType());
    return "";
  }

  private static String rdfHead(Request request, Response response) {
    final RDFFormat format = selectFormat(request);
    response.type(format.getDefaultMIMEType());
    return "";
  }

  private static int jsonldForDatabase(Request request, Response response) throws IOException {
    return rdfForDatabase(request, response, RDFFormat.JSONLD);
  }

  private static int ttlForDatabase(Request request, Response response) throws IOException {
    return rdfForDatabase(request, response, RDFFormat.TURTLE);
  }

  private static int rdfForDatabase(Request request, Response response) throws IOException {
    final RDFFormat format = selectFormat(request);
    response.type(format.getDefaultMIMEType());
    return rdfForDatabase(request, response, format);
  }

  private static int rdfForDatabase(Request request, Response response, RDFFormat format)
      throws IOException {
    Database db = sessionManager.getSession(request).getDatabase();
    Collection<String> schemaNames = new ArrayList<>();
    if (request.queryParams("schemas") != null) {
      List<String> selectedSchemas =
          Arrays.stream(request.queryParams("schemas").split(",")).toList();
      for (String name : MolgenisWebservice.getSchemaNames(request)) {
        if (selectedSchemas.contains(name)) {
          if (db.getSchema(name) == null) {
            throw new MolgenisException("Schema '" + name + "' unknown or permission denied");
          }
          schemaNames.add(name);
        }
      }
    } else {
      schemaNames = MolgenisWebservice.getSchemaNames(request);
    }
    String[] schemaNamesArr = schemaNames.toArray(new String[schemaNames.size()]);
    Schema[] schemas = new Schema[schemaNames.size()];

    final String baseURL = extractBaseURL(request);

    final RDFService rdf = new RDFService(request.url().split("/api/")[0], baseURL, format);
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
    response.type(format.getDefaultMIMEType());
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
    response.type(format.getDefaultMIMEType());
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

  private static int jsonldForRow(Request request, Response response) throws IOException {
    return rdfForRow(request, response, RDFFormat.JSONLD);
  }

  private static int ttlForRow(Request request, Response response) throws IOException {
    return rdfForRow(request, response, RDFFormat.TURTLE);
  }

  private static int rdfForRow(Request request, Response response) throws IOException {
    final RDFFormat format = selectFormat(request);
    response.type(format.getDefaultMIMEType());
    return rdfForRow(request, response, format);
  }

  private static int rdfForRow(Request request, Response response, RDFFormat format)
      throws IOException {
    Table table = getTableById(request);
    String rowId = sanitize(request.params("row"));

    final String baseURL = extractBaseURL(request);
    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    response.type(rdf.getMimeType());

    OutputStream outputStream = response.raw().getOutputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int jsonldForColumn(Request request, Response response) throws IOException {
    return rdfForColumn(request, response, RDFFormat.JSONLD);
  }

  private static int ttlForColumn(Request request, Response response) throws IOException {
    return rdfForColumn(request, response, RDFFormat.TURTLE);
  }

  private static int rdfForColumn(Request request, Response response) throws IOException {
    final RDFFormat format = selectFormat(request);
    response.type(format.getDefaultMIMEType());
    return rdfForColumn(request, response, format);
  }

  private static int rdfForColumn(Request request, Response response, RDFFormat format)
      throws IOException {
    Table table = getTableById(request);
    String columnName = sanitize(request.params("column"));

    final String baseURL = extractBaseURL(request);

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
      for (var format : acceptedRdfFormats) {
        if (format.hasDefaultMIMEType(type)) {
          return format;
        }
      }
    }
    // Default to TURTLE
    return RDFFormat.TURTLE;
  }
}
