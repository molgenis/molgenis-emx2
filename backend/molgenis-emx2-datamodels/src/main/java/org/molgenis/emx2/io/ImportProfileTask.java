package org.molgenis.emx2.io;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.profiles.CreateSchemas;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesClasspath;
import org.molgenis.emx2.tasks.Task;

public class ImportProfileTask extends Task {

  private static final String ONTOLOGY_LOCATION = "/_ontologies";
  private static final String ONTOLOGY_SEMANTICS_LOCATION = ONTOLOGY_LOCATION + "/_semantics.csv";

  @JsonIgnore private final Schema schema;
  private final String configLocation;
  private final boolean includeDemoData;

  public ImportProfileTask(Schema schema, String configLocation, boolean includeDemoData) {
    this.schema = schema;
    this.configLocation = configLocation;
    this.includeDemoData = includeDemoData;
  }

  @Override
  public void run() {
    this.start();
    Task commitTask = new Task();
    try {
      schema.tx(
          db -> {
            Schema s = db.getSchema(schema.getName());
            load(s);
            this.addSubTask(commitTask);
            commitTask.setDescription("Committing");
          });
    } catch (Exception e) {
      try {
        commitTask.completeWithError("CommitTask failed: " + e.getMessage());
      } catch (MolgenisException e2) {
        try {
          this.completeWithError("ImportProfileTask  failed: " + e2.getMessage());
        } catch (Exception e3) {
          throw (e3);
        }
      }
    }
    commitTask.complete();
    this.complete();
  }

  void load(Schema schema) {
    // load config (a.k.a. 'profile') YAML file
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(this.configLocation);
    Profiles profiles = getProfiles(schema, schemaFromProfile);

    // create the schema using the selected profile tags within the big model
    SchemaMetadata schemaMetadata = null;
    try {
      schemaMetadata = schemaFromProfile.create();
    } catch (Exception e) {
      throw new MolgenisException("Failed to create schema from profile: " + e.getMessage(), e);
    }

    // special option: fixed schema import location for ontologies (not schema or data)
    Schema ontologySchema;
    if (profiles.getOntologiesToFixedSchema() != null) {
      ontologySchema = createSchema(profiles.getOntologiesToFixedSchema(), schema.getDatabase());
      if (profiles.getSetFixedSchemaViewPermission() != null) {
        ontologySchema.addMember(
            profiles.getSetFixedSchemaViewPermission(), Privileges.VIEWER.toString());
      }
      if (profiles.getSetFixedSchemaEditPermission() != null) {
        ontologySchema.addMember(
            profiles.getSetFixedSchemaEditPermission(), Privileges.EDITOR.toString());
      }
    } else {
      ontologySchema = schema;
    }

    // special options: import additional models into ontology schema (schema=refs+ontologies)
    if (profiles.getAdditionalFixedSchemaModel() != null) {
      // load schema and data
      String fixedModelPath = profiles.getAdditionalFixedSchemaModel();
      TableStore fixedModelStore = new TableStoreForCsvFilesClasspath(fixedModelPath);
      Task importSchemaTask = new ImportSchemaTask(fixedModelStore, ontologySchema, false);
      importSchemaTask.setDescription("Import additional EMX into the ontology schema");
      this.addSubTask(importSchemaTask);
      importSchemaTask.run();
    }

    // import the schema
    schema.migrate(schemaMetadata);
    this.addSubTask("Loaded tables and columns from profile(s)").complete();

    // import ontologies (not schema or data)
    TableStore store = new TableStoreForCsvFilesClasspath(ONTOLOGY_LOCATION);
    Task ontologyTask =
        new ImportDataTask(ontologySchema, store, false)
            .setDescription("Import ontologies from profile");
    this.addSubTask(ontologyTask);
    ontologyTask.run();

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
      // prevent data tables with ontology table names to be imported into ontologies by accident

      for (String example : profiles.getDemoDataList()) {
        TableStore demoDataStore = new TableStoreForCsvFilesClasspath(example);
        Task demoDataTask =
            new ImportDataTask(schema, demoDataStore, false, includeTableNames)
                .setDescription("Import demo data from profile");
        this.addSubTask(demoDataTask);
        demoDataTask.run();
      }
    }

    // load schema settings from dir containing e.g. molgenis_settings.csv or molgenis_members.csv
    for (String setting : profiles.getSettingsList()) {
      MolgenisIO.fromClasspathDirectory(setting, schema, false);
    }

    // lastly, apply any ontology table semantics from a predefined location
    // this requires special parsing, because we must only update ontology tables used in the schema
    // to prevent adding additional unused tables
    SchemaMetadata ontologySemantics = getOntologySemantics(ontologySchema);
    ontologySchema.migrate(ontologySemantics);
  }

  private Profiles getProfiles(Schema schema, SchemaFromProfile schemaFromProfile) {
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
        ImportProfileTask profileLoader =
            new ImportProfileTask(
                createNewSchema, profileLocation, createSchemasIfMissing.isImportDemoData());
        profileLoader.setDescription("Loading profile: " + profileLocation);
        this.addSubTask(profileLoader);
        profileLoader.run();
        // profileLoader.load(createNewSchema, createSchemasIfMissing.isImportDemoData());
      }
    }
    return profiles;
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
