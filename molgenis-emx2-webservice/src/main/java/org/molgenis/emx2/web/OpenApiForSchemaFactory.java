package org.molgenis.emx2.web;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

import java.util.*;

import static org.molgenis.emx2.web.Constants.*;

public class OpenApiForSchemaFactory {

  public static final String OK = "200";
  public static final String OBJECT = "object";
  public static final String PROBLEM = "Problem";
  public static final String BAD_REQUEST = "400";
  public static final String BAD_REQUEST_MESSAGE = "Bad request";
  public static final String DATA_PATH = "/data/"; // NOSONAR
  public static final String MEMBER = "Member";
  public static final String SCHEMA_METADATA = "SchemaMetadata";

  private OpenApiForSchemaFactory() {
    // hide public constructor
  }

  public static OpenAPI createOpenApi(SchemaMetadata schema) {

    // basic metadata
    OpenAPI api = new OpenAPI();
    api.info(createOpenApiInfo(schema));

    // createTableIfNotExists the paths and components
    Paths paths = new Paths();
    Components components = new Components();

    // auth
    components.addSecuritySchemes(
        "ApiKeyAuth",
        new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("x-molgenis-token"));
    List<SecurityRequirement> securityRequirementList = new ArrayList<>();
    securityRequirementList.add(new SecurityRequirement().addList("ApiKeyAuth"));
    api.security(securityRequirementList);

    // /data
    PathItem dataApi = new PathItem();
    dataApi.post(apiPostOperation());
    paths.addPathItem(DATA_PATH, dataApi);

    // /data/:schema
    createOpenApiForSchema(schema, paths, components);

    // /data/:schema/:table
    for (String tableNameUnencoded : schema.getTableNames()) {
      TableMetadata table = schema.getTableMetadata(tableNameUnencoded);
      createOpenApiForTable(table, paths, components);
    }

    // assembly
    api.setPaths(paths);
    api.setComponents(components);

    return api;
  }

  private static Info createOpenApiInfo(SchemaMetadata schema) {
    return new Info()
        .title("API for: " + schema.getName())
        .version("0.0.1")
        .description(
            "MOLGENIS API for schema stored in MOLGENIS under name '" + schema.getName() + "'");
  }

  private static Operation apiPostOperation() {
    return new Operation()
        .summary("Create a new schema")
        .requestBody(
            new RequestBody()
                .content(
                    new Content()
                        .addMediaType(
                            ACCEPT_JSON,
                            new MediaType()
                                .schema(
                                    new Schema()
                                        .type(OBJECT)
                                        .addProperties("name", new StringSchema())))))
        .responses(apiResponses());
  }

