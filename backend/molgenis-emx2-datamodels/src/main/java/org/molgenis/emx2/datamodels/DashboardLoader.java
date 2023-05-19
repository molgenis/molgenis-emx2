package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class DashboardLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    createSchema(schema, "dashboard/molgenis.csv");
    
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("dashboard/demodata/ontologies", schema, false);
      MolgenisIO.fromClasspathDirectory("dashboard/demodata/datasets", schema, false);
    }
  }
}
