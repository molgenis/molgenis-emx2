package org.molgenis.emx2.fairmapper.schemas;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.graphql.GraphqlClient;
import org.molgenis.emx2.json.JsonUtil;

public class GraphqlSchemaFetcher implements SchemaFetcher {

  private final GraphqlClient client;

  public GraphqlSchemaFetcher(GraphqlClient client) {
    this.client = client;
  }

  @Override
  public Optional<SchemaMetadata> fetch(String schemaName) {
    String query = readFile("schema-query.graphql");
    JsonNode result = client.sendSchemaQuery(schemaName, query);
    if (!result.has("_schema")) {
      throw new MolgenisException("No schema returned in graphql response: " + result);
    }

    try {
      SchemaMetadata schema = JsonUtil.jsonToSchema(result.get("_schema").toString());
      return Optional.of(schema);
    } catch (IOException e) {
      throw new MolgenisException("Unable to map query result to SchemaMetaData", e);
    }
  }

  private String readFile(String fileName) {
    try (InputStream inputStream = GraphqlSchemaFetcher.class.getResourceAsStream(fileName)) {
      return new String(Objects.requireNonNull(inputStream).readAllBytes());
    } catch (IOException e) {
      throw new MolgenisException("Unable to read query from file: " + fileName, e);
    }
  }
}
