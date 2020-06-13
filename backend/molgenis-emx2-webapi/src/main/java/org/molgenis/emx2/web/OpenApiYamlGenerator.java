package org.molgenis.emx2.web;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.molgenis.emx2.SchemaMetadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.web.Constants.*;

public class OpenApiYamlGenerator {

  static final String OK = "200";
  private static final String OBJECT = "object";
  private static final String PROBLEM = "Problem";
  static final String BAD_REQUEST = "400";
  static final String BAD_REQUEST_MESSAGE = "Bad request";
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
    // components.addSchemas(SCHEMA, schemaSchema());

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

    // api/csv/:schema

    // assembly
    api.setPaths(paths);
    api.setComponents(components);
    return api;
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

  private static Schema schemaSchema() {

    // column
    Schema columnMetadata = new Schema();
    columnMetadata.addProperties("name", new StringSchema());
    columnMetadata.addProperties("type", new StringSchema());

    // table
    Schema tableMetadata = new Schema();
    tableMetadata.addProperties("name", new StringSchema());
    tableMetadata.addProperties("columns", new ArraySchema().items(columnMetadata));

    // schema
    Schema metadataSchema = new Schema();
    metadataSchema.addProperties("tables", new ArraySchema().items(tableMetadata));

    return metadataSchema;
  }
}
