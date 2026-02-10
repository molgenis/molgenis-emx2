package org.molgenis.emx2.io;

import java.util.*;
import java.util.function.Function;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.profiles.CreateSchemas;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesClasspath;
import org.molgenis.emx2.tasks.Task;

public class ImportProfileTask extends Task {

  private static final String ONTOLOGY_LOCATION = "/_ontologies";
  private static final String ONTOLOGY_SEMANTICS_LOCATION = ONTOLOGY_LOCATION + "/_semantics.csv";

  private final String configLocation;
  private final boolean includeDemoData;
  private final Database database;
  private final Function<Database, Schema> importSchemaFunction;
  private Schema schema;
  // set during load(), consumed after main transaction commits
  private Schema deferredOntologySchema;

  public ImportProfileTask(
      Database database,
      String schemaName,
      String description,
      String configLocation,
      boolean includeDemoData) {
    this(database, configLocation, includeDemoData, db -> db.createSchema(schemaName, description));
  }

  ImportProfileTask(
      Database database,
      String configLocation,
      boolean includeDemoData,
      Function<Database, Schema> importSchemaFunction) {
    this.database = database;
    this.configLocation = configLocation;
    this.includeDemoData = includeDemoData;
    this.importSchemaFunction = importSchemaFunction;
  }

  @Override
  public void run() {
    this.start();
    Task commitTask = new Task();
    try {
      this.database.tx(
          db -> {
            Schema s = importSchemaFunction.apply(db);
            this.schema = s;
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

    // import ontology data in a separate transaction to avoid holding locks on the shared
    // ontology schema for the duration of the main schema creation transaction
    if (deferredOntologySchema != null) {
      this.database.tx(
          db -> {
            Schema ontologySchema = db.getSchema(deferredOntologySchema.getName());
            loadOntologyData(ontologySchema);
          });
    }

    this.complete();
  }

  void load(Schema schema) {
    // load config (a.k.a. 'profile') YAML file
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(this.configLocation);
    Profiles profiles = getProfiles(schema, schemaFromProfile);

    // create the schema using the selected profile tags within the big model
    SchemaMetadata schemaMetadata;
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

    // import ontology data: if using a shared ontology schema, defer to a separate transaction
    // to avoid holding locks on shared tables during the main schema creation
    if (ontologySchema != schema) {
      deferredOntologySchema = ontologySchema;
    } else {
      loadOntologyData(ontologySchema);
    }

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
  }

  private void loadOntologyData(Schema ontologySchema) {
    TableStore store = new TableStoreForCsvFilesClasspath(ONTOLOGY_LOCATION);
    Task ontologyTask =
        new ImportOntologiesTask(
            ontologySchema, store, ONTOLOGY_LOCATION, ONTOLOGY_SEMANTICS_LOCATION);
    this.addSubTask(ontologyTask);
    ontologyTask.run();
  }

  private Profiles getProfiles(Schema schema, SchemaFromProfile schemaFromProfile) {
    Profiles profiles = schemaFromProfile.getProfiles();

    // special option: if there are createSchemasIfMissing, import those first
    if (profiles.getFirstCreateSchemasIfMissing() != null) {
      for (CreateSchemas createSchemasIfMissing : profiles.getFirstCreateSchemasIfMissing()) {
        String missingSchemaName = createSchemasIfMissing.getName();
        Database db = schema.getDatabase();
        Schema createNewSchema = db.getSchema(missingSchemaName);
        // if schema exists by this name, stop and continue with next
        if (createNewSchema != null) {
          continue;
        }
        String profileLocation = createSchemasIfMissing.getProfile();
        ImportProfileTask profileLoader =
            new ImportProfileTask(
                db,
                missingSchemaName,
                "",
                profileLocation,
                createSchemasIfMissing.isImportDemoData());
        profileLoader.setDescription("Loading profile: " + profileLocation);
        this.addSubTask(profileLoader);
        profileLoader.run();
        profileLoader.load(profileLoader.schema);
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

  /** Helper to check if schema exists and if not create it */
  private Schema createSchema(String schema, Database db) {
    Schema createSchema = db.getSchema(schema);
    if (createSchema == null) {
      createSchema = db.createSchema(schema);
    }
    return createSchema;
  }
}
