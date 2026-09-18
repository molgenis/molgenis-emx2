package org.molgenis.emx2.fairmapper.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.SchemaMetadataProvider;
import org.molgenis.emx2.json.JsonUtil;

public class GraphqlSchemaMetadataProvider implements SchemaMetadataProvider {

  private final GraphqlClient client;

  public GraphqlSchemaMetadataProvider(GraphqlClient client) {
    this.client = client;
  }

  @Override
  public SchemaMetadata getSchemaMetadata(String schemaName) {
    String query = readFile("schema-query.graphql");
    JsonNode result = client.sendSchemaQuery(schemaName, query);
    if (!result.has("_schema")) {
      throw new MolgenisException("No schema returned in graphql response: " + result);
    }

    try {
      SchemaMetadata schema = JsonUtil.jsonToSchema(result.get("_schema").toString());
      schema.setSchemaMetadataProvider(this);
      schema.setName(schemaName);
      return schema;
    } catch (IOException e) {
      throw new MolgenisException("Unable to map query result to SchemaMetaData", e);
    }
  }

  private String readFile(String fileName) {
    try (InputStream inputStream =
        GraphqlSchemaMetadataProvider.class.getResourceAsStream(fileName)) {
      return new String(Objects.requireNonNull(inputStream).readAllBytes());
    } catch (IOException e) {
      throw new MolgenisException("Unable to read query from file: " + fileName, e);
    }
  }
}
