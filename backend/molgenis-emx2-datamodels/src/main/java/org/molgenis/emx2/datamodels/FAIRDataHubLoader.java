package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaCsvFromProfile;
import org.molgenis.emx2.io.MolgenisIO;

public class FAIRDataHubLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    // generate and load schema
    SchemaCsvFromProfile schemaCsvFromProfile =
        new SchemaCsvFromProfile("fairdatahub/FAIRDataHub.yaml");
    String generatedSchemaLocation = schemaCsvFromProfile.generate();
    createSchema(schema, generatedSchemaLocation);

    // load any required data associated to template
    Profiles profiles = schemaCsvFromProfile.getProfiles();
    for (String data : profiles.dataList) {
      MolgenisIO.fromClasspathDirectory(data, schema, false);
    }

    // optionally, load demo data
    if (includeDemoData) {
      for (String example : profiles.examplesList) {
        MolgenisIO.fromClasspathDirectory(example, schema, false);
      }
    }
  }
}
