package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class FAIRDataHubLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    // create Beacon v2 + FAIR Data Point schema (which will create tables in ontology schema)
    createSchema(schema, "fairdatahub/beaconv2/molgenis.csv");
    createSchema(schema, "fairdatahub/fairdatapoint/molgenis.csv");
    createSchema(schema, "fairdatahub/addons/molgenis.csv");

    // load ontologies
    MolgenisIO.fromClasspathDirectory("fairdatahub/ontologies", schema, false);

    // apply ontology semantics
    createSchema(schema, "fairdatahub/ontologies/SEMANTICS.csv");

    // optionally, load demo data
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("fairdatahub/beaconv2/demodata", schema, false);
      MolgenisIO.fromClasspathDirectory("fairdatahub/fairdatapoint/demodata", schema, false);
      MolgenisIO.fromClasspathDirectory("fairdatahub/addons/demodata", schema, false);
    }
  }
}
