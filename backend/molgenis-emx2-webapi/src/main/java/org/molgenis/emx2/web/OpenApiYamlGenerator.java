package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.*;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

public class OpenApiYamlGenerator {

  static final String OK = "200";
  private static final String OBJECT = "object";
  private static final String PROBLEM = "Problem";
  static final String BAD_REQUEST = "400";
  static final String BAD_REQUEST_MESSAGE = "Bad request";

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
    for (int i = 0; i < MOLGENIS_TOKEN.length; i++) {
      String authTokenKey = MOLGENIS_TOKEN[i];
      components.addSecuritySchemes("ApiKeyAuth" + i, securityScheme(authTokenKey));
    }
    components.addSchemas(PROBLEM, problemSchema());
    components.addResponses(PROBLEM, problemResponse());

    // api/zip/:schema
    String zipType = "Zip";
    String zipPrefix = "/" + schema.getName() + "/api/zip";
    PathItem zipPath = getSchemaOperations(zipType, ACCEPT_ZIP);
    paths.addPathItem(zipPrefix, zipPath);

    // api/excel/:schema
    String excelType = "Excel";
    String excelPrefix = "/" + schema.getName() + "/api/excel";
    PathItem excelPath = getSchemaOperations(excelType, ACCEPT_EXCEL);
    paths.addPathItem(excelPrefix, excelPath);

    // table paths
    for (TableMetadata table : schema.getTables()) {
      paths.addPathItem(
          excelPrefix + "/" + table.getTableName(), getExcelTableOperations(excelType));
      paths.addPathItem(zipPrefix + "/" + table.getTableName(), getExcelTableOperations(zipType));
    }

    // api/csv/:schema

    // assembly
    api.setPaths(paths);
    api.setComponents(components);
    return api;
  }

  @NotNull
  private static PathItem getSchemaOperations(String tag, String mimeType) {
    PathItem excelPath = new PathItem();
    excelPath.post(postFileOperation(tag));
    excelPath.get(getFileOperation(tag, mimeType));
    return excelPath;
  }

  @NotNull
  private static PathItem getExcelTableOperations(String type) {
    PathItem tablePath = new PathItem();
    String mimeType = ACCEPT_EXCEL;

    tablePath.get(
        new Operation()
            .addTagsItem(type)
            .summary("Get table rows in " + type + " format")
            .responses(getResponses(mimeType)));

    return tablePath;
  }

  private static SecurityScheme securityScheme(String authTokenKey) {
    return new SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .in(SecurityScheme.In.HEADER)
        .name(authTokenKey);
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

  private static Operation getFileOperation(String type, String mimeType) {
    return new Operation()
        .summary("Get complete schema metadata as " + type)
        .addTagsItem(type)
        .addParametersItem(new Parameter().name("emx1").in("query").schema(new BooleanSchema()))
        .responses(getResponses(mimeType));
  }

  private static ApiResponses getResponses(String mimeType) {
    return new ApiResponses()
        .addApiResponse(
            OK,
            new ApiResponse()
                .content(
                    new Content()
                        .addMediaType(
                            mimeType,
                            new MediaType().schema(new StringSchema().format("binary")))));
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

  public static ApiResponses apiResponses() {
    Content message = getMessageContent();

    return new ApiResponses()
        .addApiResponse(OK, new ApiResponse().description("Success").content(message))
        .addApiResponse(BAD_REQUEST, new ApiResponse().description("Failed").content(message));
  }

  static Content getMessageContent() {
    return new Content()
        .addMediaType(
            ACCEPT_JSON,
            new MediaType()
                .schema(
                    new Schema()
                        .addProperties("title", new StringSchema())
                        .addProperties("message", new StringSchema())));
  }

  private static RequestBody tableRequestBody(String tableName) {
    return new RequestBody()
        .content(
            new Content()
                .addMediaType(
                    ACCEPT_JSON,
                    new MediaType().schema(new ArraySchema().items(new Schema().$ref(tableName)))));
  }
}