  private static void createOpenApiForSchema(
      SchemaMetadata schema, Paths paths, Components components) {

    String path =
        new StringBuilder().append(DATA_PATH).append("/").append(schema.getName()).toString();

    // components
    schemaMetadataSchema(components);

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
                        ACCEPT_JSON, new MediaType().schema(new Schema().$ref(PROBLEM)))));

    // operations
    PathItem schemaPath = new PathItem();

    // post multi-part-form for upload file
    schemaPath.post(schemaZipUpload());
    schemaPath.get(schemaGet());
    schemaPath.delete(schemaDelete());

    paths.addPathItem(path, schemaPath);

    // /data/:schema.roles
    components.addSchemas(
        MEMBER,
        new ObjectSchema()
            .addProperties("user", new StringSchema())
            .addProperties("role", new StringSchema()));

    PathItem membersPath = new PathItem();
    membersPath.get(membersGet());
    membersPath.post(membersPost());
    membersPath.delete(membersDelete());
    paths.addPathItem("/admin/" + schema.getName() + "/members", membersPath);

    // meta/tableName retrieves table metadata

    // import

    // export

    // post new table

    // post attribute

  }

  private static Operation membersGet() {
    return tableGet(MEMBER);
  }

  private static Operation membersDelete() {
    return tableDeleteOperation(MEMBER);
  }

  private static Operation membersPost() {
    return tablePostOperation(MEMBER);
  }

  private static Operation schemaDelete() {
    return new Operation().summary("Delete this schema").responses(apiResponses());
  }

  private static Operation schemaGet() {
    return new Operation()
        .summary("Get complete schema metadata (JSON, CSV) or even complete contents (as ZIP)")
        .responses(
            new ApiResponses()
                .addApiResponse(
                    OK,
                    new ApiResponse()
                        .content(
                            new Content()
                                .addMediaType(
                                    ACCEPT_JSON,
                                    new MediaType().schema(new Schema().$ref(SCHEMA_METADATA)))
                                .addMediaType(
                                    ACCEPT_ZIP,
                                    new MediaType().schema(new StringSchema().format("binary")))
                                .addMediaType(
                                    ACCEPT_CSV,
                                    new MediaType().schema(new Schema().$ref(SCHEMA_METADATA)))
                                .addMediaType(
                                    ACCEPT_EXCEL,
                                    new MediaType().schema(new Schema().$ref(SCHEMA_METADATA))))));
  }

  private static Operation schemaZipUpload() {
    return new Operation()
        .summary("Import zipfile")
        .requestBody(
            new RequestBody()
                .content(new Content().addMediaType("multipart/form-data", fileUploadMediaType())))
        .responses(
            new ApiResponses()
                .addApiResponse(OK, new ApiResponse().description("Success"))
                .addApiResponse(
                    BAD_REQUEST, new ApiResponse().description(BAD_REQUEST_MESSAGE).$ref(PROBLEM))
                .addApiResponse("500", new ApiResponse().description("Server error")));
  }

  private static MediaType fileUploadMediaType() {
    return new MediaType()
        .schema(
            new Schema()
                .type(OBJECT)
                .addProperties("file", new FileSchema().description("upload file")));
  }

  private static void createOpenApiForTable(
      TableMetadata table, Paths paths, Components components) {
    String tableName = table.getTableName();

    // components
    rowSchemaComponent(table, components);
    apiResponseComponentFor(tableName, components);

    // operations
    PathItem tablePath = new PathItem();

    // multiple row operations
    tablePath.get(tableGet(tableName));
    tablePath.post(tablePostOperation(tableName));
    tablePath.put(tablePutOperation(tableName));
    tablePath.delete(tableDeleteOperation(tableName));

    // add the paths to paths
    String path =
        new StringBuilder()
            .append(DATA_PATH)
            .append(table.getSchema().getName())
            .append("/")
            .append(tableName)
            .toString();
    paths.addPathItem(path, tablePath);
  }

  private static Operation tableGet(String tableName) {

    MediaType mediaType =
        new MediaType().schema(new ArraySchema().items(new Schema().$ref(tableName)));

    return new Operation()
        .addTagsItem(tableName)
        .summary("Retrieve multiple rows from " + tableName)
        .responses(
            new ApiResponses()
                .addApiResponse(
                    OK,
                    new ApiResponse()
                        .description("success")
                        .content(
                            new Content()
                                .addMediaType(ACCEPT_JSON, mediaType)
                                .addMediaType(ACCEPT_CSV, mediaType))));
  }

  private static void rowSchemaComponent(TableMetadata table, Components components) {
    Map<String, Schema> properties = new LinkedHashMap<>();
    for (Column column : table.getColumns()) {
      properties.put(column.getColumnName(), columnSchema(column));
    }
    components.addSchemas(
        table.getTableName(),
        new Schema()
            .description("JSON schema for " + table.getTableName())
            .type(OBJECT)
            .properties(properties));
  }

  private static Operation tablePutOperation(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Update an array of one or more " + tableName)
        .requestBody(tableRequestBody(tableName))
        .responses(apiResponses());
  }

  private static Operation tablePostOperation(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Add an array of one or more " + tableName)
        .requestBody(tableRequestBody(tableName))
        .responses(apiResponses());
  }

  private static Operation tableDeleteOperation(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Delete an array of one more " + tableName)
        .requestBody(tableRequestBody(tableName))
        .responses(apiResponses());
  }

  private static ApiResponses apiResponses() {
    return new ApiResponses()
        .addApiResponse(OK, new ApiResponse().description("success"))
        .addApiResponse(BAD_REQUEST, new ApiResponse().description(BAD_REQUEST_MESSAGE));
  }

  private static RequestBody tableRequestBody(String tableName) {
    return new RequestBody()
        .content(
            new Content()
                .addMediaType(
                    ACCEPT_JSON,
                    new MediaType().schema(new ArraySchema().items(new Schema().$ref(tableName))))
                .addMediaType(
                    ACCEPT_CSV, new MediaType().schema(new StringSchema().format(ACCEPT_CSV)))
                .addMediaType(
                    ACCEPT_FORMDATA,
                    new MediaType()
                        .schema(
                            new Schema()
                                .type(OBJECT)
                                .addProperties("file", new FileSchema().format(ACCEPT_CSV)))));
  }

  private static void apiResponseComponentFor(String tableName, Components components) {
    components.addResponses(
        tableName,
        new ApiResponse()
            .description("Response for table " + tableName)
            .content(
                new Content()
                    .addMediaType(
                        ACCEPT_JSON, new MediaType().schema(new Schema().$ref(tableName)))));
  }

  private static void schemaMetadataSchema(Components components) {

    // note: in future version we could use the PoJo metadata conversion of MOLGENIS. But now
    // hardcoded

    // column
    Schema columnMetadata = new Schema();
    columnMetadata.addProperties("name", new StringSchema());
    columnMetadata.addProperties("type", new StringSchema());
    components.addSchemas("ColumnAnnotation", columnMetadata);

    // table
    Schema tableMetadata = new Schema();
    tableMetadata.addProperties("name", new StringSchema());
    tableMetadata.addProperties(
        "columns", new ArraySchema().items(new Schema().$ref("ColumnAnnotation")));
    components.addSchemas("TableMetadata", tableMetadata);

    // schema
    Schema metadataSchema = new Schema();
    metadataSchema.addProperties(
        "tables", new ArraySchema().items(new Schema().$ref("TableMetadata")));

    components.addSchemas(SCHEMA_METADATA, metadataSchema);
  }

  private static Schema columnSchema(Column column) {
    switch (column.getColumnType()) {
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
        return columnSchema(column.getRefColumn());
      case REF_ARRAY:
      case MREF:
        return new ArraySchema().items(columnSchema(column.getRefColumn()));
      default:
        throw new MolgenisException(
            "internal_error",
            "Should never happen unless during development",
            "createColumnSchema failed: ColumnType " + column.getColumnType() + " not supported ");
    }
  }
}
