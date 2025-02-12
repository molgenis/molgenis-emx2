package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.rdf.RDFTest.RDF_API_LOCATION;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/**
 * Not a test itself, but offers to component to quickly build specific tests such as
 * CatalogueComplianceTest and FDPComplianceTest
 */
public abstract class ComplianceTest {

  /**
   * Create a schema according to a profile and return its exported RDF
   *
   * @param schemaName
   * @param profile
   * @return
   */
  public static String createSchemaExportRDF(String schemaName, String profile) {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(schemaName);
    new ImportProfileTask(schema, profile, true).run();
    OutputStream outputStream = new ByteArrayOutputStream();
    var rdf = new RDFService("http://localhost:8080", RDF_API_LOCATION, null);
    rdf.describeAsRDF(outputStream, null, null, null, schema);
    return outputStream.toString();
  }

  /**
   * Reusable test for compliance of RDF data to a set of SHACL files
   *
   * @param SHACLFiles
   * @param rdf
   * @throws Exception
   */
  public static void testCompliance(String[] SHACLFiles, String rdf) throws Exception {
    SHACLValidator sv = new SHACLValidator();
    for (String SHACLFile : SHACLFiles) {
      sv.addValidateShapesFromFile(SHACLFile);
    }
    sv.addValidateDataFromString(rdf);
    sv.clearAll();
  }

  /** Below SHACL files sets that can be applied to different RDF exports for validation */

  /** Source files and information: https://specs.fairdatapoint.org/fdp-specs-v1.2.html */
  public static final String[] FAIR_DATA_POINT_SHACL_FILES =
      new String[] {
        "SHACL/FAIR_Data_Point/v1.2/CatalogShape.ttl",
        "SHACL/FAIR_Data_Point/v1.2/FAIRDataPointShape.ttl"
      };

  /**
   * Source files and information:
   * https://semiceu.github.io/DCAT-AP/releases/3.0.0/#validation-of-dcat-ap
   */
  public static final String[] DCAT_AP_SHACL_FILES =
      new String[] {
        "SHACL/DCAT-AP/v3.0.0/imports.ttl",
        "SHACL/DCAT-AP/v3.0.0/mdr-vocabularies.shape.ttl",
        "SHACL/DCAT-AP/v3.0.0/mdr_imports.ttl",
        "SHACL/DCAT-AP/v3.0.0/range.ttl",
        "SHACL/DCAT-AP/v3.0.0/shapes.ttl",
        "SHACL/DCAT-AP/v3.0.0/shapes_recommended.ttl"
      };

  /**
   * Information: https://www.health-ri.nl/health-ri-roadmap-plateauplanning Source files:
   * https://github.com/Health-RI/metadata-shacl-validation/blob/master/validator/resources/healthri/config.properties
   */
  public static final String[] HEALTH_RI_SHACL_FILES =
      new String[] {
        "SHACL/Health-RI_core_plateau_1/v1.0.0/Catalog.ttl",
        "SHACL/Health-RI_core_plateau_1/v1.0.0/DataService.ttl",
        "SHACL/Health-RI_core_plateau_1/v1.0.0/Dataset.ttl",
        "SHACL/Health-RI_core_plateau_1/v1.0.0/DatasetSeries.ttl",
        "SHACL/Health-RI_core_plateau_1/v1.0.0/Distribution.ttl",
        "SHACL/Health-RI_core_plateau_1/v1.0.0/Resource.ttl"
      };

  /**
   * Information: https://vp-onboarding-doc.readthedocs.io/en/latest/level_1/index.html Source
   * files: https://github.com/ejp-rd-vp/FDP-Reference-Implementation-Configuration/tree/main/shacl
   */
  public static final String[] EJP_RD_VP_SHACL_FILES =
      new String[] {
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/biobank.shacl",
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/catalog.shacl",
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/data-service.shacl",
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/dataset.shacl",
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/distribution.shacl",
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/guideline.shacl",
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/patient-registry.shacl",
        "SHACL/EJP_RD_VP_Level_1/01-08-2024/resource.shacl"
      };
}
