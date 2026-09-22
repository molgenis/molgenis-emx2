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
import io.javalin.http.Context;
import io.javalin.http.NotAcceptableResponse;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.TempFile;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfRootService;
import org.molgenis.emx2.rdf.RdfSchemaService;
import org.molgenis.emx2.rdf.RdfSchemaValidationService;
import org.molgenis.emx2.rdf.generators.RdfApiGenerator;
import org.molgenis.emx2.rdf.shacl.ShaclSelector;
import org.molgenis.emx2.rdf.shacl.ShaclSet;

public class RDFApi {
  private static final String TMP_FILENAME = "download.tmp";
  private static final String QUERY_STRING_SHACLS = "shacls";
  private static final String QUERY_STRING_VALIDATE = "validate";

  private static final Map<MediaType, RDFFormat> mediaTypeRdfFormatMap = new HashMap<>();
  private static final List<MediaType> acceptedMediaTypes = new ArrayList<>(); // order of priority
  public static final ApplicationCachePerUser APPLICATION_CACHE =
      ApplicationCachePerUser.getInstance();

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
    defineApiRoutes(app, prefix, API_RDF, null);
    defineApiRoutes(app, prefix, API_TTL, RDFFormat.TURTLE);
    defineApiRoutes(app, prefix, API_JSONLD, RDFFormat.JSONLD);
  }

  private static void defineApiRoutes(
      Javalin app, String prefix, String apiLocation, RDFFormat format) {
    app.get(prefix + apiLocation, ctx -> databaseGet(ctx, format));
    app.head(prefix + apiLocation, ctx -> databaseHead(ctx, format));
    app.get(prefix + "{schema}" + apiLocation, ctx -> schemaGet(ctx, format));
    app.head(prefix + "{schema}" + apiLocation, ctx -> setFormat(ctx, format));
    app.get(prefix + "{schema}" + apiLocation + "/{table}", ctx -> tableGet(ctx, format));
    app.head(prefix + "{schema}" + apiLocation + "/{table}", ctx -> setFormat(ctx, format));
    app.get(prefix + "{schema}" + apiLocation + "/{table}/{row}", ctx -> rowGet(ctx, format));
    app.head(prefix + "{schema}" + apiLocation + "/{table}/{row}", ctx -> setFormat(ctx, format));
    app.get(
        prefix + "{schema}" + apiLocation + "/{table}/column/{column}",
        ctx -> columnGet(ctx, format));
    app.head(
        prefix + "{schema}" + apiLocation + "/{table}/column/{column}",
        ctx -> setFormat(ctx, format));
  }

  private static void databaseHead(Context ctx, RDFFormat format) {
    if (ctx.queryParam(QUERY_STRING_SHACLS) != null) {
      ctx.contentType(ACCEPT_YAML);
    } else {
      setFormat(ctx, format);
    }
  }

  private static void databaseGet(Context ctx, RDFFormat format) throws IOException {
    if (ctx.queryParam(QUERY_STRING_SHACLS) != null) {
      shaclSetsYaml(ctx);
    } else {
      rdfForDatabase(ctx, format);
    }
  }

  private static void shaclSetsYaml(Context ctx) throws IOException {
    ctx.contentType(ACCEPT_YAML);

    // Only show available SHACLs if there are any schema's available to validate on.
    if (APPLICATION_CACHE.getDatabaseForUser(ctx).getSchemaNames().isEmpty()) {
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

  private static void rdfForDatabase(Context ctx, RDFFormat format) throws IOException {
    format = setFormat(ctx, format);

    Database db = APPLICATION_CACHE.getDatabaseForUser(ctx);
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
    try (TempFile tmp = new TempFile(TMP_FILENAME)) {
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp.get().toFile()))) {
        try (RdfRootService service = new RdfRootService(baseUrl, format, out)) {
          db.tx(
              database -> {
                for (int i = 0; i < schemas.length; i++) {
                  schemas[i] = (db.getSchema(schemaNamesArr[i]));
                }
                service.getGenerator().generate(List.of(schemas));
              });
        }
      }
      tmp.streamTo(ctx.outputStream());
    }
  }

  private static void schemaGet(Context ctx, RDFFormat format) throws IOException {
    String shaclId = ctx.queryParam(QUERY_STRING_VALIDATE);
    Consumer<RdfApiGenerator> consumer = generator -> generator.generate(getSchema(ctx));
    if (shaclId != null) {
      ShaclSet shaclSet = retrieveShaclSet(ctx, shaclId);
      runValidationService(ctx, format, shaclSet, consumer);
    } else {
      runRdfService(ctx, format, consumer);
    }
  }

  private static void tableGet(Context ctx, RDFFormat format) throws IOException {
    runRdfService(ctx, format, generator -> generator.generate(getTableByIdOrName(ctx)));
  }

  private static void rowGet(Context ctx, RDFFormat format) throws IOException {
    Table table = getTableByIdOrName(ctx);
    PrimaryKey primaryKey = PrimaryKey.fromEncodedString(table, sanitize(ctx.pathParam("row")));
    runRdfService(ctx, format, generator -> generator.generate(table, primaryKey));
  }

  private static void columnGet(Context ctx, RDFFormat format) throws IOException {
    Table table = getTableByIdOrName(ctx);
    Column column = table.getMetadata().getColumn(sanitize(ctx.pathParam("column")));
    runRdfService(ctx, format, generator -> generator.generate(table, column));
  }

  private static void runRdfService(
      Context ctx, RDFFormat format, Consumer<RdfApiGenerator> consumer) throws IOException {
    format = setFormat(ctx, format);
    String baseUrl = extractBaseURL(ctx);

    try (TempFile tmp = new TempFile(TMP_FILENAME)) {
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp.get().toFile()))) {
        try (RdfSchemaService service =
            new RdfSchemaService(baseUrl, getSchema(ctx), format, out)) {
          consumer.accept(service.getGenerator());
        }
      }
      tmp.streamTo(ctx.outputStream());
    }
  }

  private static void runValidationService(
      Context ctx, RDFFormat format, ShaclSet shaclSet, Consumer<RdfApiGenerator> consumer)
      throws IOException {
    format = setFormat(ctx, format);
    String baseUrl = extractBaseURL(ctx);

    try (TempFile tmp = new TempFile(TMP_FILENAME)) {
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp.get().toFile()))) {
        try (RdfSchemaValidationService service =
            new RdfSchemaValidationService(baseUrl, getSchema(ctx), format, out, shaclSet)) {
          consumer.accept(service.getGenerator());
        }
      }
      tmp.streamTo(ctx.outputStream());
    }
  }

  private static ShaclSet retrieveShaclSet(Context ctx, String id) {
    ShaclSet shaclSet = ShaclSelector.get(id);
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
    MediaType mediaType = getContentType(ctx, acceptedMediaTypes);
    if (mediaType == null) {
      throw new NotAcceptableResponse(
          "Only the following accept-header values are supported: "
              + acceptedMediaTypes.stream().map(MediaType::toString).toList());
    }
    return mediaTypeRdfFormatMap.get(mediaType);
  }
}
