package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.API_JSONLD;
import static org.molgenis.emx2.Constants.API_RDF;
import static org.molgenis.emx2.Constants.API_TTL;
import static org.molgenis.emx2.utils.URLUtils.extractBaseURL;
import static org.molgenis.emx2.web.Constants.ACCEPT_YAML;
import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static org.molgenis.emx2.web.util.HttpHeaderUtils.getContentType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.net.MediaType;
import io.javalin.Javalin;
import io.javalin.http.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private static final Map<MediaType, RDFFormat> mediaTypeRdfFormatMap = new HashMap<>();
  private static final List<MediaType> acceptedMediaTypes = new ArrayList<>(); // order of priority

  static {
    // Defines order of priority!
    List<RDFFormat> acceptedRdfFormats =
        List.of(
            RDFFormat.TURTLE,
            RDFFormat.JSONLD,
            RDFFormat.RDFXML,
            RDFFormat.NTRIPLES,
            RDFFormat.NQUADS,
            RDFFormat.TRIG,
            RDFFormat.N3);

    for (RDFFormat format : acceptedRdfFormats) {
      MediaType mediaType = MediaType.parse(format.getDefaultMIMEType());
      mediaTypeRdfFormatMap.put(mediaType, format);
      acceptedMediaTypes.add(mediaType);
    }
  }

  public static void create(Javalin app) {
    // ideally, we estimate/calculate the content length and inform the client using
    // response.raw().setContentLengthLong(x) but since the output is streaming and the triples
    // created on-the-fly, there is no way of knowing (or is there?)
    defineApiRoutePerPrefix(app, "");
    defineApiRoutePerPrefix(app, "/apps/{app}/");
  }

  private static void defineApiRoutePerPrefix(Javalin app, String prefix) {
    defineApiRoutes(app, prefix, API_RDF, RDFApi::selectFormat);
    defineApiRoutes(app, prefix, API_TTL, (ctx) -> RDFFormat.TURTLE);
    defineApiRoutes(app, prefix, API_JSONLD, (ctx) -> RDFFormat.JSONLD);
  }

  private static void defineApiRoutes(
      Javalin app, String prefix, String apiLocation, Function<Context, RDFFormat> formatFunction) {
    defineApiCallMethods(
        app,
        prefix + apiLocation,
        (ctx) -> databaseGet(ctx, formatFunction.apply(ctx)),
        (ctx) -> databaseHead(ctx, formatFunction.apply(ctx)),
        RDFApi::defaultOptions);

    defineApiCallMethods(
        app,
        prefix + "{schema}" + apiLocation,
        (ctx) -> schemaGet(ctx, formatFunction.apply(ctx)),
        (ctx) -> headerRdfAndValidation(ctx, formatFunction.apply(ctx)),
        RDFApi::defaultOptions);

    defineApiCallMethods(
        app,
        prefix + "{schema}" + apiLocation + "/{table}",
        (ctx) -> tableGet(ctx, formatFunction.apply(ctx)),
        (ctx) -> headerRdfAndValidation(ctx, formatFunction.apply(ctx)),
        RDFApi::defaultOptions);

    defineApiCallMethods(
        app,
        prefix + "{schema}" + apiLocation + "/{table}/{row}",
        (ctx) -> rowGet(ctx, formatFunction.apply(ctx)),
        (ctx) -> headerRdfAndValidation(ctx, formatFunction.apply(ctx)),
        RDFApi::defaultOptions);

    defineApiCallMethods(
        app,
        prefix + "{schema}" + apiLocation + "/{table}/column/{column}",
        (ctx) -> columnGet(ctx, formatFunction.apply(ctx)),
        (ctx) -> headerRdfAndValidation(ctx, formatFunction.apply(ctx)),
        RDFApi::defaultOptions);
  }

  private static void defineApiCallMethods(
      Javalin app, String route, Handler getHandler, Handler headHandler, Handler optionsHandler) {
    app.get(route, getHandler);
    app.head(route, headHandler);
    app.options(route, optionsHandler);
  }

  private static void defaultOptions(Context ctx) {
    ctx.header("Allow", "GET, HEAD, OPTIONS");
  }

  private static void databaseHead(Context ctx, RDFFormat format) {
    if (ctx.queryParam("shacls") != null) {
      headerShaclSets(ctx);
    } else {
      headerRdfAndValidation(ctx, format);
    }
  }

  private static void databaseGet(Context ctx, RDFFormat format) throws IOException {
    if (ctx.queryParam("shacls") != null) {
      databaseShaclSetsGet(ctx);
    } else {
      databaseRdfGet(ctx, format);
    }
  }

  private static void databaseShaclSetsGet(Context ctx) throws IOException {
    headerShaclSets(ctx);

    // Only show available SHACLs if there are any schema's available to validate on.
    if (applicationCache.getDatabaseForUser(ctx).getSchemaNames().isEmpty()) {
      throw new MolgenisException("No permission to view any schema to use SHACLs on");
    }

    // Output is not identical to input. Nested arrays do not have extra indent:
    // .enable(YAMLGenerator.Feature.INDENT_ARRAYS) -> causes newline in root array items
    // .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR) -> all lines have extra indent
    ObjectMapper mapper =
        new ObjectMapper(
            YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build());

    try (OutputStream outputStream = ctx.outputStream()) {
      mapper.writeValue(outputStream, ShaclSelector.getAllFiltered());
    }
  }

  private static void databaseRdfGet(Context ctx, RDFFormat format) throws IOException {
    headerRdfAndValidation(ctx, format);

    Database db = applicationCache.getDatabaseForUser(ctx);
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

  private static void schemaGet(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method = RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class);
    Schema schema = getSchema(ctx);
    if (ctx.queryParam("validate") != null) {
      ShaclSet shaclSet = retrieveShaclSet(ctx, sanitize(ctx.queryParam("validate")));
      runRdfValidationService(ctx, schema, format, shaclSet, method, schema);
    } else {
      runRdfService(ctx, schema, format, method, schema);
    }
  }

  private static void tableGet(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method = RdfApiGenerator.class.getDeclaredMethod("generate", Table.class);
    Table table = getTableByIdOrName(ctx);
    runRdfService(ctx, table.getSchema(), format, method, table);
  }

  private static void rowGet(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method =
        RdfApiGenerator.class.getDeclaredMethod("generate", Table.class, PrimaryKey.class);
    Table table = getTableByIdOrName(ctx);
    PrimaryKey primaryKey = PrimaryKey.fromEncodedString(table, sanitize(ctx.pathParam("row")));
    runRdfService(ctx, table.getSchema(), format, method, table, primaryKey);
  }

  private static void columnGet(Context ctx, RDFFormat format)
      throws IOException, NoSuchMethodException {
    Method method = RdfApiGenerator.class.getDeclaredMethod("generate", Table.class, Column.class);
    Table table = getTableByIdOrName(ctx);
    Column column = table.getMetadata().getColumn(sanitize(ctx.pathParam("column")));
    runRdfService(ctx, table.getSchema(), format, method, table, column);
  }

  private static void runRdfService(
      final Context ctx,
      final Schema schema,
      final RDFFormat format,
      final Method method,
      final Object... methodArgs)
      throws IOException {
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
      final RDFFormat format,
      final ShaclSet shaclSet,
      final Method method,
      final Object... methodArgs)
      throws IOException {
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
    headerRdfAndValidation(ctx, format);

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
      throw new NotFoundResponse("Validation set could not be found.");
    }
    return shaclSet;
  }

  private static void headerRdfAndValidation(Context ctx, RDFFormat format) {
    ctx.header(
        "Accept",
        acceptedMediaTypes.stream().map(MediaType::toString).collect(Collectors.joining(", ")));
    ctx.contentType(format.getDefaultMIMEType());
  }

  private static void headerShaclSets(Context ctx) {
    ctx.header("Accept", ACCEPT_YAML);
    ctx.contentType(ACCEPT_YAML);
  }

  public static RDFFormat selectFormat(Context ctx) {
    MediaType mediaType = getContentType(ctx, acceptedMediaTypes);
    if (mediaType == null) {
      throw new NotAcceptableResponse(
          "Only the following accept-header values are supported: "
              + acceptedMediaTypes.stream().map(MediaType::toString).toList());
    }
    return mediaTypeRdfFormatMap.get(mediaType);
  }
}
