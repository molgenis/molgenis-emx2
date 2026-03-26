package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.TypeDiscriminator.TableAssignment;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TypeDiscriminatorTest {

  private static TypeDiscriminator discriminator;

  @BeforeAll
  static void setUp() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema("TypeDiscriminatorTest");

    schema.create(
        new TableMetadata("Agents")
            .setTableType(TableType.DATA)
            .setSemantics("foaf:Agent")
            .add(new Column("pid").setPkey()));

    schema.create(
        new TableMetadata("Organisations")
            .setTableType(TableType.DATA)
            .setInheritName("Agents")
            .setSemantics("foaf:Agent,org:Organization")
            .add(new Column("name")));

    schema.create(
        new TableMetadata("Contacts")
            .setTableType(TableType.DATA)
            .setSemantics("vcard:Individual")
            .add(new Column("pid").setPkey()));

    schema.create(
        new TableMetadata("Resources")
            .setTableType(TableType.DATA)
            .add(new Column("pid").setPkey())
            .add(new Column("type").setType(ColumnType.ONTOLOGY).setRefTable("Resource types")));

    Table resourceTypes = schema.getTable("Resource types");
    resourceTypes.insert(
        new Row()
            .setString("name", "Catalogue")
            .setString("ontologyTermURI", "http://www.w3.org/ns/dcat#Catalog"),
        new Row()
            .setString("name", "Cohort study")
            .setString("ontologyTermURI", "http://www.w3.org/ns/dcat#Dataset")
            .setStringArray(
                "alternativeIds", new String[] {"http://www.w3.org/ns/dcat#DatasetSeries"}));

    discriminator = new TypeDiscriminator(schema);
  }

  @Test
  void catalogMapsToResourcesCatalogue() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/dcat#Catalog"));
    TableAssignment result = discriminator.assignTable(types);
    assertEquals("Resources", result.tableName());
    assertEquals("type", result.typeColumnName());
    assertEquals("Catalogue", result.typeValue());
  }

  @Test
  void datasetMapsToResourcesCohortStudy() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/dcat#Dataset"));
    TableAssignment result = discriminator.assignTable(types);
    assertEquals("Resources", result.tableName());
    assertEquals("type", result.typeColumnName());
    assertEquals("Cohort study", result.typeValue());
  }

  @Test
  void agentMapsToOrganisations() {
    Set<IRI> types = Set.of(Values.iri("http://xmlns.com/foaf/0.1/Agent"));
    TableAssignment result = discriminator.assignTable(types);
    assertEquals("Organisations", result.tableName());
    assertNull(result.typeColumnName());
    assertNull(result.typeValue());
  }

  @Test
  void organizationMapsToOrganisations() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/org#Organization"));
    TableAssignment result = discriminator.assignTable(types);
    assertEquals("Organisations", result.tableName());
    assertNull(result.typeColumnName());
    assertNull(result.typeValue());
  }

  @Test
  void vcardIndividualMapsToContacts() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/2006/vcard/ns#Individual"));
    TableAssignment result = discriminator.assignTable(types);
    assertEquals("Contacts", result.tableName());
    assertNull(result.typeColumnName());
    assertNull(result.typeValue());
  }

  @Test
  void datasetSeriesMapsToResourcesCohortStudy() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/dcat#DatasetSeries"));
    TableAssignment result = discriminator.assignTable(types);
    assertEquals("Resources", result.tableName());
    assertEquals("type", result.typeColumnName());
    assertEquals("Cohort study", result.typeValue());
  }

  @Test
  void unknownTypeReturnsNull() {
    Set<IRI> types = Set.of(Values.iri("http://example.org/Unknown"));
    assertNull(discriminator.assignTable(types));
  }

  @Test
  void emptyTypesReturnsNull() {
    assertNull(discriminator.assignTable(Set.of()));
  }

  @Test
  void multipleTypesUsesFirstMatch() {
    Set<IRI> types =
        Set.of(
            Values.iri("http://www.w3.org/ns/dcat#Catalog"),
            Values.iri("http://www.w3.org/ns/dcat#Dataset"));
    TableAssignment result = discriminator.assignTable(types);
    assertNotNull(result);
    assertEquals("Resources", result.tableName());
  }
}
