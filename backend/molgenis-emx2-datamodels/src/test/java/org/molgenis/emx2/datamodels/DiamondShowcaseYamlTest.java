package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.SchemaLoaderSettings;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiamondShowcaseYamlTest {

  private static final String SCHEMA_NAME = "DiamondShowcaseYamlTest";
  private static final String COMPANION_SCHEMA_NAME = "diamontologies";

  private static final String SUBJECT = "Subject";
  private static final String CLINICAL_SUBJECT = "ClinicalSubject";
  private static final String RESEARCH_SUBJECT = "ResearchSubject";
  private static final String CLINICAL_RESEARCH_SUBJECT = "ClinicalResearchSubject";

  private static final String DISEASE_CATEGORIES = "Disease categories";
  private static final String ASSAY_CATEGORIES = "Assay categories";

  private static Database database;
  private static Schema schema;
  private static Schema companionSchema;

  @BeforeAll
  void loadSchema() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    database.dropSchemaIfExists(COMPANION_SCHEMA_NAME);
    SchemaLoaderSettings settings =
        new SchemaLoaderSettings(database, SCHEMA_NAME, "Diamond showcase (YAML) smoke test", true);
    new DiamondShowcaseYamlLoader(settings).run();
    schema = database.getSchema(SCHEMA_NAME);
    companionSchema = database.getSchema(COMPANION_SCHEMA_NAME);
  }

  @Test
  void subjectTableIsCreated() {
    assertNotNull(schema.getTable(SUBJECT), "Subject table must exist");
    assertEquals(
        TableType.DATA,
        schema.getTable(SUBJECT).getMetadata().getTableType(),
        "Subject must be DATA type");
  }

  @Test
  void diamondChildHasBothParentsAndRootsAtSubject() {
    TableMetadata clinResearchMeta = schema.getTable(CLINICAL_RESEARCH_SUBJECT).getMetadata();
    List<String> parents = clinResearchMeta.getInheritNames();
    assertTrue(parents.contains(CLINICAL_SUBJECT), "diamond child must extend ClinicalSubject");
    assertTrue(parents.contains(RESEARCH_SUBJECT), "diamond child must extend ResearchSubject");

    String root = clinResearchMeta.getRootTable().getTableName();
    assertEquals(SUBJECT, root, "diamond ClinicalResearchSubject must root at Subject");
  }

  @Test
  void companionSchemaIsProvisionedWithOntologyTables() {
    assertNotNull(companionSchema, "companion schema 'diamontologies' must be provisioned");
    Table diseaseCategories = companionSchema.getTable(DISEASE_CATEGORIES);
    Table assayCategories = companionSchema.getTable(ASSAY_CATEGORIES);
    assertNotNull(diseaseCategories, "Disease categories ontology table must exist in companion");
    assertNotNull(assayCategories, "Assay categories ontology table must exist in companion");
    assertEquals(
        TableType.ONTOLOGIES,
        diseaseCategories.getMetadata().getTableType(),
        "Disease categories must have tableType=ONTOLOGIES");
  }

  @Test
  void diseaseCategoryColumnHasDottedCrossSchemaRef() {
    Column diseaseCategoryCol = schema.getTable(SUBJECT).getMetadata().getColumn("diseaseCategory");
    assertNotNull(diseaseCategoryCol, "diseaseCategory column must exist on Subject");
    assertEquals(
        ColumnType.ONTOLOGY_ARRAY,
        diseaseCategoryCol.getColumnType(),
        "diseaseCategory must be ONTOLOGY_ARRAY");
    assertEquals(
        COMPANION_SCHEMA_NAME,
        diseaseCategoryCol.getRefSchemaName(),
        "diseaseCategory must reference the companion schema by name");
    assertEquals(
        DISEASE_CATEGORIES,
        diseaseCategoryCol.getRefTableName(),
        "diseaseCategory must reference the Disease categories companion table");
  }

  @Test
  void assayCategoryColumnHasDottedCrossSchemaRef() {
    Column assayCategoryCol =
        schema.getTable(CLINICAL_RESEARCH_SUBJECT).getMetadata().getColumn("assayCategory");
    assertNotNull(assayCategoryCol, "assayCategory column must exist on ClinicalResearchSubject");
    assertEquals(
        ColumnType.ONTOLOGY_ARRAY,
        assayCategoryCol.getColumnType(),
        "assayCategory must be ONTOLOGY_ARRAY");
    assertEquals(
        COMPANION_SCHEMA_NAME,
        assayCategoryCol.getRefSchemaName(),
        "assayCategory must reference the companion schema by name");
    assertEquals(
        ASSAY_CATEGORIES,
        assayCategoryCol.getRefTableName(),
        "assayCategory must reference the Assay categories companion table");
  }

  @Test
  void companionOntologyTermsAreLoadedAsDemoData() {
    List<Row> diseaseRows = companionSchema.getTable(DISEASE_CATEGORIES).retrieveRows();
    assertTrue(diseaseRows.size() >= 4, "at least 4 disease category terms expected");

    List<Row> assayRows = companionSchema.getTable(ASSAY_CATEGORIES).retrieveRows();
    assertTrue(assayRows.size() >= 3, "at least 3 assay category terms expected");
  }

  @Test
  void demoDataLoadedAcrossDiamondTables() {
    List<Row> subjectRows = schema.getTable(SUBJECT).retrieveRows();
    assertEquals(8, subjectRows.size(), "root Subject table must project all 8 diamond demo rows");
  }
}
