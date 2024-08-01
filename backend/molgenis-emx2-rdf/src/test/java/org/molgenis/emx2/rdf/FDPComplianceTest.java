package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.rdf.RDFTest.RDF_API_LOCATION;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class FDPComplianceTest {

  private static String fdpSchemaRDF;

  @BeforeAll
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema fdpSchema = database.dropCreateSchema("fairdatapoint_shacl_validation");
    ProfileLoader fdpLoader = new ProfileLoader("_profiles/FAIRDataPoint.yaml");
    fdpLoader.load(fdpSchema, true);
    OutputStream outputStream = new ByteArrayOutputStream();
    var rdf = new RDFService("http://localhost:8080", RDF_API_LOCATION, null);
    rdf.describeAsRDF(outputStream, null, null, null, fdpSchema);
    fdpSchemaRDF = outputStream.toString();
  }

  /** Source files and information: https://specs.fairdatapoint.org/fdp-specs-v1.2.html */
  @Test
  public void FAIRDataPoint_Compliance() throws Exception {
    SHACLValidator sv = new SHACLValidator();
    sv.addValidateShapesFromFile("SHACL/FAIR_Data_Point/v1.2/FAIRDataPointShape.ttl");
    sv.addValidateShapesFromFile("SHACL/FAIR_Data_Point/v1.2/CatalogShape.ttl");
    sv.addValidateDataFromString(fdpSchemaRDF);
    sv.clearAll();
  }

  /**
   * Source files and information:
   * https://semiceu.github.io/DCAT-AP/releases/3.0.0/#validation-of-dcat-ap
   */
  @Test
  public void DCAT_AP_Compliance() throws Exception {
    SHACLValidator sv = new SHACLValidator();
    sv.addValidateShapesFromFile("SHACL/DCAT-AP/v3.0.0/shapes.ttl");
    sv.addValidateShapesFromFile("SHACL/DCAT-AP/v3.0.0/range.ttl");
    sv.addValidateShapesFromFile("SHACL/DCAT-AP/v3.0.0/shapes_recommended.ttl");
    sv.addValidateShapesFromFile("SHACL/DCAT-AP/v3.0.0/imports.ttl");
    sv.addValidateShapesFromFile("SHACL/DCAT-AP/v3.0.0/mdr_imports.ttl");
    sv.addValidateShapesFromFile("SHACL/DCAT-AP/v3.0.0/mdr-vocabularies.shape.ttl");
    sv.addValidateDataFromString(fdpSchemaRDF);
    sv.clearAll();
  }

  /**
   * Information: https://www.health-ri.nl/health-ri-roadmap-plateauplanning Source files:
   * https://github.com/Health-RI/metadata-shacl-validation/blob/master/validator/resources/healthri/config.properties
   */
  @Test
  public void Health_RI_core_plateau_1_Compliance() throws Exception {
    SHACLValidator sv = new SHACLValidator();
    sv.addValidateShapesFromFile("SHACL/Health-RI_core_plateau_1/v1.0.0/Catalog.ttl");
    sv.addValidateShapesFromFile("SHACL/Health-RI_core_plateau_1/v1.0.0/DataService.ttl");
    sv.addValidateShapesFromFile("SHACL/Health-RI_core_plateau_1/v1.0.0/Dataset.ttl");
    sv.addValidateShapesFromFile("SHACL/Health-RI_core_plateau_1/v1.0.0/DatasetSeries.ttl");
    sv.addValidateShapesFromFile("SHACL/Health-RI_core_plateau_1/v1.0.0/Distribution.ttl");
    sv.addValidateShapesFromFile("SHACL/Health-RI_core_plateau_1/v1.0.0/Resource.ttl");
    sv.addValidateDataFromString(fdpSchemaRDF);
    sv.clearAll();
  }

  /**
   * Information: https://vp-onboarding-doc.readthedocs.io/en/latest/level_1/index.html Source
   * files: https://github.com/ejp-rd-vp/FDP-Reference-Implementation-Configuration/tree/main/shacl
   */
  @Test
  public void EJP_RD_VP_Lvl1_Compliance() throws Exception {
    SHACLValidator sv = new SHACLValidator();
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/biobank.shacl");
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/catalog.shacl");
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/data-service.shacl");
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/dataset.shacl");
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/distribution.shacl");
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/guideline.shacl");
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/patient-registry.shacl");
    sv.addValidateShapesFromFile("SHACL/EJP_RD_VP_Level_1/01-08-2024/resource.shacl");
    sv.addValidateDataFromString(fdpSchemaRDF);
    sv.clearAll();
  }
}
