package org.molgenis.emx2.web;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.molgenis.emx2.Row.MOLGENISID;
import static org.molgenis.emx2.web.Constants.ACCEPT_JSON;
import static org.molgenis.emx2.web.Constants.ACCEPT_ZIP;

public class OpenApiForSchemaFactory {

  static final Parameter molgenisid =
      new PathParameter().name(MOLGENISID).in("path").required(true).schema(new UUIDSchema());
  public static final String OBJECT = "object";
  public static final String PROBLEM = "Problem";

  private OpenApiForSchemaFactory() {
    // hide public constructor
  }

  public static OpenAPI createOpenApi(SchemaMetadata schema) throws MolgenisException {

    OpenAPI api = new OpenAPI();
    api.info(createOpenApiInfo(schema));

    Paths paths = new Paths();
    Components components = new Components();

    createOpenApiForSchema(schema, paths, components);

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

  private static void createOpenApiForSchema(
      SchemaMetadata schema, Paths paths, Components components) {

    String path = new StringBuilder().append("/data/").append(schema.getName()).toString();

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

    // meta/tableName retrieves table metadata

    // import

    // export

    // post new table

    // post attribute

    paths.addPathItem(path, schemaPath);
  }

  private static Operation schemaGet() {
    return new Operation()
        .summary("Get complete schema metadata")
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .content(
                            new Content()
                                .addMediaType(
                                    ACCEPT_JSON,
                                    new MediaType().schema(new Schema().$ref("SchemaMetadata")))
                                .addMediaType(
                                    ACCEPT_ZIP,
                                    new MediaType().schema(new StringSchema().format("binary"))))));
  }

  private static Operation schemaZipUpload() {
    return new Operation()
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
                                            "file", new FileSchema().description("upload file"))))))
        .responses(
            new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("Success"))
                .addApiResponse("400", new ApiResponse().description("Bad request").$ref(PROBLEM))
                .addApiResponse("500", new ApiResponse().description("Server error")));
  }

  // users have roles
  // roles have permissions
  // thus we need listing of role-permissions and user-roles
  // thus get schema.roles/permissions and schema.users would provide listing of those things

  /*
  *
  resource centric:
  api/schema.permissions
  api/schema/table.permissions
  api/schema/table/molgenisid.permissions
  get: {role: permission, anotherRole: anotherPermission}
  post, delete {role:aPermission} allows to change those

  role centric, so I can manage roles. Doesn't include RLS
  GET api/schema.roles: {
  	aRole: {_schema: permission, aTable:permission, etc:permission}
  	otherRole: {etc}
  }
  POST
  GET api/schema.roles/aRole: {_schema: permission, aTable:permission, etc:permission}
  DELETE api/schema.roles/aRole: 200
  POST api/schema.roles/aRole: {_schema: permission, aTable:permission}} //creates role if not exists
  * */

  // post to grant new { role: roleid, permission: permission, object: schema/table }
  // revoke permission by delete of {idem}
  // there is no update of a permission
  // do we want all permissions for schema in one go? I think yes?

  // in addition I want to quickly check if current user has a permission for a table.

  private static void createOpenApiForTable(TableMetadata table, Paths paths, Components components)
      throws MolgenisException {
    String tableName = table.getTableName();

    // components
    rowSchemaComponent(table, components);
    apiResponseComponentFor(tableName, components);

    // operations
    PathItem tablePath = new PathItem();
    PathItem rowPath = new PathItem();

    tablePath.get(tableGet(tableName));
    tablePath.post(tablePostOperation(tableName));
    tablePath.put(tablePutOperation(tableName));
    rowPath.get(rowGetOperation(tableName));
    rowPath.delete(rowDelete(tableName));

    // add the paths to paths
    String path =
        new StringBuilder()
            .append("/data/")
            .append(table.getSchema().getName())
            .append("/")
            .append(tableName)
            .toString();
    paths.addPathItem(path, tablePath);
    paths.addPathItem(path + "/{molgenisid}", rowPath);
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
                    "200",
                    new ApiResponse()
                        .description("success")
                        .content(
                            new Content()
                                .addMediaType(ACCEPT_JSON, mediaType)
                                .addMediaType("text/csv", mediaType))));
  }

  private static void rowSchemaComponent(TableMetadata table, Components components)
      throws MolgenisException {
    Map<String, Schema> properties = new LinkedHashMap<>();
    for (Column column : table.getColumns()) {
      properties.put(column.getColumnName(), columnSchema(column));
    }
    components.addSchemas(table.getTableName(), new Schema().type(OBJECT).properties(properties));
    Map<String, Schema> insertProperties = new LinkedHashMap<>(properties);
    insertProperties.remove(MOLGENISID);
    components.addSchemas(
        "New" + table.getTableName(), new Schema().type(OBJECT).properties(insertProperties));
  }

  private static Operation rowDelete(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Delete one row from " + tableName)
        .addParametersItem(molgenisid)
        .responses(
            new ApiResponses().addApiResponse("200", new ApiResponse().description("success")));
  }

  private static Operation rowGetOperation(String tableName) {
    return new Operation()
        .summary("Retrieve one row from " + tableName + " using " + MOLGENISID)
        .addTagsItem(tableName)
        .addParametersItem(molgenisid)
        .responses(rowApiResponse(tableName));
  }

  private static Operation tablePutOperation(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Update row in " + tableName)
        .requestBody(tablePutRequestBody(tableName))
        .responses(rowApiResponse(tableName));
  }

  private static Operation tablePostOperation(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Insert row into " + tableName)
        .requestBody(tablePostRequestBody(tableName))
        .responses(rowApiResponse(tableName));
  }

  private static ApiResponses rowApiResponse(String tableName) {
    return new ApiResponses()
        .addApiResponse("200", new ApiResponse().$ref(tableName))
        .addApiResponse("400", new ApiResponse().description("Bad request"));
  }

  private static RequestBody tablePostRequestBody(String tableName) {
    return new RequestBody()
        .content(
            new Content()
                .addMediaType(
                    ACCEPT_JSON, new MediaType().schema(new Schema().$ref("New" + tableName))));
  }

  private static RequestBody tablePutRequestBody(String tableName) {
    return new RequestBody()
        .content(
            new Content()
                .addMediaType(ACCEPT_JSON, new MediaType().schema(new Schema().$ref(tableName))));
  }

  private static void apiResponseComponentFor(String tableName, Components components) {
    components.addResponses(
        tableName,
        new ApiResponse()
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

    components.addSchemas("SchemaMetadata", metadataSchema);
  }

  private static Schema columnSchema(Column column) throws MolgenisException {
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
        return columnSchema(column.getRefColumn());
      case REF_ARRAY:
      case MREF:
        return new ArraySchema().items(columnSchema(column.getRefColumn()));
      default:
        throw new MolgenisException(
            "internal_error",
            "Should never happen unless during development",
            "createColumnSchema failed: Type " + column.getType() + " not supported ");
    }
  }
}
