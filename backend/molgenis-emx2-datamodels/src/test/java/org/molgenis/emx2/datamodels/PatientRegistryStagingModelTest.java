package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatientRegistryStagingModelTest {

  private SchemaMetadata staging;
  private SchemaMetadata registry;

  @BeforeAll
  void createModels() {
    staging = new SchemaFromProfile("_profiles/PatientRegistryStaging.yaml").create();
    registry = new SchemaFromProfile("_profiles/PatientRegistry.yaml").create();
  }

  @ParameterizedTest
  @CsvSource({
    "Materials, submitter",
    "Samples RNA, sample collection method",
    "Experiments, sample"
  })
  void columnIsRequiredInStagingOnly(String tableName, String columnName) {
    assertTrue(column(staging, tableName, columnName).isRequired());
    assertFalse(column(registry, tableName, columnName).isRequired());
  }

  @Test
  void rnaSequencingCentreIsFreeText() {
    assertEquals(
        ColumnType.STRING, column(staging, "Experiments RNA", "sequencing centre").getColumnType());
    assertEquals(
        ColumnType.STRING,
        column(registry, "Experiments RNA", "sequencing centre").getColumnType());
  }

  @Test
  void otherSequencingCentresStillReferToOrganisations() {
    for (String tableName : new String[] {"Experiments lrGS", "Experiments srDNA"}) {
      assertEquals(
          "Organisations", column(staging, tableName, "sequencing centre").getRefTableName());
    }
  }

  @Test
  void sampleTypeIsCheckedForRnaSamplesInBothProfiles() {
    for (SchemaMetadata model : new SchemaMetadata[] {staging, registry}) {
      String validation = column(model, "Samples", "sample type").getValidation();
      assertNotNull(validation);
      assertTrue(validation.contains("mg_tableclass?.endsWith('.Samples RNA')"), validation);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Samples RNA, extraction kit, Extraction kits",
    "Experiments RNA, library preparation kit manufacturer, Library prep kit manufacturers",
    "Experiments RNA, library preparation kit, Library preparation kits",
    "Experiments RNA, enrichment kit manufacturer, Enrichment kit manufacturers",
    "Experiments RNA, target enrichment kit, Sequencing enrichment kits"
  })
  void rnaKitColumnsReferToCatalogueOntologies(
      String tableName, String columnName, String ontologyTable) {
    for (SchemaMetadata model : new SchemaMetadata[] {staging, registry}) {
      Column column = column(model, tableName, columnName);
      assertEquals(ColumnType.ONTOLOGY, column.getColumnType());
      assertEquals("CatalogueOntologies", column.getRefSchemaName());
      assertEquals(ontologyTable, column.getRefTableName());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Samples lrGS, extraction kit",
    "Samples srDNA, extraction kit",
    "Experiments lrGS, library preparation kit manufacturer",
    "Experiments srDNA, library preparation kit manufacturer",
    "Experiments lrGS, library preparation kit",
    "Experiments srDNA, library preparation kit"
  })
  void dnaKitColumnsStayFreeTextAndRequiredInStaging(String tableName, String columnName) {
    assertEquals(ColumnType.STRING, column(registry, tableName, columnName).getColumnType());
    assertEquals(ColumnType.STRING, column(staging, tableName, columnName).getColumnType());
    assertTrue(column(staging, tableName, columnName).isRequired());
  }

  @Test
  void libraryKitManufacturerIsRenamed() {
    for (String tableName :
        new String[] {"Experiments RNA", "Experiments lrGS", "Experiments srDNA"}) {
      assertNull(registry.getTableMetadata(tableName).getLocalColumn("library kit manufacturer"));
    }
  }

  @Test
  void catalogueOntologyTableNamesAreValid() {
    for (SchemaMetadata model : new SchemaMetadata[] {staging, registry}) {
      for (TableMetadata table : model.getTables()) {
        for (Column column : table.getLocalColumns()) {
          if ("CatalogueOntologies".equals(column.getRefSchemaName())) {
            String refTable = column.getRefTableName();
            assertDoesNotThrow(
                () -> new TableMetadata(refTable), table.getTableName() + "." + column.getName());
          }
        }
      }
    }
  }

  private static Column column(SchemaMetadata model, String tableName, String columnName) {
    Column column = model.getTableMetadata(tableName).getLocalColumn(columnName);
    assertNotNull(column, tableName + "." + columnName);
    return column;
  }
}
