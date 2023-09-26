package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.MolgenisIO;

public class FAIRDataHubLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    // generate and load schema
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile("fairdatahub/FAIRDataHub.yaml");
    SchemaMetadata schemaMetadata = schemaFromProfile.create();
    schema.migrate(schemaMetadata);

    // load any required data associated to template
    Profiles profiles = schemaFromProfile.getProfiles();
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
