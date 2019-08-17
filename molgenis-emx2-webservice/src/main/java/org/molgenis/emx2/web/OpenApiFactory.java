package org.molgenis.emx2.web;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.molgenis.Column;
import org.molgenis.MolgenisException;
import org.molgenis.Table;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.molgenis.Row.MOLGENISID;

public class OpenApiFactory {

  public static final String APPLICATION_JSON = "application/json";
  static final Parameter molgenisid =
      new PathParameter().name(MOLGENISID).in("path").required(true).schema(new UUIDSchema());
  public static final String OBJECT = "object";
  public static final String PROBLEM = "Problem";

  private OpenApiFactory() {
    // hide public constructor
  }

  public static OpenAPI createOpenApi(org.molgenis.Schema schema) throws MolgenisException {

    OpenAPI api = new OpenAPI();
    api.info(createInfo(schema));

    Paths paths = new Paths();
    Components components = new Components();

    createApiForSchema(schema, paths, components);

    for (String tableNameUnencoded : schema.getTableNames()) {
      Table table = schema.getTable(tableNameUnencoded);
      createApiForTable(table, paths, components);
    }

    // assembly
    api.setPaths(paths);
    api.setComponents(components);

    return api;
  }

  private static void createApiForSchema(
      org.molgenis.Schema schema, Paths paths, Components components) {
    String path = new StringBuilder().append("/data/").append(schema.getName()).toString();

    // components

    Map<String, Schema> problemProperties = new LinkedHashMap<>();
    problemProperties.put(
        "type",
        new StringSchema()
            .format("url")
            .description("a URL to a document describing the error condition "));
    problemProperties.put(
        "title",
        new StringSchema().description("A short, human-readable title for the general error type"));
    problemProperties.put(
        "detail",
        new StringSchema().description("A human-readable description of the specific error"));
    components.addSchemas(PROBLEM, new Schema().type(OBJECT).properties(problemProperties));
    components.addResponses(
        PROBLEM,
        new ApiResponse()
            .content(
                new Content()
                    .addMediaType(
                        APPLICATION_JSON, new MediaType().schema(new Schema().$ref(PROBLEM)))));

    // operations
    PathItem schemaPath = new PathItem();

    // post multi-part-form for upload file
    schemaPath.post(
        new Operation()
            .summary("Import zipfile")
            .requestBody(
                new RequestBody()
                    .content(
                        new Content()
                            .addMediaType(
                                "multipart/form-data",
                                new MediaType()
                                    .schema(
                                        new Schema()
                                            .type(OBJECT)
                                            .addProperties(
                                                "file",
                                                new FileSchema().description("upload file"))))))
            .responses(
                new ApiResponses()
                    .addApiResponse("200", new ApiResponse().description("Success"))
                    .addApiResponse(
                        "400", new ApiResponse().description("Bad request").$ref(PROBLEM))
                    .addApiResponse("500", new ApiResponse().description("Server error"))));

    // meta/tableName retrieves table metadata

    // import

    // export

    // post new table

    // post attribute

    paths.addPathItem(path, schemaPath);
  }

  public static void createApiForTable(Table table, Paths paths, Components components)
      throws MolgenisException {
    String tableName = table.getName();

    // components
    addRowSchemeComponent(table, components);
    apiResponseComponentFor(tableName, components);

    // operations
    PathItem tablePath = new PathItem();
    PathItem tablePathWithMolgenisid = new PathItem();

    tablePath.get(getTableQuery(tableName));
    tablePath.post(post(tableName));
    tablePath.put(put(tableName));
    tablePathWithMolgenisid.get(get(tableName));
    tablePathWithMolgenisid.delete(delete(tableName));

    // add the paths to paths
    String path =
        new StringBuilder()
            .append("/data/")
            .append(table.getSchemaName())
            .append("/")
            .append(tableName)
            .toString();
    paths.addPathItem(path, tablePath);
    paths.addPathItem(path + "/{molgenisid}", tablePathWithMolgenisid);
  }

