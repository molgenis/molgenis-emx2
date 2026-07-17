package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

public class OntologyTest extends RdfTestLoaders {
  private static final String SCHEMA_NAME = RdfCompositeKeyTest.class.getSimpleName();
  private static final String CROSS_SCHEMA_NAME = SCHEMA_NAME + "_cross_schema";

  static Schema ontologyTest;
  static Schema ontologyCrossSchemaTest;

  @BeforeAll
  static void beforeAll() {
    ontologyTest = database.dropCreateSchema(SCHEMA_NAME);
    ontologyTest.create(table("Diseases").setTableType(TableType.ONTOLOGIES));
    ontologyTest.create(
        table(
            "Patients",
            column("name").setPkey(),
            column("diseases")
                .setSemantics("http://purl.obolibrary.org/obo/NCIT_C2991")
                .setType(ColumnType.ONTOLOGY_ARRAY)
                .setRefTable("Diseases")));

    ontologyTest
        .getTable("Diseases")
        .insert(
            row(
                "order",
                0,
                "name",
                "U07",
                "label",
                "Emergency Use of U07",
                "codesystem",
                "ICD-10",
                "code",
                "U07",
                "ontologyTermURI",
                "https://icd.who.int/browse10/2019/en#/U07",
                "definition",
                "Codes used by WHO for the provisional assignment of new diseases of uncertain etiology."),
            row(
                "order",
                1,
                "name",
                "U07.1",
                "label",
                "COVID-19",
                "parent",
                "U07",
                "codesystem",
                "ICD-10",
                "code",
                "U07.1",
                "ontologyTermURI",
                "https://icd.who.int/browse10/2019/en#/U07.1",
                "definition",
                "COVID-19 NOS"),
            row(
                "order",
                2,
                "name",
                "C00-C75 Malignant neoplasms, stated or presumed to be primary, of specified sites, except of lymphoid, haematopoietic and related tissue",
                "code",
                "C00-C75"),
            row(
                "order",
                3,
                "name",
                "C00-C14 Malignant neoplasms of lip, oral cavity and pharynx",
                "parent",
                "C00-C75 Malignant neoplasms, stated or presumed to be primary, of specified sites, except of lymphoid, haematopoietic and related tissue",
                "code",
                "C00-C14"));
    ontologyTest
        .getTable("Patients")
        .insert(
            row(
                "name",
                "bob",
                "diseases",
                "\"U07\", \"C00-C14 Malignant neoplasms of lip, oral cavity and pharynx\""));

    // Test for cross-schema references
    ontologyCrossSchemaTest = database.dropCreateSchema(CROSS_SCHEMA_NAME);
    ontologyCrossSchemaTest.create(
        table(
            "Patients",
            column("name").setPkey(),
            column("diseases")
                .setSemantics("http://purl.obolibrary.org/obo/NCIT_C2991")
                .setType(ColumnType.ONTOLOGY_ARRAY)
                .setRefSchemaName(SCHEMA_NAME)
                .setRefTable("Diseases")));

    ontologyCrossSchemaTest
        .getTable("Patients")
        .insert(
            row(
                "name",
                "pim",
                "diseases",
                "\"U07\", \"C00-C14 Malignant neoplasms of lip, oral cavity and pharynx\""));
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(CROSS_SCHEMA_NAME);
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void testThatURLsAreNotSplitForOntologyParentItem() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(ontologyTest, "Diseases");

    IRI subject =
        Values.iri(
            getApi(SCHEMA_NAME)
                + "Diseases/name=C00-C14%20Malignant%20neoplasms%20of%20lip%2C%20oral%20cavity%20and%20pharynx");

    Set<Value> parents = handler.resources.get(subject).get(RDFS.SUBCLASSOF);
    assertEquals(
        2, parents.size(), "This disease should only be a subclass of Diseases and C00-C75");
  }

  @Test
  void testDataTableOntologyColumnValue() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(ontologyTest, "Patients");

    Set<Value> expectedSemantic =
        Set.of(
            Values.iri("https://icd.who.int/browse10/2019/en#/U07"),
            Values.iri(
                getApi(SCHEMA_NAME)
                    + "Diseases/name=C00-C14%20Malignant%20neoplasms%20of%20lip%2C%20oral%20cavity%20and%20pharynx"));
    Set<Value> expectedNonSemantic =
        Set.of(
            Values.iri(getApi(SCHEMA_NAME) + "Diseases/name=U07"),
            Values.iri(
                getApi(SCHEMA_NAME)
                    + "Diseases/name=C00-C14%20Malignant%20neoplasms%20of%20lip%2C%20oral%20cavity%20and%20pharynx"));

    Set<Value> actualSemantic =
        handler
            .resources
            .get(Values.iri(getApi(SCHEMA_NAME) + "Patients/name=bob"))
            .get(Values.iri("http://purl.obolibrary.org/obo/NCIT_C2991"));
    Set<Value> actualNonSemantic =
        handler
            .resources
            .get(Values.iri(getApi(SCHEMA_NAME) + "Patients/name=bob"))
            .get(Values.iri(getApi(SCHEMA_NAME) + "Patients/column/diseases"));

    assertEquals(expectedSemantic, actualSemantic);
    assertEquals(expectedNonSemantic, actualNonSemantic);
  }

  @Test
  void testCrossSchemaDataTableOntologyColumnValue() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(ontologyCrossSchemaTest, "Patients");

    Set<Value> expectedSemantic =
        Set.of(
            Values.iri("https://icd.who.int/browse10/2019/en#/U07"),
            Values.iri(
                getApi(SCHEMA_NAME)
                    + "Diseases/name=C00-C14%20Malignant%20neoplasms%20of%20lip%2C%20oral%20cavity%20and%20pharynx"));
    Set<Value> expectedNonSemantic =
        Set.of(
            Values.iri(getApi(SCHEMA_NAME) + "Diseases/name=U07"),
            Values.iri(
                getApi(SCHEMA_NAME)
                    + "Diseases/name=C00-C14%20Malignant%20neoplasms%20of%20lip%2C%20oral%20cavity%20and%20pharynx"));

    Set<Value> actualSemantic =
        handler
            .resources
            .get(Values.iri(getApi(CROSS_SCHEMA_NAME) + "Patients/name=pim"))
            .get(Values.iri("http://purl.obolibrary.org/obo/NCIT_C2991"));
    Set<Value> actualNonSemantic =
        handler
            .resources
            .get(Values.iri(getApi(CROSS_SCHEMA_NAME) + "Patients/name=pim"))
            .get(Values.iri(getApi(CROSS_SCHEMA_NAME) + "Patients/column/diseases"));

    assertEquals(expectedSemantic, actualSemantic);
    assertEquals(expectedNonSemantic, actualNonSemantic);
  }
}
