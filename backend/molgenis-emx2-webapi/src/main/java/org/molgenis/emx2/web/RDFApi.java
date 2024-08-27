package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
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

public class RDFApi {
  public static final String FORMAT = "format";
  private static MolgenisSessionManager sessionManager;
  public static final String RDF_API_LOCATION = "/api/rdf";

  public static void create(Javalin app, MolgenisSessionManager sm) {
    // ideally, we estimate/calculate the content length and inform the client using
    // response.raw().setContentLengthLong(x) but since the output is streaming and the triples
    // created on-the-fly, there is no way of knowing (or is there?)
    sessionManager = sm;
    app.get(RDF_API_LOCATION, RDFApi::rdfForDatabase);
    app.get("/api/jsonld", RDFApi::jslonldForDatabase);
    app.get("/api/ttl", RDFApi::ttlForDatabase);
    final String schemaPath = "/{schema}" + RDF_API_LOCATION;
    app.get(schemaPath, RDFApi::rdfForSchema);
    // FIXME: rdfForTable also handles requests for a specific row if there is a composite key
    // TODO: probably best to merge these two methods and always use query string to encode the row
    app.get(schemaPath + "/{table}", RDFApi::rdfForTable);
    app.get(schemaPath + "/{table}/{row}", RDFApi::rdfForRow);
    app.get(schemaPath + "/{table}/column/{column}", RDFApi::rdfForColumn);
    app.get("/{schema}/api/jsonld", RDFApi::jsonldForSchema);
    app.get("/{schema}/api/ttl", RDFApi::ttlForSchema);
    app.get("/{schema}/api/jsonld/{table}", RDFApi::jsonldForTable);
    app.get("/{schema}/api/ttl/{table}", RDFApi::ttlForTable);
  }

  private static int jslonldForDatabase(Context ctx) throws IOException {
    return rdfForDatabase(ctx, RDFFormat.JSONLD);
  }

  private static int ttlForDatabase(Context ctx) throws IOException {
    return rdfForDatabase(ctx, RDFFormat.TURTLE);
  }

  private static int rdfForDatabase(Context ctx) throws IOException {
    final RDFFormat format = selectFormat(ctx);
    return rdfForDatabase(ctx, format);
  }

  private static int rdfForDatabase(Context ctx, RDFFormat format) throws IOException {
    Database db = sessionManager.getSession(ctx.req()).getDatabase();
    Collection<String> schemaNames = new ArrayList<>();
    if (ctx.queryParam("schemas") != null) {
      List<String> selectedSchemas = Arrays.stream(ctx.queryParam("schemas").split(",")).toList();
      for (String name : MolgenisWebservice.getSchemaNames(ctx)) {
        if (selectedSchemas.contains(name)) {
          if (db.getSchema(name) == null) {
            throw new MolgenisException("Schema '" + name + "' unknown or permission denied");
          }
          schemaNames.add(name);
        }
      }
    } else {
      schemaNames = MolgenisWebservice.getSchemaNames(ctx);
    }
    String[] schemaNamesArr = schemaNames.toArray(new String[schemaNames.size()]);
    Schema[] schemas = new Schema[schemaNames.size()];

    final String baseURL = extractBaseURL(ctx);

    final RDFService rdf = new RDFService(ctx.url().split("/api/")[0], baseURL, format);
    ctx.contentType(rdf.getMimeType());
    OutputStream outputStream = ctx.outputStream();
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

  private static int ttlForSchema(Context ctx) throws IOException {
    return rdfForSchema(ctx, RDFFormat.TURTLE);
  }

  private static int jsonldForSchema(Context ctx) throws IOException {
    return rdfForSchema(ctx, RDFFormat.JSONLD);
  }

  private static int rdfForSchema(Context ctx) throws IOException {
    final RDFFormat format = selectFormat(ctx);
    return rdfForSchema(ctx, format);
  }

  private static int rdfForSchema(Context ctx, RDFFormat format) throws IOException {
    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema " + ctx.pathParam("schema") + " was not found");
    }
    final String baseURL = extractBaseURL(ctx);

    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    ctx.contentType(rdf.getMimeType());

    OutputStream outputStream = ctx.outputStream();
    rdf.describeAsRDF(outputStream, null, null, null, schema);
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int jsonldForTable(Context ctx) throws IOException {
    return rdfForTable(ctx, RDFFormat.JSONLD);
  }

  private static int ttlForTable(Context ctx) throws IOException {
    return rdfForTable(ctx, RDFFormat.TURTLE);
  }

  private static int rdfForTable(Context ctx) throws IOException {
    final RDFFormat format = selectFormat(ctx);
    return rdfForTable(ctx, format);
  }

  private static int rdfForTable(Context ctx, RDFFormat format) throws IOException {
    Table table = getTableById(ctx);
    String rowId = null;
    if (ctx.queryString() != null && !ctx.queryString().isBlank()) {
      rowId = ctx.queryString();
    }
    final String baseURL = extractBaseURL(ctx);

    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    ctx.contentType(rdf.getMimeType());

    OutputStream outputStream = ctx.outputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForRow(Context ctx) throws IOException {
    Table table = getTableById(ctx);
    String rowId = sanitize(ctx.pathParam("row"));

    final String baseURL = extractBaseURL(ctx);
    final RDFFormat format = selectFormat(ctx);
    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    ctx.contentType(rdf.getMimeType());

    OutputStream outputStream = ctx.outputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static int rdfForColumn(Context ctx) throws IOException {
    Table table = getTableById(ctx);
    String columnName = sanitize(ctx.pathParam("column"));

    final String baseURL = extractBaseURL(ctx);
    final RDFFormat format = selectFormat(ctx);

    RDFService rdf = new RDFService(baseURL, RDF_API_LOCATION, format);
    ctx.contentType(rdf.getMimeType());

    OutputStream outputStream = ctx.outputStream();
    rdf.describeAsRDF(outputStream, table, null, columnName, table.getSchema());
    outputStream.flush();
    outputStream.close();
    return 200;
  }

  private static String extractBaseURL(Context ctx) {
    // NOTE: The request.host() already includes the server port!
    String scheme = ctx.scheme();
    String port = null;
    var parts = ctx.host().split(":", 2);
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
        + (!ctx.path().isEmpty() ? "/" + ctx.path() + "/" : "/");
  }

  private static boolean isWellKnownPort(String scheme, String port) {
    return (scheme.equals("http") && port.equals("80"))
        || (scheme.equals("https") && port.equals("443"));
  }

  public static RDFFormat selectFormat(Context ctx) {
    var accept = ctx.header("Accept");
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
