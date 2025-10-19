package org.molgenis.emx2.jsonld;

import static org.molgenis.emx2.Constants.MG_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.graphql.GraphqlApi;

public class RestOverGraphql {
  private static final ObjectMapper mapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  public static String getTableAsJson(
      GraphqlApi graphql, String tableId, Map<String, Object> variables) {
    // todo add ability to pass query filters, limit, offset via variables
    String query = String.format("{%s{...All%sFields}}", tableId, tableId);
    return graphql.queryAsString(query, variables);
  }

  public static String getTableAsTtl(GraphqlApi graphql, String tableId) {
    return getTableAsTtl(graphql, tableId, Map.of());
  }

  public static String getTableAsTtl(
      GraphqlApi graphql, String tableId, Map<String, Object> variables) {
    try {
      String query = String.format("{%s{...All%sFields}}", tableId, tableId);
      Map data = graphql.queryAsMap(query, variables);
      String jsonLdSchema =
          JsonLdSchemaGenerator.generateJsonLdSchema(
              graphql.getSchema().getMetadata(), "http://localhost");
      return convertToTurtle(mapper.readValue(jsonLdSchema, Map.class), data);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  // todo add
  // set based post, put, delete later
  // id based idem

  public static String convertToTurtle(
      Map<String, ?> jsonLdSchema, Map<String, Object> graphqlLikeData) throws IOException {
    Map wrapper = new LinkedHashMap<>();
    graphqlLikeData.put(MG_ID, "my:.");
    wrapper.putAll(jsonLdSchema);
    wrapper.put("data", graphqlLikeData);
    try (StringReader reader = new StringReader(mapper.writeValueAsString(wrapper))) {
      Model model = Rio.parse(reader, "", RDFFormat.JSONLD);
      StringWriter writer = new StringWriter();
      Rio.write(model, writer, RDFFormat.TURTLE);
      return writer.toString();
    }
  }
}
