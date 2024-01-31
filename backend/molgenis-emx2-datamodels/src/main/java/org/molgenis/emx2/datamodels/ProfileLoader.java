package org.molgenis.emx2.datamodels;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.profiles.CreateSchemas;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class ProfileLoader extends AbstractDataLoader {

  private static final String ONTOLOGY_LOCATION = "/_ontologies";
  private static final String ONTOLOGY_SEMANTICS_LOCATION = ONTOLOGY_LOCATION + "/_semantics.csv";

  // the classpath location of your config (a.k.a. 'profile') YAML file
  private final String configLocation;

  public ProfileLoader(String configLocation) {
    this.configLocation = configLocation;
  }

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {

    // load config (a.k.a. 'profile') YAML file
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(this.configLocation);
    Profiles profiles = schemaFromProfile.getProfiles();

    // special option: if there are createSchemasIfMissing, import those first
    if (profiles.getFirstCreateSchemasIfMissing() != null) {
      for (CreateSchemas createSchemasIfMissing : profiles.getFirstCreateSchemasIfMissing()) {
        String schemaName = createSchemasIfMissing.getName();
        Database db = schema.getDatabase();
        Schema createNewSchema = db.getSchema(schemaName);
        // if schema exists by this name, stop and continue with next
        if (createNewSchema != null) {
          continue;
        }
        createNewSchema = db.createSchema(schemaName);
        String profileLocation = createSchemasIfMissing.getProfile();
        ProfileLoader profileLoader = new ProfileLoader(profileLocation);
        profileLoader.load(createNewSchema, createSchemasIfMissing.isImportDemoData());
      }
    }

    // create the schema using the selected profile tags within the big model
    SchemaMetadata schemaMetadata = schemaFromProfile.create();

    // special option: fixed schema import location for ontologies (not schema or data)
    Schema ontoSchema;
    if (profiles.getOntologiesToFixedSchema() != null) {
      ontoSchema = createSchema(profiles.getOntologiesToFixedSchema(), schema.getDatabase());
      if (profiles.getSetViewPermission() != null) {
        ontoSchema.addMember(profiles.getSetViewPermission(), Privileges.VIEWER.toString());
      }
      if (profiles.getSetEditPermission() != null) {
        ontoSchema.addMember(profiles.getSetEditPermission(), Privileges.EDITOR.toString());
      }
    } else {
      ontoSchema = schema;
    }

    // import the schema
    schema.migrate(schemaMetadata);

    // import ontologies (not schema or data)
    MolgenisIO.fromClasspathDirectory(ONTOLOGY_LOCATION, ontoSchema, false);

    // special options: provide specific user/role with View/Edit permissions on imported schema
    if (profiles.getSetViewPermission() != null) {
      schema.addMember(profiles.getSetViewPermission(), Privileges.VIEWER.toString());
    }
    if (profiles.getSetEditPermission() != null) {
      schema.addMember(profiles.getSetEditPermission(), Privileges.EDITOR.toString());
    }

    // optionally, load demo data (i.e. some example records, or specific application data)
    if (includeDemoData) {
      // prevent data tables with ontology table names to be imported into ontologies by accident
      String[] includeTableNames = getTypeOfTablesToInclude(schema);
      for (String example : profiles.getDemoDataList()) {
        MolgenisIO.fromClasspathDirectory(example, schema, false, includeTableNames);
      }
    }

    // load schema settings from dir containing e.g. molgenis_settings.csv or molgenis_members.csv
    for (String setting : profiles.getSettingsList()) {
      MolgenisIO.fromClasspathDirectory(setting, schema, false);
    }

    // lastly, apply any ontology table semantics from a predefined location
    // this requires special parsing, because we must only update ontology tables used in the schema
    // to prevent adding additional unused tables
    SchemaMetadata ontologySemantics = getOntologySemantics(ontoSchema);
    ontoSchema.migrate(ontologySemantics);
  }

  /** Helper function to get a string array of data table names from a schema */
  private String[] getTypeOfTablesToInclude(Schema schema) {
    List<String> tablesToUpdate = new ArrayList<>();
    for (TableMetadata tableMetadata : schema.getMetadata().getTables()) {
      if (tableMetadata.getTableType().equals(TableType.DATA)) {
        tablesToUpdate.add(tableMetadata.getTableName());
      }
    }
    String[] tablesToUpdateArr = new String[tablesToUpdate.size()];
    tablesToUpdate.toArray(tablesToUpdateArr);
    return tablesToUpdateArr;
  }

  /**
   * Get potential updates regarding the semantics of ontology tables used in the imported schema
   *
   * @return SchemaMetadata
   */
  private SchemaMetadata getOntologySemantics(Schema schema) {
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
    return Emx2.fromRowList(keepRows);
  }

  /** Helper to check if schema exists and if not create it */
  private Schema createSchema(String schema, Database db) {
    Schema createSchema = db.getSchema(schema);
    if (createSchema == null) {
      createSchema = db.createSchema(schema);
    }
    return createSchema;
  }
}
