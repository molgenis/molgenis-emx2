package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.rdf.ComplianceTest.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class CatalogueComplianceTest {

  private static String catalogueSchemaRDF;

  @BeforeAll
  public static void setup() {
    catalogueSchemaRDF =
        createSchemaExportRDF("catalogue_shacl_validation", "_profiles/DataCatalogueFlat.yaml");
  }

  @Test
  public void FAIRDataPoint_Compliance() throws Exception {
    testCompliance(FAIR_DATA_POINT_SHACL_FILES, catalogueSchemaRDF);
  }

  @Test
  public void DCAT_AP_Compliance() throws Exception {
    testCompliance(DCAT_AP_SHACL_FILES, catalogueSchemaRDF);
  }

  @Test
  public void Health_RI_core_plateau_1_Compliance() throws Exception {
    testCompliance(HEALTH_RI_SHACL_FILES, catalogueSchemaRDF);
  }

  @Test
  public void EJP_RD_VP_Lvl1_Compliance() throws Exception {
    testCompliance(EJP_RD_VP_SHACL_FILES, catalogueSchemaRDF);
  }
}
