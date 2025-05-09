package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.rdf.SHACLComplianceTester.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Run RDF export of Data Catalogue to selected file and check compliance against one or more sets
 * of SHACL files. This tool is intended to make development easier by storing the intermediate RDF
 * export for manual inspection.
 */
public class RunRDFExportAndCheckCompliance {

  /**
   * Set performFreshRDFExport to 'true' to create a fresh RDF export of Data Catalogue at the
   * location 'rdfExportLoc', may take a few minutes.
   */
  public static void main(String[] args) throws Exception {
    boolean performFreshRDFExport = false;
    String rdfExportLoc = System.getProperty("user.home") + File.separator + "DataCatRDF.ttl";
    new RunRDFExportAndCheckCompliance().run(performFreshRDFExport, rdfExportLoc);
  }

  /**
   * Step 1: create new EMX2 database, fill with Data Catalogue, and run RDF export. Step 2: read
   * the export back into memory. Step 3: check compliance of RDF against one or more sets of SHACL
   * files. If no violations are detected, the tool exits with status 0. If violations are detected,
   * they will be printed to console and exit status will be 1.
   */
  public void run(boolean performFreshRDFExport, String rdfExportLoc) throws Exception {

    // Create new Data Catalogue and export to RDF
    if (performFreshRDFExport) {
      String rdf =
          ComplianceTest.createSchemaExportRDF("catalogue", "_profiles/DataCatalogue.yaml");
      try (PrintWriter out = new PrintWriter(rdfExportLoc)) {
        out.println(rdf);
      }
    }

    // Read the exported RDF back in and validate against
    String catalogueSchemaRDF = Files.readString(Paths.get(rdfExportLoc), Charset.defaultCharset());

    // Test compliance against one or more sets of SHACL files
    testShaclCompliance(FAIR_DATA_POINT_SHACL_FILES, catalogueSchemaRDF);
    // testShaclCompliance(DCAT_AP_SHACL_FILES, catalogueSchemaRDF);
    testShaclCompliance(HEALTH_RI_V1_SHACL_FILES, catalogueSchemaRDF);
    testShaclCompliance(HEALTH_RI_V2_SHACL_FILES, catalogueSchemaRDF);
    testShaclCompliance(EJP_RD_VP_SHACL_FILES, catalogueSchemaRDF);
  }
}
