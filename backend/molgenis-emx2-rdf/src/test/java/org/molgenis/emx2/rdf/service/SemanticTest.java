package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.rdf.RdfUtils.SETTING_SEMANTIC_PREFIXES;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

class SemanticTest extends RdfServiceTestRunner {
  private static final String SCHEMA_NAME = SemanticTest.class.getSimpleName();

  static Schema semanticTest;

  @BeforeAll
  static void beforeAll() {
    semanticTest = database.dropCreateSchema(SCHEMA_NAME);

    semanticTest.create(
        table(
            "valid",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("title")
                .setType(ColumnType.STRING)
                .setSemantics("http://purl.org/dc/terms/title"),
            column("description").setType(ColumnType.STRING).setSemantics("dcterms:description"),
            column("nonDefinedPrefix")
                .setType(ColumnType.STRING)
                .setSemantics("nonDefinedPrefix:value")),
        table(
            "invalid",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("theme").setType(ColumnType.STRING).setSemantics("theme")));

    semanticTest
        .getTable("valid")
        .insert(
            row("id", "1", "title", "test", "description", "test2", "nonDefinedPrefix", "test3"));
    semanticTest.getTable("invalid").insert(row("id", "2", "theme", "test4"));

    semanticTest
        .getMetadata()
        .setSetting(SETTING_SEMANTIC_PREFIXES, "dcterms,http://purl.org/dc/terms/");
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void testSemanticPrefixesSetting() throws IOException {
    final Set<Namespace> defaultNamespaces =
        new HashSet<>() {
          {
            add(new SimpleNamespace("PrefixesEdit", BASE_URL + "/PrefixesEdit/api/rdf/"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final Set<Namespace> customNamespaces =
        new HashSet<>() {
          {
            add(new SimpleNamespace("PrefixesEdit", BASE_URL + "/PrefixesEdit/api/rdf/"));
            add(new SimpleNamespace("dcat", "http://www.w3.org/ns/dcat#"));
            add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
          }
        };

    final String customPrefixes =
        """
  dcat,http://www.w3.org/ns/dcat#
  dcterms,http://purl.org/dc/terms/
  """;

    try {
      Schema schema = database.dropCreateSchema("PrefixesEdit");
      // Test default behaviour.
      assertFalse(schema.hasSetting(SETTING_SEMANTIC_PREFIXES));
      InMemoryRDFHandler handlerBefore = parseSchemaRdf(schema);
      assertEquals(defaultNamespaces, handlerBefore.namespaces);

      // Change setting
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);

      // Test behaviour after changing setting.
      InMemoryRDFHandler handlerAfter = parseSchemaRdf(schema);
      assertEquals(customNamespaces, handlerAfter.namespaces);
    } finally {
      database.dropSchemaIfExists("PrefixesEdit");
    }
  }

  @Test
  void testMissingIriSemanticPrefixesSetting() {
    final String customPrefixes = "example,example";

    try {
      Schema schema = database.dropCreateSchema("PrefixesMissingIri");
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);
      assertThrows(MolgenisException.class, () -> parseSchemaRdf(schema));
    } finally {
      database.dropSchemaIfExists("PrefixesMissingIri");
    }
  }

  @Test
  void testIllegalPrefixSemanticPrefixesSetting() {
    final String customPrefixes = "urn,http://example.com";

    try {
      Schema schema = database.dropCreateSchema("PrefixesIllegalPrefix");
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);
      assertThrows(MolgenisException.class, () -> parseSchemaRdf(schema));
    } finally {
      database.dropSchemaIfExists("PrefixesIllegalPrefix");
    }
  }

  @Test
  void testEmptySemanticPrefixesSetting() throws IOException {
    final Set<Namespace> expectedNamespaces =
        new HashSet<>() {
          {
            add(new SimpleNamespace("PrefixesEmpty", BASE_URL + "/PrefixesEmpty/api/rdf/"));
          }
        };

    final String customPrefixes = "";

    try {
      Schema schema = database.dropCreateSchema("PrefixesEmpty");
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);
      InMemoryRDFHandler handler = parseSchemaRdf(schema);
      assertEquals(expectedNamespaces, handler.namespaces);
    } finally {
      database.dropSchemaIfExists("PrefixesEmpty");
    }
  }

  @Test
  void testDuplicateNamespaces() throws IOException {
    final Set<Namespace> expectedNamespace =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "PrefixSettingEqual1", BASE_URL + "/PrefixSettingEqual1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingEqual2", BASE_URL + "/PrefixSettingEqual2/api/rdf/"));
            add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
          }
        };

    final String customPrefixes1 =
        """
      dcterms,http://purl.org/dc/terms/
      """;

    final String customPrefixes2 =
        """
      dcterms,http://purl.org/dc/terms/
      """;

    validateNamespaces("PrefixSettingEqual", expectedNamespace, customPrefixes1, customPrefixes2);
  }

  /**
   * If 2 namespaces share the same IRI, the first one is kept and used for everything.
   *
   * @throws IOException
   */
  @Test
  void testNamespaceDifferentPrefixSameUrl() throws IOException {
    final Set<Namespace> expectedNamespace =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "PrefixSettingName1", BASE_URL + "/PrefixSettingName1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingName2", BASE_URL + "/PrefixSettingName2/api/rdf/"));
            add(new SimpleNamespace("dcterms1", "http://purl.org/dc/terms/"));
          }
        };

    final String customPrefixes1 =
        """
      dcterms1,http://purl.org/dc/terms/
      """;

    final String customPrefixes2 =
        """
      dcterms2,http://purl.org/dc/terms/
      """;

    validateNamespaces("PrefixSettingName", expectedNamespace, customPrefixes1, customPrefixes2);
  }

  /**
   * If multiple namespace share the same prefix but refer to a different IRI, the first one is
   * kept. IRIs belonging to the other prefix are not broken but are simply not shortened.
   *
   * @throws IOException
   */
  @Test
  void testNamespaceDifferentUrlSamePrefix() throws IOException {
    final Set<Namespace> expectedNamespace =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "PrefixSettingNameIri1", BASE_URL + "/PrefixSettingNameIri1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingNameIri2", BASE_URL + "/PrefixSettingNameIri2/api/rdf/"));
            add(new SimpleNamespace("name", "http://purl.org/dc/terms/"));
          }
        };

    final String customPrefixes1 =
        """
      name,http://purl.org/dc/terms/
         """;

    final String customPrefixes2 =
        """
      name,http://www.w3.org/2000/01/rdf-schema#
         """;

    validateNamespaces("PrefixSettingNameIri", expectedNamespace, customPrefixes1, customPrefixes2);
  }

  @Test
  void testSingleCustomPrefixesSetting() throws IOException {
    final Set<Namespace> expectedNamespaces =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "PrefixSettingPartly1", BASE_URL + "/PrefixSettingPartly1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingPartly2", BASE_URL + "/PrefixSettingPartly2/api/rdf/"));
            add(new SimpleNamespace("example", "http://example.com/"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final String customPrefixes1 =
        """
      example,http://example.com/
         """;

    validateNamespaces("PrefixSettingPartly", expectedNamespaces, customPrefixes1, null);
  }

  @Test
  void prefixedNames() throws IOException {
    Set<IRI> expectedPredicates =
        Set.of(
            Values.iri("http://purl.org/dc/terms/title"),
            Values.iri("http://purl.org/dc/terms/description"));

    InMemoryRDFHandler handler = parseTableRdf(semanticTest, "valid");
    Set<IRI> actualPredicates =
        handler.resources.get(Values.iri(getApi(semanticTest) + "Valid/id=1")).keySet();
    assertTrue(actualPredicates.containsAll(expectedPredicates));

    assertThrows(MolgenisException.class, () -> parseTableRdf(semanticTest, "invalid"));
  }
}
