package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.rdf.SHACLComplianceTester.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.rdf.SHACLValidator;

@Disabled
public class SHACLIntegrationTest extends ComplianceTest {

  /**
   * Combine all SHACL shapes into one to find out if the superset is also a valid shape. If this is
   * the case, we only have to create and maintain one example dataset because there are no mutually
   * exclusive constraints.
   */
  @Test
  public void CombineAndValidateAllSHACLFiles() throws Exception {
    List<String> allSHACLFilesList = new ArrayList<>();
    Collections.addAll(allSHACLFilesList, FAIR_DATA_POINT_SHACL_FILES);
    Collections.addAll(allSHACLFilesList, DCAT_AP_SHACL_FILES);
    Collections.addAll(allSHACLFilesList, HEALTH_RI_SHACL_FILES);
    Collections.addAll(allSHACLFilesList, EJP_RD_VP_SHACL_FILES);
    String[] allSHACLFilesArray = new String[allSHACLFilesList.size()];
    for (int i = 0; i < allSHACLFilesList.size(); i++)
      allSHACLFilesArray[i] = allSHACLFilesList.get(i);
    SHACLValidator sv = new SHACLValidator();
    for (String SHACLFile : allSHACLFilesArray) {
      sv.addValidateShapesFromFile(SHACLFile);
    }
  }
}
