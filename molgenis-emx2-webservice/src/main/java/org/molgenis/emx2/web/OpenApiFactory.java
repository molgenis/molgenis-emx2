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

  public static OpenAPI create(org.molgenis.Schema schema) throws MolgenisException {

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

      PathItem tablePath = new PathItem();
      PathItem pathWithMolgenisid = new PathItem();

      Map<String, Schema> propertiesWithId = new LinkedHashMap<>();
      for (Column column : table.getColumns()) {
        propertiesWithId.put(column.getName(), columnSchema(column));
      }
      Map<String, Schema> propertiesWithoutId = new LinkedHashMap<>(propertiesWithId);
      propertiesWithoutId.remove("molgenisid");

      // components
      components.addSchemas(tableName, new Schema().type("object").properties(propertiesWithoutId));
      components.addSchemas(
          tableName + "WithId", new Schema().type("object").properties(propertiesWithId));
      components.addResponses(
          tableName,
          new ApiResponse()
              .content(
                  new Content()
                      .addMediaType(
                          "application/json",
                          new MediaType().schema(new Schema().$ref(tableName + "withId")))));
      //      components.addRequestBodies(tableName, new RequestBody().setContent(new Content()
      //              .addMediaType("application/json", new MediaType().schema(new
      // Schema().$ref(tableName))));
      //      components.addParameters(
      //          tableName,
      //          new Parameter()
      //              .name("body")
      //              .in("body")
      //              .description("A row in " + tableName)
      //              .schema(new Schema().$ref(tableName)));

      // input/output
      Parameter molgenisid =
          new PathParameter()
              .name(MOLGENISID)
              .in("path")
              .required(true)
              .schema(new StringSchema().format("uuid"));
      RequestBody body =
          new RequestBody()
              .content(
                  new Content()
                      .addMediaType(
                          "application/json",
                          new MediaType().schema(new Schema().$ref(tableName))));
      ApiResponses responses =
          new ApiResponses()
              .addApiResponse("200", new ApiResponse().$ref(tableName))
              .addApiResponse("400", new ApiResponse().description("Bad request"));

      // operations
      pathWithMolgenisid.get(
          new Operation()
              .summary("retrieve")
              .addTagsItem(tableName)
              .addParametersItem(molgenisid)
              .responses(responses));
      tablePath.post(new Operation().addTagsItem(tableName).requestBody(body).responses(responses));
      pathWithMolgenisid.put(
          new Operation()
              .addTagsItem(tableName)
              .addParametersItem(molgenisid)
              .requestBody(body)
              .responses(responses));
      pathWithMolgenisid.delete(
          new Operation()
              .addTagsItem(tableName)
              .addParametersItem(molgenisid)
              .responses(
                  new ApiResponses()
                      .addApiResponse("200", new ApiResponse().description("success"))));

      // add the paths to paths
      String prefix = "/data/" + table.getSchemaName() + "/" + tableName;
      paths.addPathItem(prefix, tablePath);
      paths.addPathItem(prefix + "/{molgenisid}", pathWithMolgenisid);
    }

    // assembly
    api.setPaths(paths);
    api.setComponents(components);

    return api;
  }

  private static Schema columnSchema(Column column) throws RuntimeException, MolgenisException {
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
        return columnSchema(refColumn);
      case REF_ARRAY:
      case MREF:
        refTable = column.getTable().getSchema().getTable(column.getRefTable());
        refColumn = refTable.getColumn(column.getRefColumn());
        return new ArraySchema().items(columnSchema(refColumn));
      default:
        throw new MolgenisException(
            "columnSchema failed: Type " + column.getType() + " not supported ");
    }
  }
}
