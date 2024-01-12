package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import spark.Request;
import spark.Response;

public class OpenApiUiFactory {

  private OpenApiUiFactory() {
    // hide constructor
  }

  public static String createSwaggerUI(String schemaName) {
    String version =
        "3.23.9"; // newer versions give errors on my Mac: i can not type in RequestBody field
    return String.format(
        "<html>"
            + "<head>"
            + "    <script src=\"http://unpkg.com/swagger-ui-dist@%s/swagger-ui-bundle.js\"></script>"
            + "    <script src=\"http://unpkg.com/swagger-ui-dist@%s/swagger-ui-standalone-preset.js\"> </script>\n"
            + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://unpkg.com/swagger-ui-dist@%s/swagger-ui.css\">"
            + "</head>"
            + "<body>"
            + "<div id=\"swagger\"/>"
            + "<script>"
            + "window.onload = function() {\n"
            + "      // Begin Swagger UI call region\n"
            + "      const ui = SwaggerUIBundle({\n"
            + "        \"dom_id\": \"#swagger\",\n"
            + "        url: \"openapi.yaml\",\n"
            + "        deepLinking: true,\n"
            + "        presets: [\n"
            + "          SwaggerUIBundle.presets.apis,\n"
            + "          SwaggerUIStandalonePreset\n"
            + "        ],\n"
            + "        plugins: [\n"
            + "          SwaggerUIBundle.plugins.DownloadUrl\n"
            + "        ]"
            + "      })\n"
            + "      \n"
            + "      \n"
            + "      // End Swagger UI call region\n"
            + "  window.ui = ui\n   "
            + "}\n"
            + "</script>"
            + "</body></html>",
        version, version, version, schemaName);
  }

  static String getOpenApiUserInterface(Request request, Response response) {
    response.status(200);
    return createSwaggerUI(getSchema(request).getMetadata().getName());
  }
}
