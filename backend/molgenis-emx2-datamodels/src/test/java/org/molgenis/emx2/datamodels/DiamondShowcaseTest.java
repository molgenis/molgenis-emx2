package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.SchemaLoaderSettings;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiamondShowcaseTest {

  private static final String SCHEMA_NAME = "DiamondShowcaseTest";

  private static final String SUBJECT = "Subject";
  private static final String COCKAYNE_SYNDROME = "CockayneSyndrome";
  private static final String XERODERMA_PIGMENTOSUM = "XerodermaPigmentosum";
  private static final String TRICHOTHIODYSTROPHY = "Trichothiodystrophy";
  private static final String ADVANCED_COCKAYNE = "AdvancedCockayne";
  private static final String EPIDERMOLYSIS_BULLOSA = "EpidermolysisBullosa";
  private static final String CLINICAL_SUBJECT = "ClinicalSubject";
  private static final String RESEARCH_SUBJECT = "ResearchSubject";
  private static final String CLINICAL_RESEARCH_SUBJECT = "ClinicalResearchSubject";

  private static final String SUBGROUPS01 = "subgroups01";
  private static final String DISEASE_GROUP = "diseaseGroup";

  private static Schema schema;

  @BeforeAll
  void loadSchema() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    SchemaLoaderSettings settings =
        new SchemaLoaderSettings(database, SCHEMA_NAME, "Diamond showcase smoke test", true);
    new DiamondShowcaseLoader(settings).run();
    schema = database.getSchema(SCHEMA_NAME);
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
  void moduleTablesExist() {
    for (String moduleName :
        List.of(
            COCKAYNE_SYNDROME,
            XERODERMA_PIGMENTOSUM,
            TRICHOTHIODYSTROPHY,
            ADVANCED_COCKAYNE,
            EPIDERMOLYSIS_BULLOSA)) {
      Table table = schema.getTable(moduleName);
      assertNotNull(table, moduleName + " MODULE table must exist");
      assertEquals(
          TableType.MODULE,
          table.getMetadata().getTableType(),
          moduleName + " must have tableType=MODULE");
    }
  }

  @Test
  void moduleArrayColumnsOnSubject() {
    TableMetadata subjectMeta = schema.getTable(SUBJECT).getMetadata();

    Column subgroups01Col = subjectMeta.getColumn(SUBGROUPS01);
    assertNotNull(subgroups01Col, "subgroups01 MODULE_ARRAY column must exist on Subject");
    assertEquals(
        ColumnType.MODULE_ARRAY,
        subgroups01Col.getColumnType(),
        "subgroups01 must be MODULE_ARRAY");
    List<String> subgroups = subgroups01Col.getValues();
    assertNotNull(subgroups, "subgroups01 values must not be null");
    assertTrue(subgroups.contains(COCKAYNE_SYNDROME), "subgroups01 must list CockayneSyndrome");
    assertTrue(
        subgroups.contains(XERODERMA_PIGMENTOSUM), "subgroups01 must list XerodermaPigmentosum");
    assertTrue(
        subgroups.contains(TRICHOTHIODYSTROPHY), "subgroups01 must list Trichothiodystrophy");

    Column diseaseGroupCol = subjectMeta.getColumn(DISEASE_GROUP);
    assertNotNull(diseaseGroupCol, "diseaseGroup MODULE_ARRAY column must exist on Subject");
    assertEquals(
        ColumnType.MODULE_ARRAY,
        diseaseGroupCol.getColumnType(),
        "diseaseGroup must be MODULE_ARRAY");
    assertTrue(
        diseaseGroupCol.getValues().contains(EPIDERMOLYSIS_BULLOSA),
        "diseaseGroup must list EpidermolysisBullosa");
  }

  @Test
  void enumColumnsOnSubject() {
    TableMetadata subjectMeta = schema.getTable(SUBJECT).getMetadata();

    Column sexCol = subjectMeta.getColumn("sex");
    assertNotNull(sexCol, "sex ENUM column must exist on Subject");
    assertEquals(ColumnType.ENUM, sexCol.getColumnType(), "sex must be ENUM");
    assertTrue(sexCol.getValues().contains("male"), "sex values must contain 'male'");

    Column tagsCol = subjectMeta.getColumn("tags");
    assertNotNull(tagsCol, "tags ENUM_ARRAY column must exist on Subject");
    assertEquals(ColumnType.ENUM_ARRAY, tagsCol.getColumnType(), "tags must be ENUM_ARRAY");
  }

  @Test
  void moduleExtendsModuleChainRooted() {
    TableMetadata advancedMeta = schema.getTable(ADVANCED_COCKAYNE).getMetadata();
    assertTrue(
        advancedMeta.getInheritNames().contains(COCKAYNE_SYNDROME),
        "AdvancedCockayne must extend CockayneSyndrome");
    assertEquals(
        TableType.MODULE, advancedMeta.getTableType(), "AdvancedCockayne must be MODULE type");
    String root = advancedMeta.getRootTable().getTableName();
    assertEquals(SUBJECT, root, "AdvancedCockayne must root at Subject");
  }

  @Test
  void diamondChildHasSingleRoot() {
    TableMetadata clinResearchMeta = schema.getTable(CLINICAL_RESEARCH_SUBJECT).getMetadata();
    List<String> parents = clinResearchMeta.getInheritNames();
    assertTrue(parents.contains(CLINICAL_SUBJECT), "diamond child must extend ClinicalSubject");
    assertTrue(parents.contains(RESEARCH_SUBJECT), "diamond child must extend ResearchSubject");

    String root = clinResearchMeta.getRootTable().getTableName();
    assertEquals(SUBJECT, root, "diamond ClinicalResearchSubject must root at Subject");
  }

  @Test
  void activeModuleColumnProjectsNonNull() {
    List<Row> rows =
        schema
            .getTable(SUBJECT)
            .query()
            .select(s("subjectId"), s(SUBGROUPS01), s("relevantmedhistory"))
            .where(f("subjectId", EQUALS, "SUBJ001"))
            .retrieveRows();

    assertEquals(1, rows.size(), "SUBJ001 must be found");
    Row subj001 = rows.get(0);
    assertNotNull(
        subj001.getString("relevantmedhistory"),
        "active CS module column relevantmedhistory must project non-null for SUBJ001");
  }

  @Test
  void inactiveModuleColumnProjectsNull() {
    List<Row> rows =
        schema
            .getTable(SUBJECT)
            .query()
            .select(s("subjectId"), s(SUBGROUPS01), s("skinSymptoms"))
            .where(f("subjectId", EQUALS, "SUBJ001"))
            .retrieveRows();

    assertEquals(1, rows.size(), "SUBJ001 must be found");
    Row subj001 = rows.get(0);
    assertNull(
        subj001.getString("skinSymptoms"),
        "inactive XP module column skinSymptoms must project null for SUBJ001 (CS-only row)");
  }

  @Test
  void demoDataLoadedWithExpectedRowCount() {
    List<Row> subjectRows = schema.getTable(SUBJECT).retrieveRows();
    assertFalse(subjectRows.isEmpty(), "Subject table must have demo rows loaded");
    assertTrue(subjectRows.size() >= 7, "At least 7 Subject demo rows expected");
  }
}
