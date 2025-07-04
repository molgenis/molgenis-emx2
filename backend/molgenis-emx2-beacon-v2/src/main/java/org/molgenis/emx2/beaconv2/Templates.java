package org.molgenis.emx2.beaconv2;

import graphql.ExecutionInput;
import graphql.GraphQL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.graphql.GraphqlSession;

public class Templates {

  public static void addTemplatesToDb(Database database) {
    //why is this done as admin?
    database.becomeAdmin();
    Schema schema = database.getSchema("_SYSTEM_");
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(schema,new GraphqlSession(database));

    for (EntryType entryType : EntryType.values()) {
      String query =
          "mutation insert($endpoint:String, $schema:String, $template:String) {"
              + " insert (Templates: { endpoint: $endpoint, schema: $schema, template: $template }) { message } }";
      Map<String, Object> variables = new HashMap<>();
      variables.put("endpoint", "beacon_" + entryType.getName());
      variables.put("schema", "default");
      String jsltPath = "entry-types/" + entryType.getName() + ".jslt";
      try {
        String jslt = readJsltFile(jsltPath);
        variables.put("template", jslt);
        graphQL.execute(ExecutionInput.newExecutionInput(query).variables(variables));
      } catch (IOException e) {
        System.out.println("Could not read jslt file " + jsltPath);
      }
    }
  }

  public static String readJsltFile(String jsltPath) throws IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (InputStream in = classLoader.getResourceAsStream(jsltPath)) {
      if (in == null) {
        throw new IOException("File not found on classpath: " + jsltPath);
      }
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          content.append(line).append(System.lineSeparator());
        }
        return content.toString();
      }
    }
  }
}
