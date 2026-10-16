package org.molgenis.emx2.fairmapper.postprocessing.ontologies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;
import org.molgenis.emx2.utils.TypeUtils;

class GraphqlOntologyMappingFetcher implements OntologyMappingFetcher {

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

  GraphqlOntologyMappingFetcher(GraphqlClient client) {
    this.client = client;
  }

  @Override
  public Map<String, String> getMapping(String schemaName, String tableName) {
    String tableNamePascalCase = TypeUtils.convertToPascalCase(tableName);
    JsonNode jsonNode = client.sendSchemaQuery(schemaName, QUERY.formatted(tableNamePascalCase));
    if (!jsonNode.has(tableNamePascalCase)) {
      throw new MolgenisException(
          "No data returned for table: " + tableName + " in schema: " + schemaName);
    }

    List<OntologyMapping> ontologyMappings =
        MAPPER.convertValue(jsonNode.get(tableNamePascalCase), new TypeReference<>() {});
    if (ontologyMappings == null) {
      throw new MolgenisException(
          "Unable to convert json node to a list of OntologyMappings: "
              + jsonNode.get(tableNamePascalCase).toString());
    }

    return ontologyMappings.stream()
        .filter(mapping -> mapping.ontologyTermURI() != null)
        .collect(Collectors.toMap(OntologyMapping::ontologyTermURI, OntologyMapping::name));
  }

  private record OntologyMapping(String ontologyTermURI, String name) {}
}
