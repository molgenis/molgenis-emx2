package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.rdf.TypeDiscriminator.TableAssignment;

class TypeDiscriminatorTest {

  @Test
  void catalogMapsToResourcesCatalogue() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/dcat#Catalog"));
    TableAssignment result = TypeDiscriminator.assignTable(types);
    assertEquals("Resources", result.tableName());
    assertEquals("Catalogue", result.typeValue());
  }

  @Test
  void datasetMapsToResourcesCohortStudy() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/dcat#Dataset"));
    TableAssignment result = TypeDiscriminator.assignTable(types);
    assertEquals("Resources", result.tableName());
    assertEquals("Cohort study", result.typeValue());
  }

  @Test
  void agentMapsToOrganisations() {
    Set<IRI> types = Set.of(Values.iri("http://xmlns.com/foaf/0.1/Agent"));
    TableAssignment result = TypeDiscriminator.assignTable(types);
    assertEquals("Organisations", result.tableName());
    assertNull(result.typeValue());
  }

  @Test
  void organizationMapsToOrganisations() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/org#Organization"));
    TableAssignment result = TypeDiscriminator.assignTable(types);
    assertEquals("Organisations", result.tableName());
    assertNull(result.typeValue());
  }

  @Test
  void vcardIndividualMapsToContacts() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/2006/vcard/ns#Individual"));
    TableAssignment result = TypeDiscriminator.assignTable(types);
    assertEquals("Contacts", result.tableName());
    assertNull(result.typeValue());
  }

  @Test
  void datasetSeriesMapsToResourcesCohortStudy() {
    Set<IRI> types = Set.of(Values.iri("http://www.w3.org/ns/dcat#DatasetSeries"));
    TableAssignment result = TypeDiscriminator.assignTable(types);
    assertEquals("Resources", result.tableName());
    assertEquals("Cohort study", result.typeValue());
  }

  @Test
  void unknownTypeReturnsNull() {
    Set<IRI> types = Set.of(Values.iri("http://example.org/Unknown"));
    assertNull(TypeDiscriminator.assignTable(types));
  }

  @Test
  void emptyTypesReturnsNull() {
    assertNull(TypeDiscriminator.assignTable(Set.of()));
  }

  @Test
  void multipleTypesUsesFirstMatch() {
    Set<IRI> types =
        Set.of(
            Values.iri("http://www.w3.org/ns/dcat#Catalog"),
            Values.iri("http://www.w3.org/ns/dcat#Dataset"));
    TableAssignment result = TypeDiscriminator.assignTable(types);
    assertEquals("Catalogue", result.typeValue());
  }
}
