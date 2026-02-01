package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DcatViaMappingE2eTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private MappingEngine engine;
  private String mappingYaml;

  @BeforeEach
  void setUp() throws IOException {
    engine = new MappingEngine();
    mappingYaml = loadMappingFile();
  }

  @Test
  void testJsonLdStructure() throws IOException {
    JsonNode input = createMinimalInput("cat1", "Test Catalogue", "Catalogue");
    JsonNode result = engine.transform(mappingYaml, input);

    assertNotNull(result);
    assertTrue(result.has("@context"));
    assertTrue(result.has("@graph"));

    JsonNode context = result.get("@context");
    assertEquals("http://www.w3.org/ns/dcat#", context.get("dcat").asText());
    assertEquals("http://purl.org/dc/terms/", context.get("dct").asText());
    assertEquals("http://xmlns.com/foaf/0.1/", context.get("foaf").asText());
    assertEquals("http://www.w3.org/2006/vcard/ns#", context.get("vcard").asText());
    assertEquals("http://www.w3.org/2004/02/skos/core#", context.get("skos").asText());
  }

  @Test
  void testCatalogTypeMapping() throws IOException {
    JsonNode input = createMinimalInput("cat1", "Test Catalogue", "Catalogue");
    JsonNode result = engine.transform(mappingYaml, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("http://example.org/catalogue/resource/cat1", item.get("@id").asText());
    assertEquals("dcat:Catalog", item.get("@type").asText());
    assertEquals("Test Catalogue", item.get("dct:title").asText());
  }

  @Test
  void testDatasetTypeMapping() throws IOException {
    JsonNode input = createMinimalInput("ds1", "Test Dataset", "Databank");
    JsonNode result = engine.transform(mappingYaml, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("dcat:Dataset", item.get("@type").asText());
  }

  @Test
  void testDataServiceTypeMapping() throws IOException {
    JsonNode input = createMinimalInput("srv1", "Test Service", "Dataservice");
    JsonNode result = engine.transform(mappingYaml, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("dcat:DataService", item.get("@type").asText());
  }

  @Test
  void testNetworkTypeMapping() throws IOException {
    JsonNode input = createMinimalInput("net1", "Test Network", "Network");
    JsonNode result = engine.transform(mappingYaml, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("dcat:Catalog", item.get("@type").asText());
  }

  @Test
  void testDefaultDistributionType() throws IOException {
    JsonNode input = createMinimalInput("dist1", "Test Distribution", "Other");
    JsonNode result = engine.transform(mappingYaml, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("dcat:Distribution", item.get("@type").asText());
  }

  @Test
  void testResourceWithPublisher() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "res1",
                  "name": "Resource",
                  "type": [{ "name": "Catalogue" }],
                  "publisher": {
                    "id": "org1",
                    "name": "Test Organization"
                  },
                  "accessRights": null,
                  "licence": null,
                  "contacts": null,
                  "theme": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode item = result.get("@graph").get(0);

    assertTrue(item.has("dct:publisher"));
    JsonNode publisher = item.get("dct:publisher");
    assertEquals("http://example.org/catalogue/organisation/org1", publisher.get("@id").asText());
    assertEquals("foaf:Organization", publisher.get("@type").asText());
    assertEquals("Test Organization", publisher.get("foaf:name").asText());
  }

  @Test
  void testResourceWithContacts() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "res1",
                  "name": "Resource",
                  "type": [{ "name": "Catalogue" }],
                  "contacts": [
                    {
                      "firstName": "John",
                      "lastName": "Doe",
                      "email": "john.doe@example.org"
                    },
                    {
                      "firstName": "Jane",
                      "lastName": "Smith",
                      "email": "jane.smith@example.org"
                    }
                  ],
                  "publisher": null,
                  "accessRights": null,
                  "licence": null,
                  "theme": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode item = result.get("@graph").get(0);

    assertTrue(item.has("dcat:contactPoint"));
    JsonNode contacts = item.get("dcat:contactPoint");
    assertTrue(contacts.isArray());
    assertEquals(2, contacts.size());

    JsonNode contact1 = contacts.get(0);
    assertEquals("http://example.org/catalogue/contact/John-Doe", contact1.get("@id").asText());
    assertEquals("vcard:Kind", contact1.get("@type").asText());
    assertEquals("John Doe", contact1.get("vcard:fn").asText());
    assertEquals("John", contact1.get("vcard:givenName").asText());
    assertEquals("Doe", contact1.get("vcard:familyName").asText());
    assertEquals("mailto:john.doe@example.org", contact1.get("vcard:hasEmail").asText());
  }

  @Test
  void testResourceWithThemes() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "res1",
                  "name": "Resource",
                  "type": [{ "name": "Catalogue" }],
                  "theme": [
                    { "ontologyTermURI": "http://example.org/theme/health" },
                    { "ontologyTermURI": "http://example.org/theme/science" }
                  ],
                  "publisher": null,
                  "contacts": null,
                  "accessRights": null,
                  "licence": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode item = result.get("@graph").get(0);

    assertTrue(item.has("dcat:theme"));
    JsonNode themes = item.get("dcat:theme");
    assertTrue(themes.isArray());
    assertEquals(2, themes.size());

    JsonNode theme1 = themes.get(0);
    assertEquals("http://example.org/theme/health", theme1.get("@id").asText());
    assertEquals("skos:Concept", theme1.get("@type").asText());
  }

  @Test
  void testResourceWithAccessRights() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "res1",
                  "name": "Resource",
                  "type": [{ "name": "Catalogue" }],
                  "accessRights": {
                    "ontologyTermURI": "http://publications.europa.eu/resource/authority/access-right/PUBLIC"
                  },
                  "publisher": null,
                  "contacts": null,
                  "theme": null,
                  "licence": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode item = result.get("@graph").get(0);

    assertTrue(item.has("dct:accessRights"));
    JsonNode accessRights = item.get("dct:accessRights");
    assertEquals(
        "http://publications.europa.eu/resource/authority/access-right/PUBLIC",
        accessRights.get("@id").asText());
    assertEquals("skos:Concept", accessRights.get("@type").asText());
  }

  @Test
  void testResourceWithLicence() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "res1",
                  "name": "Resource",
                  "type": [{ "name": "Catalogue" }],
                  "licence": {
                    "ontologyTermURI": "https://creativecommons.org/licenses/by/4.0/"
                  },
                  "publisher": null,
                  "contacts": null,
                  "theme": null,
                  "accessRights": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode item = result.get("@graph").get(0);

    assertTrue(item.has("dct:license"));
    JsonNode licence = item.get("dct:license");
    assertEquals("https://creativecommons.org/licenses/by/4.0/", licence.get("@id").asText());
  }

  @Test
  void testResourceWithMetadata() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "res1",
                  "name": "Resource",
                  "type": [{ "name": "Catalogue" }],
                  "pid": "doi:10.1234/test",
                  "keywords": ["health", "research", "data"],
                  "website": "http://example.org/resource",
                  "issued": "2024-01-01",
                  "modified": "2024-02-01",
                  "publisher": null,
                  "contacts": null,
                  "theme": null,
                  "accessRights": null,
                  "licence": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode item = result.get("@graph").get(0);

    assertEquals("doi:10.1234/test", item.get("dct:identifier").asText());
    assertEquals("http://example.org/resource", item.get("dcat:landingPage").asText());
    assertEquals("2024-01-01", item.get("dct:issued").asText());
    assertEquals("2024-02-01", item.get("dct:modified").asText());

    assertTrue(item.has("dcat:keyword"));
    JsonNode keywords = item.get("dcat:keyword");
    assertTrue(keywords.isArray());
    assertEquals(3, keywords.size());
  }

  @Test
  void testOrganisationMapping() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [],
              "Organisations": [
                {
                  "id": "org1",
                  "name": "Test Organization"
                },
                {
                  "id": "org2",
                  "name": "Another Organization"
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode graph = result.get("@graph");

    assertEquals(2, graph.size());

    JsonNode org1 = graph.get(0);
    assertEquals("http://example.org/catalogue/organisation/org1", org1.get("@id").asText());
    assertEquals("foaf:Organization", org1.get("@type").asText());
    assertEquals("Test Organization", org1.get("foaf:name").asText());
  }

  @Test
  void testCompleteDataset() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "ds1",
                  "name": "Complete Dataset",
                  "description": "A comprehensive test dataset",
                  "type": [{ "name": "Databank" }],
                  "pid": "doi:10.1234/sample",
                  "keywords": ["health", "research"],
                  "website": "http://example.org/datasets/ds1",
                  "issued": "2024-01-15",
                  "modified": "2024-01-30",
                  "publisher": {
                    "id": "org1",
                    "name": "Research Institute"
                  },
                  "contacts": [
                    {
                      "firstName": "Alice",
                      "lastName": "Researcher",
                      "email": "alice@example.org"
                    }
                  ],
                  "theme": [
                    { "ontologyTermURI": "http://example.org/theme/health" }
                  ],
                  "accessRights": {
                    "ontologyTermURI": "http://publications.europa.eu/resource/authority/access-right/PUBLIC"
                  },
                  "licence": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ],
              "Organisations": [
                {
                  "id": "org1",
                  "name": "Research Institute"
                }
              ]
            }
            """);

    JsonNode result = engine.transform(mappingYaml, input);
    JsonNode graph = result.get("@graph");

    assertEquals(2, graph.size());

    JsonNode dataset = graph.get(0);
    assertEquals("dcat:Dataset", dataset.get("@type").asText());
    assertTrue(dataset.has("dct:title"));
    assertTrue(dataset.has("dct:publisher"));
    assertTrue(dataset.has("dcat:contactPoint"));
    assertTrue(dataset.has("dcat:theme"));
    assertTrue(dataset.has("dct:accessRights"));
  }

  private JsonNode createMinimalInput(String id, String name, String typeName) throws IOException {
    return objectMapper.readTree(
        String.format(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "catalogue" },
              "Resources": [
                {
                  "id": "%s",
                  "name": "%s",
                  "type": [{ "name": "%s" }],
                  "publisher": null,
                  "contacts": null,
                  "theme": null,
                  "accessRights": null,
                  "licence": null,
                  "dataResources": null,
                  "childNetworks": null
                }
              ]
            }
            """,
            id, name, typeName));
  }

  private String loadMappingFile() throws IOException {
    Path mappingPath = Path.of("../../fair-mappings/dcat-via-mapping/src/export-mapping.yaml");
    return Files.readString(mappingPath);
  }
}
