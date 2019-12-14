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

public class OpenApiYamlGenerator {

  private static final String OK = "200";
  private static final String OBJECT = "object";
  private static final String PROBLEM = "Problem";
  private static final String BAD_REQUEST = "400";
  private static final String BAD_REQUEST_MESSAGE = "Bad request";
  private static final String MEMBER = "Member";
  private static final String SCHEMA = "SchemaMetadata";

  private OpenApiYamlGenerator() {
    // hide public constructor
  }

  public static OpenAPI createOpenApi(SchemaMetadata schema) {

    Paths paths = new Paths();
    Components components = new Components();

    // basic metadata
    OpenAPI api = new OpenAPI();
    api.info(info(schema));
    api.security(securityRequirementList());

    // components
    components.addSecuritySchemes("ApiKeyAuth", securityScheme());
    components.addSchemas(PROBLEM, problemSchema());
    components.addResponses(PROBLEM, problemResponse());
    components.addSchemas(MEMBER, memberSchema());
    components.addSchemas(SCHEMA, schemaSchema());

    // api/json
    PathItem dataApi = new PathItem();
    dataApi.post(apiPostOperation());
    paths.addPathItem("/api/json", dataApi);

    // api/zip/:schema
    PathItem zipPath = new PathItem();
    zipPath.post(postFileOperation("Zip"));
    zipPath.get(getFileOperation("Zip", ACCEPT_ZIP));
    paths.addPathItem("/api/zip/" + schema.getName(), zipPath);

    // api/excel/:schema
    PathItem excelPath = new PathItem();
    excelPath.post(postFileOperation("Excel"));
    excelPath.get(getFileOperation("Excel", ACCEPT_EXCEL));
    paths.addPathItem("/api/excel/" + schema.getName(), excelPath);

    // api/json/:schema
    PathItem schemaPath = new PathItem();
    schemaPath.get(schemaGetOperation());
    schemaPath.delete(schemaDeleteOperation());
    paths.addPathItem("/api/json/" + schema.getName(), schemaPath);

    // api/members/:schema
    PathItem membersPath = new PathItem();
    membersPath.get(tableGetOperation(MEMBER));
    membersPath.post(tablePostOperation(MEMBER));
    membersPath.delete(tableDeleteOperation(MEMBER));
    paths.addPathItem("/api/members/" + schema.getName(), membersPath);

    // api/json/:schema/:table
    for (String tableNameUnencoded : schema.getTableNames()) {
      TableMetadata table = schema.getTableMetadata(tableNameUnencoded);
      String tableName = table.getTableName();

      // table components
      components.addSchemas(table.getTableName(), tableSchema(table));
      components.addResponses(tableName, tableResponse(tableName));

      // table operations
      PathItem tablePath = new PathItem();
      tablePath.get(tableGetOperation(tableName));
      tablePath.post(tablePostOperation(tableName));
      // tablePath.put(tablePutOperation(tableName));
      tablePath.delete(tableDeleteOperation(tableName));

      // add the paths to paths
      paths.addPathItem("/api/json/" + table.getSchema().getName() + "/" + tableName, tablePath);
    }

    // assembly
    api.setPaths(paths);
    api.setComponents(components);
    return api;
  }

  private static Schema tableSchema(TableMetadata table) {
    Map<String, Schema> properties = new LinkedHashMap<>();
    for (Column column : table.getColumns()) {
      properties.put(column.getName(), columnSchema(column));
    }
    return new Schema()
        .description("JSON schema for " + table.getTableName())
        .type(OBJECT)
        .properties(properties);
  }

  private static ApiResponse tableResponse(String tableName) {
    return new ApiResponse()
        .description("JSON response for table " + tableName)
        .content(
            new Content()
                .addMediaType(ACCEPT_JSON, new MediaType().schema(new Schema().$ref(tableName))));
  }

  private static SecurityScheme securityScheme() {
    return new SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .in(SecurityScheme.In.HEADER)
        .name("x-molgenis-token");
  }

  private static List<SecurityRequirement> securityRequirementList() {
    List<SecurityRequirement> securityRequirementList = new ArrayList<>();
    securityRequirementList.add(new SecurityRequirement().addList("ApiKeyAuth"));
    return securityRequirementList;
  }

  private static Info info(SchemaMetadata schema) {
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

  private static Schema memberSchema() {
    return new ObjectSchema()
        .addProperties("user", new StringSchema())
        .addProperties("role", new StringSchema());
  }

  private static Schema problemSchema() {
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
    return new Schema().type(OBJECT).properties(problemProperties);
  }

  private static ApiResponse problemResponse() {
    return new ApiResponse()
        .content(
            new Content()
                .addMediaType(ACCEPT_JSON, new MediaType().schema(new Schema().$ref(PROBLEM))));
  }

  private static Operation schemaDeleteOperation() {
    return new Operation().summary("Delete this schema").responses(apiResponses());
  }

  private static Operation getFileOperation(String type, String mimeType) {
    return new Operation()
        .summary("Get complete schema metadata as " + type)
        .addTagsItem(type)
        .responses(
            new ApiResponses()
                .addApiResponse(
                    OK,
                    new ApiResponse()
                        .content(
                            new Content()
                                .addMediaType(
                                    mimeType,
                                    new MediaType().schema(new StringSchema().format("binary"))))));
  }

  private static Operation schemaGetOperation() {
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
                                    ACCEPT_JSON, new MediaType().schema(new Schema().$ref(SCHEMA)))
                                .addMediaType(
                                    ACCEPT_ZIP,
                                    new MediaType().schema(new StringSchema().format("binary")))
                                .addMediaType(
                                    ACCEPT_CSV, new MediaType().schema(new Schema().$ref(SCHEMA)))
                                .addMediaType(
                                    ACCEPT_EXCEL,
                                    new MediaType().schema(new Schema().$ref(SCHEMA))))));
  }

  private static Operation postFileOperation(String type) {
    return new Operation()
        .summary("Import " + type + " file")
        .addTagsItem(type)
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

  private static Operation tableGetOperation(String tableName) {
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
                        .content(new Content().addMediaType(ACCEPT_JSON, mediaType))));
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
                    new MediaType().schema(new ArraySchema().items(new Schema().$ref(tableName)))));
  }

  private static Schema schemaSchema() {

    // column
    Schema columnMetadata = new Schema();
    columnMetadata.addProperties("name", new StringSchema());
    columnMetadata.addProperties("type", new StringSchema());
    // components.addSchemas("ColumnAnnotation", columnMetadata);

    // table
    Schema tableMetadata = new Schema();
    tableMetadata.addProperties("name", new StringSchema());
    tableMetadata.addProperties("columns", new ArraySchema().items(columnMetadata));
    // components.addSchemas("TableMetadata", tableMetadata);

    // schema
    Schema metadataSchema = new Schema();
    metadataSchema.addProperties("tables", new ArraySchema().items(tableMetadata));

    return metadataSchema;
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
