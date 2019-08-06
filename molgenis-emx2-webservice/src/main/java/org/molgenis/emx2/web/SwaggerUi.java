package org.molgenis.emx2.web;

public class SwaggerUi {

  public static String createSwaggerUI(String schemaName) {
    String version =
        "3.19.5"; // newer versions give errors on my Mac: i can not type in RequestBody field
    return String.format(
        "<html>"
            + "<head>"
            + "    <script src=\"http://unpkg.com/swagger-ui-dist@%s/swagger-ui-bundle.js\"></script>"
            + "    <script src=\"http://unpkg.com/swagger-ui-dist@%s/swagger-ui-standalone-preset.js\"> </script>\n"
            + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://unpkg.com/swagger-ui-dist@%s/swagger-ui.css\">"
            + "</head>"
            + "<body>"
            + "<div>Using Swagger UI %s</div>"
            + "<div id=\"swagger\"/>"
            + "<script>"
            + "window.onload = function() {\n"
            + "      // Begin Swagger UI call region\n"
            + "      const ui = SwaggerUIBundle({\n"
            + "        \"dom_id\": \"#swagger\",\n"
            + "        url: \"%s/openapi.yaml\",\n"
            + "        deepLinking: true,\n"
            + "        presets: [\n"
            + "          SwaggerUIBundle.presets.apis,\n"
            + "          SwaggerUIStandalonePreset\n"
            + "        ],\n"
            + "        plugins: [\n"
            + "          SwaggerUIBundle.plugins.DownloadUrl\n"
            + "        ],"
            + "        layout: \"StandaloneLayout\"\n"
            + "      })\n"
            + "      \n"
            + "      \n"
            + "      // End Swagger UI call region\n"
            + "  window.ui = ui\n   "
            + "}\n"
            + "</script>"
            + "</body></html>",
        version, version, version, version, schemaName);
  }
}
