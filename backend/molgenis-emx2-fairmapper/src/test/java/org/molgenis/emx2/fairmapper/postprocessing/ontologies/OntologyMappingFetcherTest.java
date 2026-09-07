package org.molgenis.emx2.fairmapper.postprocessing.ontologies;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;

class OntologyMappingFetcherTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void shouldResolveOntologies() {
    GraphqlClient staticClient =
        getStaticClient(
            """
            {
              "Countries": [
                {
                  "name": "Worldwide"
                },
                {
                  "name": "Afghanistan",
                  "ontologyTermURI": "http://publications.europa.eu/resource/authority/country/AFG"
                }
              ]
            }
            """);
    OntologyMappingFetcher mapper = new OntologyMappingFetcher(staticClient);
    Map<String, String> mapping = mapper.getMapping("schema", "Countries");
    assertEquals(
        Map.of("http://publications.europa.eu/resource/authority/country/AFG", "Afghanistan"),
        mapping);
  }

  @Test
  void shouldThrowWhenResponseIsMissingRequestedTable() {
    GraphqlClient staticClient =
        getStaticClient(
            """
            {
              "SomeOtherTable": []
            }
            """);
    OntologyMappingFetcher mapper = new OntologyMappingFetcher(staticClient);

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> mapper.getMapping("schema", "Countries"));
    assertEquals("No data returned for table: Countries in schema: schema", exception.getMessage());
  }

  GraphqlClient getStaticClient(String expectedResponse) {
    GraphqlClient graphqlClient = spy(new GraphqlClient("http://example.org", "some-token"));
    try {
      doReturn(MAPPER.readTree(expectedResponse))
          .when(graphqlClient)
          .sendSchemaQuery(anyString(), anyString());
    } catch (JsonProcessingException e) {
      fail("Unable to set up graphql client");
    }

    return graphqlClient;
  }
}
