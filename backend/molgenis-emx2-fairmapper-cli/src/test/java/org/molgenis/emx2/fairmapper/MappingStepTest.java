package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.fairmapper.engine.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.engine.RemotePipelineExecutor;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.step.MappingStep;

class MappingStepTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private GraphqlClient mockClient;
  private JsltTransformEngine mockTransformEngine;
  private RemotePipelineExecutor executor;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    mockClient = mock(GraphqlClient.class);
    mockTransformEngine = mock(JsltTransformEngine.class);
    executor = new RemotePipelineExecutor(mockClient, mockTransformEngine, tempDir, "testSchema");
  }

  @Test
  void testSimpleMappingStep() throws IOException {
    String mappingYaml =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#
          dct: http://purl.org/dc/terms/

        Resources:
          _id: "=_request.baseUrl + '/resource/' + id"
          _type: dcat:Dataset
          name: dct:title
          description: dct:description
        """;

    Files.writeString(tempDir.resolve("export.yaml"), mappingYaml);

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "Resources": [
                { "id": "1", "name": "Test Dataset", "description": "Test description" }
              ]
            }
            """);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new MappingStep("export.yaml", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
    assertTrue(result.has("@context"));
    assertTrue(result.has("@graph"));
    assertEquals(1, result.get("@graph").size());

    JsonNode item = result.get("@graph").get(0);
    assertEquals("http://example.org/resource/1", item.get("@id").asText());
    assertEquals("dcat:Dataset", item.get("@type").asText());
    assertEquals("Test Dataset", item.get("dct:title").asText());
    assertEquals("Test description", item.get("dct:description").asText());
  }

  @Test
  void testMappingStepWithForeach() throws IOException {
    String mappingYaml =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#
          vcard: http://www.w3.org/2006/vcard/ns#

        _context:
          baseUrl: "=_request.baseUrl"

        Resources:
          _let:
            resourceUrl: "=baseUrl + '/resource/' + id"
          _id: "=resourceUrl"
          _type: dcat:Dataset
          contacts:
            _let:
              contactBase: "=resourceUrl + '/contact'"
            predicate: dcat:contactPoint
            foreach: c in contacts
            value:
              "@id": "=contactBase + '/' + c.firstName"
              "@type": vcard:Kind
              "vcard:fn": "=c.firstName + ' ' + c.lastName"
        """;

    Files.writeString(tempDir.resolve("export.yaml"), mappingYaml);

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "Resources": [
                {
                  "id": "1",
                  "contacts": [
                    { "firstName": "John", "lastName": "Doe" },
                    { "firstName": "Jane", "lastName": "Smith" }
                  ]
                }
              ]
            }
            """);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new MappingStep("export.yaml", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
    JsonNode item = result.get("@graph").get(0);
    assertTrue(item.has("dcat:contactPoint"));

    JsonNode contacts = item.get("dcat:contactPoint");
    assertTrue(contacts.isArray());
    assertEquals(2, contacts.size());

    JsonNode contact1 = contacts.get(0);
    assertEquals("http://example.org/resource/1/contact/John", contact1.get("@id").asText());
    assertEquals("John Doe", contact1.get("vcard:fn").asText());
  }

  @Test
  void testMappingStepFileNotFound() {
    JsonNode input = objectMapper.createObjectNode();
    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new MappingStep("nonexistent.yaml", null)),
            null);

    assertThrows(IOException.class, () -> executor.execute(input, mapping));
  }

  @Test
  void testMappingStepInvalidPath() {
    JsonNode input = objectMapper.createObjectNode();
    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new MappingStep("../outside/mapping.yaml", null)),
            null);

    assertThrows(FairMapperException.class, () -> executor.execute(input, mapping));
  }

  @Test
  void testMappingStepWithMultipleTables() throws IOException {
    String mappingYaml =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#
          foaf: http://xmlns.com/foaf/0.1/

        Resources:
          _id: "=_request.baseUrl + '/resource/' + id"
          _type: dcat:Dataset
          name: dct:title

        Organisations:
          _id: "=_request.baseUrl + '/organisation/' + id"
          _type: foaf:Organization
          name: foaf:name
        """;

    Files.writeString(tempDir.resolve("export.yaml"), mappingYaml);

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "Resources": [
                { "id": "r1", "name": "Resource 1" }
              ],
              "Organisations": [
                { "id": "o1", "name": "Org 1" }
              ]
            }
            """);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new MappingStep("export.yaml", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertEquals(2, result.get("@graph").size());

    JsonNode item1 = result.get("@graph").get(0);
    assertEquals("http://example.org/resource/r1", item1.get("@id").asText());
    assertEquals("dcat:Dataset", item1.get("@type").asText());

    JsonNode item2 = result.get("@graph").get(1);
    assertEquals("http://example.org/organisation/o1", item2.get("@id").asText());
    assertEquals("foaf:Organization", item2.get("@type").asText());
  }

  @Test
  void testMappingStepWithSettings() throws IOException {
    String mappingYaml =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        _context:
          orgName: "=settings.organization || 'Unknown'"

        Resources:
          _id: "=id"
          _type: dcat:Dataset
          organization:
            predicate: dct:publisher
            value: "=orgName"
        """;

    Files.writeString(tempDir.resolve("export.yaml"), mappingYaml);

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_settings": [
                { "key": "organization", "value": "MOLGENIS" }
              ],
              "Resources": [
                { "id": "1" }
              ]
            }
            """);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new MappingStep("export.yaml", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("MOLGENIS", item.get("dct:publisher").asText());
  }

  @Test
  void testMappingStepEmptyInput() throws IOException {
    String mappingYaml =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          _type: dcat:Dataset
        """;

    Files.writeString(tempDir.resolve("export.yaml"), mappingYaml);

    JsonNode input = objectMapper.readTree("{ \"Resources\": [] }");

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new MappingStep("export.yaml", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
    assertTrue(result.has("@graph"));
    assertEquals(0, result.get("@graph").size());
  }
}
