package org.molgenis.emx2.datamodels;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class ProfileLoader extends AbstractDataLoader {

  private static final String ONTOLOGY_SEMANTICS_LOCATION =
      File.separator + "_ontologies" + File.separator + "_semantics.csv";

  // the classpath location of your config YAML file
  private String configLocation;

  public ProfileLoader(String configLocation) {
    this.configLocation = configLocation;
  }

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    // first create the schema using the selected profiles
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(this.configLocation);
    SchemaMetadata schemaMetadata = schemaFromProfile.create();
    Profiles profiles = schemaFromProfile.getProfiles();

    // special option: fixed schema import location for data/ontologies (not schema or examples!)
    Schema fixedSchema = null;
    if (profiles.dataToFixedSchema != null) {
      fixedSchema = createSchema(profiles.dataToFixedSchema, schema.getDatabase());
      if (profiles.setViewPermission != null) {
        fixedSchema.addMember(profiles.setViewPermission, Privileges.VIEWER.toString());
      }
    }

    // import the schema (may depend on the data schema with e.g. separate ontologies)
    schema.migrate(schemaMetadata);

    // import any required data/ontologies essential for the template (not schema or examples!)
    Schema dataSchema = profiles.dataToFixedSchema == null ? schema : fixedSchema;
    for (String data : profiles.dataList) {
      MolgenisIO.fromClasspathDirectory(data, dataSchema, false);
    }

    // special option: provide specified user/role with View permissions on imported schema
    if (profiles.setViewPermission != null) {
      schema.addMember(profiles.setViewPermission, Privileges.VIEWER.toString());
    }

    // optionally, load examples (i.e. disposable demo records, not essential for the template)
    if (includeDemoData) {
      for (String example : profiles.examplesList) {
        MolgenisIO.fromClasspathDirectory(example, schema, false);
      }
    }

    // lastly, apply any ontology table semantics from a predefined location
    SchemaMetadata ontologySemantics = getOntologySemantics(dataSchema);
    dataSchema.migrate(ontologySemantics);
  }

  /**
   * Get potential updates regarding the semantics of ontology tables used in the imported schema
   *
   * @param schema
   * @return SchemaMetadata
   */
  public SchemaMetadata getOntologySemantics(Schema schema) {
    Set<String> tablesToUpdate = new HashSet<>();
    for (TableMetadata tableMetadata : schema.getMetadata().getTables()) {
      if (tableMetadata.getTableType().equals(TableType.ONTOLOGIES)) {
        tablesToUpdate.add(tableMetadata.getTableName());
      }
    }
    URL dirURL = getClass().getResource(ONTOLOGY_SEMANTICS_LOCATION);
    if (dirURL == null) {
      throw new MolgenisException(
          "Import failed: File " + ONTOLOGY_SEMANTICS_LOCATION + " doesn't exist in classpath");
    }
    InputStreamReader ontologySemanticsISR =
        new InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream(ONTOLOGY_SEMANTICS_LOCATION)));
    List<Row> keepRows = new ArrayList<>();
    for (Row row : CsvTableReader.read(ontologySemanticsISR)) {
      if (tablesToUpdate.contains(row.getString("tableName"))) {
        keepRows.add(row);
      }
    }
    SchemaMetadata tablesToUpdateSchema = Emx2.fromRowList(keepRows);
    return tablesToUpdateSchema;
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
