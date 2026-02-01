package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MappingEngineTest {
  private MappingEngine engine;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    engine = new MappingEngine();
  }

  @Test
  void testSimpleMapping() throws IOException {
    String mapping =
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

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "Resources": [
                { "id": "1", "name": "Test Dataset", "description": "A test" }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    assertNotNull(result);
    assertTrue(result.has("@context"));
    assertTrue(result.has("@graph"));
    assertEquals(1, result.get("@graph").size());

    JsonNode item = result.get("@graph").get(0);
    assertEquals("http://example.org/resource/1", item.get("@id").asText());
    assertEquals("dcat:Dataset", item.get("@type").asText());
    assertEquals("Test Dataset", item.get("dct:title").asText());
    assertEquals("A test", item.get("dct:description").asText());
  }

  @Test
  void testExpressionEvaluation() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        _context:
          baseUrl: "=_request.baseUrl"
          schema: "=_schema.name"

        Resources:
          _id: "=baseUrl + '/' + schema + '/resource/' + id"
          _type: dcat:Dataset
          name: dct:title
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "_schema": { "name": "testSchema" },
              "Resources": [
                { "id": "123", "name": "My Resource" }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("http://example.org/testSchema/resource/123", item.get("@id").asText());
  }

  @Test
  void testNestedLetScopes() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        _context:
          baseUrl: "=_request.baseUrl"

        Resources:
          _let:
            resourceUrl: "=baseUrl + '/resource/' + id"
          _id: "=resourceUrl"
          _type: dcat:Dataset
          publisher:
            _let:
              orgUrl: "=resourceUrl + '/org'"
            predicate: dct:publisher
            value:
              "@id": "=orgUrl + '/' + publisher.id"
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_request": { "baseUrl": "http://example.org" },
              "Resources": [
                { "id": "1", "publisher": { "id": "org1" } }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("http://example.org/resource/1", item.get("@id").asText());
    assertTrue(item.has("dct:publisher"));

    JsonNode publisher = item.get("dct:publisher");
    assertEquals("http://example.org/resource/1/org/org1", publisher.get("@id").asText());
  }

  @Test
  void testForeachArrayIteration() throws IOException {
    String mapping =
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

    JsonNode result = engine.transform(mapping, input);

    JsonNode item = result.get("@graph").get(0);
    assertTrue(item.has("dcat:contactPoint"));

    JsonNode contacts = item.get("dcat:contactPoint");
    assertTrue(contacts.isArray());
    assertEquals(2, contacts.size());

    JsonNode contact1 = contacts.get(0);
    assertEquals("http://example.org/resource/1/contact/John", contact1.get("@id").asText());
    assertEquals("vcard:Kind", contact1.get("@type").asText());
    assertEquals("John Doe", contact1.get("vcard:fn").asText());

    JsonNode contact2 = contacts.get(1);
    assertEquals("http://example.org/resource/1/contact/Jane", contact2.get("@id").asText());
    assertEquals("Jane Smith", contact2.get("vcard:fn").asText());
  }

  @Test
  void testNullIdHandling() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          _type: dcat:Dataset
          name: dct:title
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "Resources": [
                { "id": null, "name": "Should be skipped" },
                { "id": "1", "name": "Should appear" }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    assertEquals(1, result.get("@graph").size());
    JsonNode item = result.get("@graph").get(0);
    assertEquals("1", item.get("@id").asText());
  }

  @Test
  void testSettingsNormalization() throws IOException {
    String mapping =
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

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "_settings": [
                { "key": "organization", "value": "MOLGENIS" },
                { "key": "other", "value": "test" }
              ],
              "Resources": [
                { "id": "1" }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("MOLGENIS", item.get("dct:publisher").asText());
  }

  @Test
  void testConditionalType() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          _type: "=type === 'catalog' ? 'dcat:Catalog' : 'dcat:Dataset'"
          name: dct:title
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "Resources": [
                { "id": "1", "type": "catalog", "name": "Cat" },
                { "id": "2", "type": "dataset", "name": "Data" }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    JsonNode item1 = result.get("@graph").get(0);
    assertEquals("dcat:Catalog", item1.get("@type").asText());

    JsonNode item2 = result.get("@graph").get(1);
    assertEquals("dcat:Dataset", item2.get("@type").asText());
  }

  @Test
  void testMaxRowsLimit() {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          _type: dcat:Dataset
        """;

    StringBuilder inputBuilder = new StringBuilder("{\"Resources\": [");
    for (int i = 1; i <= 10001; i++) {
      if (i > 1) inputBuilder.append(",");
      inputBuilder.append("{\"id\": \"").append(i).append("\"}");
    }
    inputBuilder.append("]}");

    assertThrows(
        FairMapperException.class,
        () -> {
          JsonNode input = objectMapper.readTree(inputBuilder.toString());
          engine.transform(mapping, input);
        });
  }

  @Test
  void testInvalidForeachSyntax() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          contacts:
            predicate: dcat:contactPoint
            foreach: invalid syntax
            value:
              "@id": "=c.id"
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "Resources": [
                { "id": "1", "contacts": [{ "id": "c1" }] }
              ]
            }
            """);

    assertThrows(FairMapperException.class, () -> engine.transform(mapping, input));
  }

  @Test
  void testNullSafeAccess() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          _type: dcat:Dataset
          publisher:
            predicate: dct:publisher
            value: "=publisher?.name"
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "Resources": [
                { "id": "1", "publisher": null },
                { "id": "2", "publisher": { "name": "MOLGENIS" } }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    JsonNode item1 = result.get("@graph").get(0);
    assertFalse(item1.has("dct:publisher"));

    JsonNode item2 = result.get("@graph").get(1);
    assertEquals("MOLGENIS", item2.get("dct:publisher").asText());
  }

  @Test
  void testEmptyTable() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          _type: dcat:Dataset
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "Resources": []
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    assertTrue(result.has("@graph"));
    assertEquals(0, result.get("@graph").size());
  }

  @Test
  void testMissingTable() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#

        Resources:
          _id: "=id"
          _type: dcat:Dataset
        """;

    JsonNode input = objectMapper.readTree("{}");

    JsonNode result = engine.transform(mapping, input);

    assertTrue(result.has("@graph"));
    assertEquals(0, result.get("@graph").size());
  }

  @Test
  void testMultipleTables() throws IOException {
    String mapping =
        """
        _prefixes:
          dcat: http://www.w3.org/ns/dcat#
          foaf: http://xmlns.com/foaf/0.1/

        Resources:
          _id: "=id"
          _type: dcat:Dataset
          name: dct:title

        Organisations:
          _id: "=id"
          _type: foaf:Organization
          name: foaf:name
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "Resources": [
                { "id": "r1", "name": "Resource 1" }
              ],
              "Organisations": [
                { "id": "o1", "name": "Org 1" }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    assertEquals(2, result.get("@graph").size());

    JsonNode item1 = result.get("@graph").get(0);
    assertEquals("r1", item1.get("@id").asText());
    assertEquals("dcat:Dataset", item1.get("@type").asText());

    JsonNode item2 = result.get("@graph").get(1);
    assertEquals("o1", item2.get("@id").asText());
    assertEquals("foaf:Organization", item2.get("@type").asText());
  }

  @Test
  void testValueWithTransform() throws IOException {
    String mapping =
        """
        _prefixes:
          vcard: http://www.w3.org/2006/vcard/ns#

        Contacts:
          _id: "=id"
          _type: vcard:Kind
          email:
            predicate: vcard:hasEmail
            value: "='mailto:' + email"
        """;

    JsonNode input =
        objectMapper.readTree(
            """
            {
              "Contacts": [
                { "id": "c1", "email": "test@example.org" }
              ]
            }
            """);

    JsonNode result = engine.transform(mapping, input);

    JsonNode item = result.get("@graph").get(0);
    assertEquals("mailto:test@example.org", item.get("vcard:hasEmail").asText());
  }

  @Test
  void testInfiniteLoopProtection() throws IOException {
    String mapping =
        """
        _prefixes: {}
        Test:
          _id: "=(() => { while(true) {} })()"
        """;

    JsonNode input = objectMapper.readTree("{\"Test\": [{ \"id\": \"1\" }]}");

    assertThrows(FairMapperException.class, () -> engine.transform(mapping, input));
  }
}
