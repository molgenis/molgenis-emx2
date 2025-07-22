package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.API_JSONLD;
import static org.molgenis.emx2.Constants.API_RDF;
import static org.molgenis.emx2.Constants.API_TTL;
import static org.molgenis.emx2.utils.URLUtils.extractBaseURL;
import static org.molgenis.emx2.web.MolgenisWebservice.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfRootService;
import org.molgenis.emx2.rdf.RdfSchemaService;
import org.molgenis.emx2.rdf.RdfSchemaValidationService;
import org.molgenis.emx2.rdf.RdfService;
import org.molgenis.emx2.rdf.generators.RdfApiGenerator;
import org.molgenis.emx2.rdf.shacl.ShaclSelector;
import org.molgenis.emx2.rdf.shacl.ShaclSet;

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
    app.get("{schema}" + apiLocation, (ctx) -> schemaPath(ctx, format));
    app.head("{schema}" + apiLocation, (ctx) -> rdfHead(ctx, format));
    app.get("{schema}" + apiLocation + "/{table}", (ctx) -> rdfForTable(ctx, format));
    app.head("{schema}" + apiLocation + "/{table}", (ctx) -> rdfHead(ctx, format));
    app.get("{schema}" + apiLocation + "/{table}/{row}", (ctx) -> rdfForRow(ctx, format));
    app.head("{schema}" + apiLocation + "/{table}/{row}", (ctx) -> rdfHead(ctx, format));
    app.get(
        "{schema}" + apiLocation + "/{table}/column/{column}", (ctx) -> rdfForColumn(ctx, format));
    app.head("{schema}" + apiLocation + "/{table}/column/{column}", (ctx) -> rdfHead(ctx, format));
  }

  private static void rdfHead(Context ctx, RDFFormat format) {
    setFormat(ctx, format);
  }

  private static void rdfForDatabase(Context ctx, RDFFormat format) throws IOException {
    format = setFormat(ctx, format);

    Database db = sessionManager.getSession(ctx.req()).getDatabase();
    Collection<String> availableSchemas = getSchemaNames(ctx);
    Collection<String> schemaNames = new ArrayList<>();
    if (ctx.queryParam("schemas") != null) {
      List<String> selectedSchemas = Arrays.stream(ctx.queryParam("schemas").split(",")).toList();
      for (String name : selectedSchemas) {
        if (!availableSchemas.contains(name) || db.getSchema(name) == null) {
          throw new MolgenisException("Schema '" + name + "' unknown or permission denied");
        }
        schemaNames.add(name);
      }
    } else {
      schemaNames = availableSchemas;
    }

    String[] schemaNamesArr = schemaNames.toArray(new String[schemaNames.size()]);
    Schema[] schemas = new Schema[schemaNames.size()];

    String baseUrl = extractBaseURL(ctx);
    try (OutputStream outputStream = ctx.outputStream()) {
      try (RdfRootService rdf = new RdfRootService(baseUrl, format, outputStream)) {
        db.tx(
            database -> {
              for (int i = 0; i < schemas.length; i++) {
                schemas[i] = (db.getSchema(schemaNamesArr[i]));
              }
              rdf.getGenerator().generate(List.of(schemas));
            });
      }
      outputStream.flush();
    }
  }

  private static void schemaPath(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    if (ctx.queryParam("validate") != null) {
      shaclForSchema(ctx, format);
    } else {
      rdfForSchema(ctx, format);
    }
  }

  private static void rdfForSchema(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method = RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class);
    Schema schema = getSchema(ctx);
    runRdfService(ctx, schema, format, method, schema);
  }

  private static void shaclForSchema(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method = RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class);
    Schema schema = getSchema(ctx);
    ShaclSet shaclSet = retrieveShaclSet(ctx, sanitize(ctx.queryParam("validate")));
    runRdfValidationService(ctx, schema, format, shaclSet, method, schema);
  }

  private static void rdfForTable(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method = RdfApiGenerator.class.getDeclaredMethod("generate", Table.class);
    Table table = getTableByIdOrName(ctx);
    runRdfService(ctx, table.getSchema(), format, method, table);
  }

  private static void rdfForRow(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method =
        RdfApiGenerator.class.getDeclaredMethod("generate", Table.class, PrimaryKey.class);
    Table table = getTableByIdOrName(ctx);
    PrimaryKey primaryKey = PrimaryKey.fromEncodedString(table, sanitize(ctx.pathParam("row")));
    runRdfService(ctx, table.getSchema(), format, method, table, primaryKey);
  }

  private static void rdfForColumn(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method = RdfApiGenerator.class.getDeclaredMethod("generate", Table.class, Column.class);
    Table table = getTableByIdOrName(ctx);
    Column column = table.getMetadata().getColumn(sanitize(ctx.pathParam("column")));
    runRdfService(ctx, table.getSchema(), format, method, table, column);
  }

  private static void runRdfService(
      final Context ctx,
      final Schema schema,
      RDFFormat format,
      final Method method,
      final Object... methodArgs)
      throws IOException {
    format = setFormat(ctx, format);
    String baseUrl = extractBaseURL(ctx);

    Class serviceClass = RdfSchemaService.class;
    Class[] serviceArgClasses =
        new Class[] {String.class, Schema.class, RDFFormat.class, OutputStream.class};

    try (OutputStream out = ctx.outputStream()) {
      Object[] serviceArgs = new Object[] {baseUrl, schema, format, out};
      runService(ctx, format, serviceClass, serviceArgClasses, serviceArgs, method, methodArgs);
    }
  }

  private static void runRdfValidationService(
      final Context ctx,
      final Schema schema,
      RDFFormat format,
      final ShaclSet shaclSet,
      final Method method,
      final Object... methodArgs)
      throws IOException {
    format = setFormat(ctx, format);
    String baseUrl = extractBaseURL(ctx);

    Class serviceClass = RdfSchemaValidationService.class;
    Class[] serviceArgClasses =
        new Class[] {
          String.class, Schema.class, RDFFormat.class, OutputStream.class, ShaclSet.class
        };

    try (OutputStream out = ctx.outputStream()) {
      Object[] serviceArgs = new Object[] {baseUrl, schema, format, out, shaclSet};
      runService(ctx, format, serviceClass, serviceArgClasses, serviceArgs, method, methodArgs);
    }
  }

  private static void runService(
      final Context ctx,
      final RDFFormat format,
      final Class<? extends RdfService> serviceClass,
      final Class[] serviceArgClasses,
      final Object[] serviceArgs,
      final Method method,
      final Object... methodArgs) {
    ctx.contentType(format.getDefaultMIMEType());

    try (RdfService<?> rdfService =
        serviceClass.getConstructor(serviceArgClasses).newInstance(serviceArgs)) {
      method.invoke(rdfService.getGenerator(), methodArgs);
    } catch (InvocationTargetException
        | IllegalAccessException
        | InstantiationException
        | NoSuchMethodException e) {
      // Any exceptions thrown should purely be due to bugs in this specific code.
      throw new RuntimeException(
          "An error occurred while trying to run the RDF API: " + e.getCause());
    }
  }

  private static ShaclSet retrieveShaclSet(Context ctx, String name) {
    ShaclSet shaclSet = ShaclSelector.get(name);
    if (shaclSet == null) {
      ctx.status(404);
      throw new MolgenisException("Validation set could not be found.");
    }
    return shaclSet;
  }

  private static RDFFormat setFormat(Context ctx, RDFFormat format) {
    if (format == null) format = selectFormat(ctx);
    ctx.contentType(format.getDefaultMIMEType());
    return format;
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
