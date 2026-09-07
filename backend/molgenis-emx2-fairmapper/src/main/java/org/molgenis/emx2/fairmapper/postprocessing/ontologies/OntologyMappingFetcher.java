package org.molgenis.emx2.fairmapper.postprocessing.ontologies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;

class OntologyMappingFetcher {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String QUERY =
      """
      query {
          %s {
              name
              ontologyTermURI
          }
      }
      """;

  private final GraphqlClient client;

  OntologyMappingFetcher(GraphqlClient client) {
    this.client = client;
  }

  public Map<String, String> getMapping(String schemaName, String tableName) {
    JsonNode jsonNode = client.sendSchemaQuery(schemaName, QUERY.formatted(tableName));
    if (!jsonNode.has(tableName)) {
      throw new MolgenisException(
          "No data returned for table: " + tableName + " in schema: " + schemaName);
    }

    List<OntologyMapping> ontologyMappings =
        MAPPER.convertValue(jsonNode.get(tableName), new TypeReference<>() {});
    return ontologyMappings.stream()
        .filter(mapping -> mapping.ontologyTermURI() != null)
        .collect(Collectors.toMap(OntologyMapping::ontologyTermURI, OntologyMapping::name));
  }

  private record OntologyMapping(String ontologyTermURI, String name) {}
}
