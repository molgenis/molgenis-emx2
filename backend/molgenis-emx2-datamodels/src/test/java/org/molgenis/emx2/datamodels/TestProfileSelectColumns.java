package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;

public class TestProfileSelectColumns {

  @Test
  void testSelectAllColumnsByTableTag() {
    Profiles profiles = new Profiles();
    profiles.setProfileTagsList(Arrays.asList("FAIR Genomes metabolomics add-on"));
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(profiles);
    SchemaMetadata schemaMetadata = schemaFromProfile.create();
    assertEquals(3, schemaMetadata.getTables().size());
    List<String> colNames =
        schemaMetadata.getTableMetadata("MetabolomicMaterialProcessing").getColumnNames();

    assertEquals(8, colNames.size());
    assertTrue(colNames.contains("SampleExtraction"));
  }

  @Test
  void testSelectByMultipleProfileTags() {
    Profiles profiles = new Profiles();
    profiles.setProfileTagsList(Arrays.asList("FAIR Genomes", "FAIR Genomes metabolomics add-on"));
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(profiles);
    SchemaMetadata schemaMetadata = schemaFromProfile.create();
    assertEquals(12, schemaMetadata.getTables().size());
    List<String> colNames1 =
        schemaMetadata.getTableMetadata("MetabolomicMaterialProcessing").getColumnNames();
    assertEquals(8, colNames1.size());
    assertTrue(colNames1.contains("SampleExtraction"));
    List<String> colNames2 =
        schemaMetadata.getTableMetadata("LeafletAndConsentForm").getColumnNames();
    assertEquals(9, colNames2.size());
    assertTrue(colNames2.contains("ConsentFormValidUntil"));
  }

  @Test
  void testSelectSpecificColumnsByTag() {
    Profiles RD3 = new Profiles();
    RD3.setProfileTagsList(Arrays.asList("RD3"));
    SchemaFromProfile RD3Schema = new SchemaFromProfile(RD3);
    SchemaMetadata RD3SchemaMetadata = RD3Schema.create();
    assertEquals(11, RD3SchemaMetadata.getTables().size());
    List<String> RD3FileColumns = RD3SchemaMetadata.getTableMetadata("Files").getColumnNames();
    assertEquals(16, RD3FileColumns.size());

    Profiles dcatFiles = new Profiles();
    dcatFiles.setProfileTagsList(Arrays.asList("DCAT files add-on"));
    SchemaFromProfile dcatFilesSchema = new SchemaFromProfile(dcatFiles);
    SchemaMetadata dcatFilesSchemaMetadata = dcatFilesSchema.create();
    assertEquals(1, dcatFilesSchemaMetadata.getTables().size());
    List<String> dcatFilesColumns =
        dcatFilesSchemaMetadata.getTableMetadata("Files").getColumnNames();
    assertEquals(6, dcatFilesColumns.size());

    Profiles beaconAddOn = new Profiles();
    beaconAddOn.setProfileTagsList(Arrays.asList("Beacon v2 EMX2 add-on"));
    SchemaFromProfile beaconAddOnSchema = new SchemaFromProfile(beaconAddOn);
    SchemaMetadata beaconAddOnSchemaMetadata = beaconAddOnSchema.create();
    assertEquals(4, beaconAddOnSchemaMetadata.getTables().size());
    List<String> BeaconFileColumns =
        beaconAddOnSchemaMetadata.getTableMetadata("Files").getColumnNames();
    assertEquals(6, BeaconFileColumns.size());
  }
}
