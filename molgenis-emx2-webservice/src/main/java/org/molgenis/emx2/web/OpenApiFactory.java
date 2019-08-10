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
  static final Parameter molgenisid =
      new PathParameter()
          .name(MOLGENISID)
          .in("path")
          .required(true)
          .schema(new StringSchema().format("uuid"));

  private OpenApiFactory() {
    // hide public constructor
  }

  public static OpenAPI createOpenApi(org.molgenis.Schema schema) throws MolgenisException {

    OpenAPI api = new OpenAPI();
    api.info(
        new Info()
            .title("API for: " + schema.getName())
            .version("0.0.1")
            .description(
                "MOLGENIS API for schema stored in MOLGENIS under name '"
                    + schema.getName()
                    + "'"));

    Paths paths = new Paths();
    Components components = new Components();

    for (String tableNameUnencoded : schema.getTableNames()) {
      Table table = schema.getTable(tableNameUnencoded);
      String tableName = tableNameUnencoded;

      // components
      components.addSchemas(tableName, rowSchemaComponentFor(table));
      components.addResponses(tableName, apiResponseComponentFor(tableName));

      // operations
      PathItem tablePath = new PathItem();
      PathItem tablePathWithMolgenisid = new PathItem();

      tablePath.post(postOperationFor(tableName));
      tablePath.put(putOperationFor(tableName));
      tablePathWithMolgenisid.get(getOperationFor(tableName));
      tablePathWithMolgenisid.delete(deleteOperationFor(tableName));

      // add the paths to paths
      String path = "/data/" + table.getSchemaName() + "/" + tableName;
      paths.addPathItem(path, tablePath);
      paths.addPathItem(path + "/{molgenisid}", tablePathWithMolgenisid);
    }

    // assembly
    api.setPaths(paths);
    api.setComponents(components);

    return api;
  }

  private static Schema rowSchemaComponentFor(Table table) throws MolgenisException {
    Map<String, Schema> properties = new LinkedHashMap<>();
    for (Column column : table.getColumns()) {
      properties.put(column.getName(), createColumnSchema(column));
    }
    return new Schema().type("object").properties(properties);
  }

  private static Operation deleteOperationFor(String tableName) {
    return new Operation()
        .addTagsItem(tableName)
        .summary("Delete one row from " + tableName)
        .addParametersItem(molgenisid)
        .responses(
            new ApiResponses().addApiResponse("200", new ApiResponse().description("success")));
  }

  private static Operation getOperationFor(String tableName) {
    ApiResponses responses = createApiResponse(tableName);

    return new Operation()
        .summary("Retrieve one row from " + tableName + " using " + MOLGENISID)
        .addTagsItem(tableName)
        .addParametersItem(molgenisid)
        .responses(responses);
  }

  private static Operation putOperationFor(String tableName) {
    RequestBody requestBody = createRequestBody(tableName);
    ApiResponses responses = createApiResponse(tableName);

    return new Operation()
        .addTagsItem(tableName)
        .summary("Update row in " + tableName)
        .requestBody(requestBody)
        .responses(responses);
  }

  public static Operation postOperationFor(String tableName) {
    RequestBody requestBody = createRequestBody(tableName);
    ApiResponses responses = createApiResponse(tableName);

    return new Operation()
        .addTagsItem(tableName)
        .summary("Insert row into " + tableName)
        .requestBody(requestBody)
        .responses(responses);
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
                    "application/json", new MediaType().schema(new Schema().$ref(tableName))));
  }

  public static ApiResponse apiResponseComponentFor(String tableName) {
    return new ApiResponse()
        .content(
            new Content()
                .addMediaType(
                    "application/json", new MediaType().schema(new Schema().$ref(tableName))));
  }

  private static Schema createColumnSchema(Column column) throws MolgenisException {
    switch (column.getType()) {
      case UUID:
        return new StringSchema().format("uuid");
      case UUID_ARRAY:
        return new ArraySchema().items(new StringSchema().format("uuid"));
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
        Table refTable = column.getTable().getSchema().getTable(column.getRefTable());
        Column refColumn = refTable.getColumn(column.getRefColumn());
        return createColumnSchema(refColumn);
      case REF_ARRAY:
      case MREF:
        refTable = column.getTable().getSchema().getTable(column.getRefTable());
        refColumn = refTable.getColumn(column.getRefColumn());
        return new ArraySchema().items(createColumnSchema(refColumn));
      default:
        throw new MolgenisException(
            "createColumnSchema failed: Type " + column.getType() + " not supported ");
    }
  }
}
