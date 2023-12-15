package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.MolgenisIO;

public class ProfileLoader extends AbstractDataLoader {

  // the classpath location of your config YAML file
  private String configLocation;

  public ProfileLoader(String configLocation) {
    this.configLocation = configLocation;
  }

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    // first generate schema
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(this.configLocation);
    SchemaMetadata schemaMetadata = schemaFromProfile.create();
    Profiles profiles = schemaFromProfile.getProfiles();

    // special option: fixed schema for data loading, and view permission
    Schema fixedSchema = null;
    if (profiles.dataToFixedSchema != null) {
      fixedSchema = createSchema(profiles.dataToFixedSchema, schema.getDatabase());
      if (profiles.setViewPermission != null) {
        fixedSchema.addMember(profiles.setViewPermission, Privileges.VIEWER.toString());
      }
    }

    // load schema (may depend on the data schema with e.g. separate ontologies)
    schema.migrate(schemaMetadata);

    // load any required data associated to template
    for (String data : profiles.dataList) {
      MolgenisIO.fromClasspathDirectory(
          data, profiles.dataToFixedSchema == null ? schema : fixedSchema, false);
    }

    // special option: provide specified user/role with View permissions on created schemas
    if (profiles.setViewPermission != null) {
      schema.addMember(profiles.setViewPermission, Privileges.VIEWER.toString());
    }

    // optionally, load demo data
    if (includeDemoData) {
      for (String example : profiles.examplesList) {
        MolgenisIO.fromClasspathDirectory(example, schema, false);
      }
    }
  }

  /**
   * Helper to check if schema exists and if not create it
   *
   * @param schema
   * @param db
   * @return
   */
  public Schema createSchema(String schema, Database db) {
    Schema createSchema = db.getSchema(schema);
    if (createSchema == null) {
      createSchema = db.createSchema(schema);
    }
    return createSchema;
  }
}
