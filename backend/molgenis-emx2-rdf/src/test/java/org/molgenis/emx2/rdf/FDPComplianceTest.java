package org.molgenis.emx2.rdf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class FDPComplianceTest extends ComplianceTest {

  private static String fdpSchemaRDF;

  @BeforeAll
  public static void setup() {
    fdpSchemaRDF =
        createSchemaExportRDF("fairdatapoint_shacl_validation", "_profiles/FAIRDataPoint.yaml");
  }

  @Test
  public void FAIRDataPoint_Compliance() throws Exception {
    testCompliance(FAIR_DATA_POINT_SHACL_FILES, fdpSchemaRDF);
  }

  @Test
  public void DCAT_AP_Compliance() throws Exception {
    testCompliance(DCAT_AP_SHACL_FILES, fdpSchemaRDF);
  }

  @Test
  public void Health_RI_core_plateau_1_Compliance() throws Exception {
    testCompliance(HEALTH_RI_SHACL_FILES, fdpSchemaRDF);
  }

  @Test
  public void EJP_RD_VP_Lvl1_Compliance() throws Exception {
    testCompliance(EJP_RD_VP_SHACL_FILES, fdpSchemaRDF);
  }
}
