package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.rdf.RdfUtils.SETTING_CUSTOM_RDF;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

class CustomRdfTest extends RdfServiceTestRunner {
  private static final String SCHEMA_NAME_PREFIX = RefLinkTest.class.getSimpleName() + "_";

  @Test
  void testCustomRdfSetting() throws IOException {
    final String schemaName = SCHEMA_NAME_PREFIX + "CustomRdfEdit";

    final Set<Namespace> defaultNamespaces =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "RefLinkTest_CustomRdfEdit", BASE_URL + "/RefLinkTest_CustomRdfEdit/api/rdf/"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final String customRdf =
        """
      @prefix example: <http://example.com/> .
      <https://molgenis.org/> example:test "Molgenis" .
      """;

    try {
      Schema schema = database.dropCreateSchema(schemaName);
      // Test default behaviour.
      assertFalse(schema.hasSetting(SETTING_CUSTOM_RDF));
      InMemoryRDFHandler handlerBefore = parseSchemaRdf(schema);
      assertFalse(handlerBefore.resources.containsKey(Values.iri("https://molgenis.org/")));

      // Change setting
      schema.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);

      // Test behaviour after changing setting.
      InMemoryRDFHandler handlerAfter = parseSchemaRdf(schema);
      assertEquals(
          defaultNamespaces, handlerAfter.namespaces); // example prefix should NOT be present
      assertTrue(
          handlerAfter
              .resources
              .get(Values.iri("https://molgenis.org/"))
              .get(Values.iri("http://example.com/test"))
              .contains(Values.literal("Molgenis")));

    } finally {
      database.dropSchemaIfExists(schemaName);
    }
  }

  /**
   * While setting the custom_RDF does not validate, trying to use the RDF API will result in an
   * error if invalid RDF is given. In this case a dot is missing to indicate the end of the triple.
   */
  @Test
  void testInvalidCustomRdfSetting() {
    final String schemaName = SCHEMA_NAME_PREFIX + "CustomRdfInvalid";

    final String customRdf =
        """
      <https://molgenis.org/> <http://purl.org/dc/terms/title> "Molgenis"
      """;

    try {
      Schema schema = database.dropCreateSchema(schemaName);
      schema.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);
      assertThrows(MolgenisException.class, () -> parseSchemaRdf(schema));
    } finally {
      database.dropSchemaIfExists(schemaName);
    }
  }

  @Test
  void testEmptyCustomRdfSetting() {
    final String schemaName = SCHEMA_NAME_PREFIX + "CustomRdfEmpty";

    final String customRdf = "";

    try {
      Schema schema = database.dropCreateSchema(schemaName);
      schema.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);
      assertDoesNotThrow(() -> parseSchemaRdf(schema));
    } finally {
      database.dropSchemaIfExists(schemaName);
    }
  }
}
