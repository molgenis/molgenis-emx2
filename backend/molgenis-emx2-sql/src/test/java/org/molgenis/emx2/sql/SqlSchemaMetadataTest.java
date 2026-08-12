package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.SETTING_SEMANTIC_PREFIXES;

import java.util.*;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SemanticPrefixes;

class SqlSchemaMetadataTest {

  @Test
  void givenAdminUser_whenRequestingInheritedRoles_thenReturnAllPrivileges() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(getClass().getSimpleName());
    database.becomeAdmin();

    List<String> expectedRoles =
        Arrays.stream(Privileges.values()).map(Privileges::toString).toList();
    assertEquals(expectedRoles, schema.getInheritedRolesForActiveUser());
  }

  @Test
  void testSemanticPrefixesUpdated() {
    Namespace[] namespaces =
        new Namespace[] {
          new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"),
          new SimpleNamespace("dcat", "http://www.w3.org/ns/dcat#"),
          new SimpleNamespace("skos", "http://www.w3.org/2004/02/skos/core#")
        };

    final String customPrefixes1 =
        """
    dcterms,http://purl.org/dc/terms/
    dcat,http://www.w3.org/ns/dcat#
    """;

    final String customPrefixes2 =
        """
    dcterms,http://purl.org/dc/terms/
    skos,http://www.w3.org/2004/02/skos/core#
    """;

    final SemanticPrefixes defaultSemantics = new SemanticPrefixes((Collection<Namespace>) null);

    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(getClass().getSimpleName() + "_semanticPrefixes");
    assertEquals(defaultSemantics, schema.getMetadata().getSemanticPrefixes());

    schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes1);
    assertEquals(
        new SemanticPrefixes(Set.of(namespaces[0], namespaces[1])),
        schema.getMetadata().getSemanticPrefixes());

    schema
        .getMetadata()
        .setSettingsWithoutReload(Map.of(SETTING_SEMANTIC_PREFIXES, customPrefixes2));
    assertEquals(
        new SemanticPrefixes(Set.of(namespaces[0], namespaces[2])),
        schema.getMetadata().getSemanticPrefixes());

    schema.getMetadata().removeSetting(SETTING_SEMANTIC_PREFIXES);
    assertEquals(defaultSemantics, schema.getMetadata().getSemanticPrefixes());
  }
}
