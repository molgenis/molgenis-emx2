package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.http.Context;

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
            + "    <script src=\"https://unpkg.com/swagger-ui-dist@%s/swagger-ui-bundle.js\"></script>"
            + "    <script src=\"https://unpkg.com/swagger-ui-dist@%s/swagger-ui-standalone-preset.js\"> </script>\n"
            + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@%s/swagger-ui.css\">"
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

  static String getOpenApiUserInterface(Context context) {
    context.status(200);
    return createSwaggerUI(getSchema(context).getMetadata().getName());
  }
}