  private static Operation getTableQuery(String tableName) {

    MediaType mediaType =
        new MediaType().schema(new ArraySchema().items(new Schema().$ref(tableName)));

    return new Operation()
        .addTagsItem(tableName)
        .summary("Retrieve multiple rows from " + tableName)
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("success")
                        .content(
                            new Content()
                                .addMediaType(APPLICATION_JSON, mediaType)
                                .addMediaType("text/csv", mediaType))));
  }

  public static Info createInfo(org.molgenis.Schema schema) {
    return new Info()
        .title("API for: " + schema.getName())
        .version("0.0.1")
        .description(
            "MOLGENIS API for schema stored in MOLGENIS under name '" + schema.getName() + "'");
  }

  private static void addRowSchemeComponent(Table table, Components components)
      throws MolgenisException {
    Map<String, Schema> properties = new LinkedHashMap<>();
    for (Column column : table.getColumns()) {
      properties.put(column.getName(), createColumnSchema(column));
    }
    components.addSchemas(table.getName(), new Schema().type(OBJECT).properties(properties));
  }

  private static Operation delete(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Delete one row from " + tableName)
        .addParametersItem(molgenisid)
        .responses(
            new ApiResponses().addApiResponse("200", new ApiResponse().description("success")));
  }

  private static Operation get(String tableName) {
    return new Operation()
        .summary("Retrieve one row from " + tableName + " using " + MOLGENISID)
        .addTagsItem(tableName)
        .addParametersItem(molgenisid)
        .responses(createApiResponse(tableName));
  }

  private static Operation put(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Update row in " + tableName)
        .requestBody(createRequestBody(tableName))
        .responses(createApiResponse(tableName));
  }

  public static Operation post(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Insert row into " + tableName)
        .requestBody(createRequestBody(tableName))
        .responses(createApiResponse(tableName));
  }

  public static ApiResponses createApiResponse(String tableName) {
    return new ApiResponses()
        .addApiResponse("200", new ApiResponse().$ref(tableName))
        .addApiResponse("400", new ApiResponse().description("Bad request"));
  }

  public static RequestBody createRequestBody(String tableName) {
    return new RequestBody()
        .content(
            new Content()
                .addMediaType(
                    APPLICATION_JSON, new MediaType().schema(new Schema().$ref(tableName))));
  }

  public static void apiResponseComponentFor(String tableName, Components components) {
    components.addResponses(
        tableName,
        new ApiResponse()
            .content(
                new Content()
                    .addMediaType(
                        APPLICATION_JSON, new MediaType().schema(new Schema().$ref(tableName)))));
  }

  private static Schema createColumnSchema(Column column) throws MolgenisException {
    switch (column.getType()) {
      case UUID:
        return new UUIDSchema();
      case UUID_ARRAY:
        return new ArraySchema().items(new UUIDSchema());
      case STRING:
        return new StringSchema();
      case STRING_ARRAY:
        return new ArraySchema().items(new StringSchema());
      case BOOL:
        return new BooleanSchema();
      case BOOL_ARRAY:
        return new ArraySchema().items(new BooleanSchema());
      case INT:
        return new IntegerSchema();
      case INT_ARRAY:
        return new ArraySchema().items(new IntegerSchema());
      case DECIMAL:
        return new NumberSchema().format("double");
      case DECIMAL_ARRAY:
        return new ArraySchema().items(new NumberSchema().format("double"));
      case TEXT:
        return new StringSchema();
      case TEXT_ARRAY:
        return new ArraySchema().items(new StringSchema());
      case DATE:
        return new StringSchema().format("date");
      case DATE_ARRAY:
        return new ArraySchema().items(new StringSchema().format("date"));
      case DATETIME:
        return new StringSchema().format("datetime");
      case DATETIME_ARRAY:
        return new ArraySchema().items(new StringSchema().format("datetime"));
      case REF:
        return createColumnSchema(column.getRefColumn());
      case REF_ARRAY:
      case MREF:
        return new ArraySchema().items(createColumnSchema(column.getRefColumn()));
      default:
        throw new MolgenisException(
            "createColumnSchema failed: Type " + column.getType() + " not supported ");
    }
  }
}
