package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.API_JSONLD;
import static org.molgenis.emx2.Constants.API_RDF;
import static org.molgenis.emx2.Constants.API_TTL;
import static org.molgenis.emx2.web.MolgenisWebservice.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.OutputStream;
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
import org.molgenis.emx2.utils.URLUtils;

public class RDFApi {
  public static final String FORMAT = "format";
  private static MolgenisSessionManager sessionManager;

  private static final List<RDFFormat> acceptedRdfFormats =
      List.of(
          RDFFormat.TURTLE,
          RDFFormat.N3,
          RDFFormat.NTRIPLES,
          RDFFormat.NQUADS,
          RDFFormat.RDFXML,
          RDFFormat.TRIG,
          RDFFormat.JSONLD);

  public static void create(Javalin app, MolgenisSessionManager sm) {
    // ideally, we estimate/calculate the content length and inform the client using
    // response.raw().setContentLengthLong(x) but since the output is streaming and the triples
    // created on-the-fly, there is no way of knowing (or is there?)
    sessionManager = sm;

    defineApiRoutes(app, API_RDF, null);
    defineApiRoutes(app, API_TTL, RDFFormat.TURTLE);
    defineApiRoutes(app, API_JSONLD, RDFFormat.JSONLD);
  }

  private static void defineApiRoutes(Javalin app, String apiLocation, RDFFormat format) {
    app.get(apiLocation, (ctx) -> rdfForDatabase(ctx, format));
    app.head(apiLocation, (ctx) -> rdfHead(ctx, format));
    app.get("{schema}" + apiLocation, (ctx) -> rdfForSchema(ctx, format));
    app.head("{schema}" + apiLocation, (ctx) -> rdfHead(ctx, format));
    app.get("{schema}" + apiLocation + "/{table}", (ctx) -> rdfForTable(ctx, format));
    app.head("{schema}" + apiLocation + "/{table}", (ctx) -> rdfHead(ctx, format));
    app.get(
        "{schema}" + apiLocation + "/{table}/column/{column}", (ctx) -> rdfForColumn(ctx, format));
    app.head("{schema}" + apiLocation + "/{table}/column/{column}", (ctx) -> rdfHead(ctx, format));
  }

  private static void rdfHead(Context ctx, RDFFormat format) {
    ctx.contentType(selectFormat(ctx, format).getDefaultMIMEType());
  }

  private static void rdfForDatabase(Context ctx, RDFFormat format) throws IOException {
    format = selectFormat(ctx, format); // defines format if null

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

    final String baseURL = URLUtils.extractBaseURL(ctx);

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
  }

  private static void rdfForSchema(Context ctx, RDFFormat format) throws IOException {
    format = selectFormat(ctx, format); // defines format if null

    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema " + ctx.pathParam("schema") + " was not found");
    }
    final String baseURL = URLUtils.extractBaseURL(ctx);

    RDFService rdf = new RDFService(baseURL, API_RDF, format);
    ctx.contentType(rdf.getMimeType());

    OutputStream outputStream = ctx.outputStream();
    rdf.describeAsRDF(outputStream, null, null, null, schema);
    outputStream.flush();
    outputStream.close();
  }

  private static void rdfForTable(Context ctx, RDFFormat format) throws IOException {
    format = selectFormat(ctx, format); // defines format if null

    Table table = getTableByIdOrName(ctx);
    String rowId = null;
    if (ctx.queryString() != null && !ctx.queryString().isBlank()) {
      rowId = ctx.queryString();
    }
    final String baseURL = URLUtils.extractBaseURL(ctx);

    RDFService rdf = new RDFService(baseURL, API_RDF, format);
    ctx.contentType(rdf.getMimeType());

    OutputStream outputStream = ctx.outputStream();
    rdf.describeAsRDF(outputStream, table, rowId, null, table.getSchema());
    outputStream.flush();
    outputStream.close();
  }

  private static void rdfForColumn(Context ctx, RDFFormat format) throws IOException {
    format = selectFormat(ctx, format); // defines format if null

    Table table = getTableByIdOrName(ctx);
    String columnName = sanitize(ctx.pathParam("column"));

    final String baseURL = URLUtils.extractBaseURL(ctx);

    RDFService rdf = new RDFService(baseURL, API_RDF, format);
    ctx.contentType(rdf.getMimeType());

    OutputStream outputStream = ctx.outputStream();
    rdf.describeAsRDF(outputStream, table, null, columnName, table.getSchema());
    outputStream.flush();
    outputStream.close();
  }

  private static RDFFormat selectFormat(Context ctx, RDFFormat format) {
    return format == null ? selectFormat(ctx) : format;
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
