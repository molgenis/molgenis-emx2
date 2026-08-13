package org.molgenis.emx2.rdf.mappers;

import static org.eclipse.rdf4j.model.util.Values.namespace;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.rdf.RdfUtils;

class NamespaceMapperTest {
  static final String BASE_URL = "http://localhost:8080";
  static final ValueFactory valueFactory = SimpleValueFactory.getInstance();

  @Test
  void testValidCustomPrefixes() {
    SchemaMetadata schema =
        createSchemaWithSemantics(
            "mySchema",
"""
rdf,http://www.w3.org/1999/02/22-rdf-syntax-ns#
""");
    NamespaceMapper mapper = new NamespaceMapper(BASE_URL, schema);

    assertAll(
        // prefixed name
        () ->
            assertEquals(
                valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                mapper.map(schema, "rdf:type")),

        // using IRI even though prefixed name is available
        () ->
            assertEquals(
                valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                mapper.map(schema, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")),
        // using IRI when no prefixed name is available
        () ->
            assertEquals(
                valueFactory.createIRI("http://purl.org/dc/terms/title"),
                mapper.map(schema, "http://purl.org/dc/terms/title")),
        //  IRI with non-typical scheme (see
        // https://www.iana.org/assignments/uri-schemes/uri-schemes.xhtml)
        //  OR prefix is not configured
        () ->
            assertEquals(
                valueFactory.createIRI("undefinedPrefix:value"),
                mapper.map(schema, "undefinedPrefix:value")),

        // incomplete semantics (IRI scheme-only / prefix without value)
        () -> assertThrows(IllegalArgumentException.class, () -> mapper.map(schema, "test")));
  }

  @Test
  void testInvalidCustomPrefixes() {
    SchemaMetadata schema =
        createSchemaWithSemantics(
            "mySchema",
            """
    invalid,thisFieldIsInvalid
    """);
    assertThrows(MolgenisException.class, () -> new NamespaceMapper(BASE_URL, schema));
  }

  @Test
  void testMultiSchemaWithCustomConfig() {
    // prefix 1 -> only in schema 1
    // prefix 2 -> same prefix, different URL
    // prefix 3 -> only in schema 1, same URL als prefix 4 in schema 2
    // prefix 4 -> only in schema 2, same URL als prefix 3 in schema 1
    // prefix 5 -> duplicate in both
    // prefix 6 -> only in schema 2

    SchemaMetadata schema1 =
        createSchemaWithSemantics(
            "schema1",
            """
            prefix1,http://example.com/schema1only#
            prefix2,http://example.com/conflicting_url1#
            prefix3,http://example.com/identical_url_different_prefix#
            prefix5,http://example.com/duplicate#
            """);

    SchemaMetadata schema2 =
        createSchemaWithSemantics(
            "schema2",
            """
                prefix2,http://example.com/conflicting_url2#
                prefix4,http://example.com/identical_url_different_prefix#
                prefix5,http://example.com/duplicate#
                prefix6,http://example.com/schema2only#
                """);

    // RDF4J already handles if conflicting items are given so use their default instead of custom
    // implementation.
    Set<Namespace> expectedNamespaces =
        Set.of(
            namespace("Schema1", BASE_URL + "/schema1/api/rdf/"),
            namespace("Schema2", BASE_URL + "/schema2/api/rdf/"),
            namespace("prefix1", "http://example.com/schema1only#"),
            namespace("prefix2", "http://example.com/conflicting_url1#"),
            namespace("prefix3", "http://example.com/identical_url_different_prefix#"),
            namespace("prefix5", "http://example.com/duplicate#"),
            namespace("prefix6", "http://example.com/schema2only#"));

    // Added in non-alphabetical order as this should not matter.
    NamespaceMapper mapper = new NamespaceMapper(BASE_URL, List.of(schema2, schema1));
    assertEquals(expectedNamespaces, mapper.getAllNamespaces());
  }

  @Test
  void nonExistingSetting() {
    String schemaName = "missingSettingSchema";
    String schemaId = "MissingSettingSchema";

    SchemaMetadata schema = new SchemaMetadata(schemaName);
    NamespaceMapper mapper = new NamespaceMapper(BASE_URL, schema);

    Set<Namespace> expectedNamespaces = DefaultNamespace.streamAll().collect(Collectors.toSet());
    expectedNamespaces.add(namespace(schemaId, BASE_URL + "/" + schemaName + "/api/rdf/"));

    assertAll(
        () -> assertEquals(expectedNamespaces, mapper.getAllNamespaces()),
        () -> assertEquals(expectedNamespaces, mapper.getAllNamespaces(schema)));
  }

  private SchemaMetadata createSchemaWithSemantics(
      String schemaName, String semanticPrefixesSetting) {
    return new SchemaMetadata(schemaName)
        .setSetting(RdfUtils.SETTING_SEMANTIC_PREFIXES, semanticPrefixesSetting);
  }
}
