package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.rdf.ComplianceTest.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class FDPComplianceTest {

  private static String fdpSchemaRDF;

  @BeforeAll
  public static void setup() {
    fdpSchemaRDF =
        CreateSchemaExportRDF("fairdatapoint_shacl_validation", "_profiles/FAIRDataPoint.yaml");
  }

  @Test
  public void FAIRDataPoint_Compliance() throws Exception {
    TestCompliance(FAIR_DATA_POINT_SHACL_FILES, fdpSchemaRDF);
  }

  @Test
  public void DCAT_AP_Compliance() throws Exception {
    TestCompliance(DCAT_AP_SHACL_FILES, fdpSchemaRDF);
  }

  @Test
  public void Health_RI_core_plateau_1_Compliance() throws Exception {
    TestCompliance(HEALTH_RI_SHACL_FILES, fdpSchemaRDF);
  }

  @Test
  public void EJP_RD_VP_Lvl1_Compliance() throws Exception {
    TestCompliance(EJP_RD_VP_SHACL_FILES, fdpSchemaRDF);
  }
}
