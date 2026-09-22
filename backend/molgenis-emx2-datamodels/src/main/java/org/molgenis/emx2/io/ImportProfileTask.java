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
  private String ontologySchemaName;
  private Profiles profiles;

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
    try {
      runCreateSchemaTransaction();
      runOntologyTransaction();
      runDemoDataTransaction();
      this.complete();
    } catch (Exception e) {
      this.completeWithError("ImportProfileTask failed: " + e.getMessage());
    }
  }

  private void runCreateSchemaTransaction() {
    Task schemaTask = this.addSubTask("Create schema and metadata");
    schemaTask.start();
    Task commitTask = new Task("Committing");
    this.database.tx(
        db -> {
          Schema s = importSchemaFunction.apply(db);
          this.schema = s;
          loadSchemaOnly(s, schemaTask);
          schemaTask.addSubTask(commitTask);
        });
    commitTask.complete();
    schemaTask.complete();
  }

  private void runDemoDataTransaction() {
    if (!includeDemoData) {
      return;
    }
    Task demoDataTask = this.addSubTask("Import demo data");
    demoDataTask.start();
    Task commitTask = new Task("Committing");
    this.database.tx(
        db -> {
          Schema s = db.getSchema(schema.getName());
          loadDemoData(s, demoDataTask);
          demoDataTask.addSubTask(commitTask);
        });
    commitTask.complete();
    demoDataTask.complete();
  }

  private void runOntologyTransaction() {
    if (ontologySchemaName == null) {
      return;
    }
    Task ontologyTask = this.addSubTask("Import ontology data");
    ontologyTask.start();
    Task commitTask = new Task("Committing");
    this.database.tx(
        db -> {
          Schema ontologySchema = db.getSchema(ontologySchemaName);
          loadOntologyData(ontologySchema, ontologyTask);
          ontologyTask.addSubTask(commitTask);
        });
    commitTask.complete();
    ontologyTask.complete();
  }

  void loadSchemaOnly(Schema schema, Task parentTask) {
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(this.configLocation);
    this.profiles = getProfiles(schema, schemaFromProfile, parentTask);

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

    if (profiles.getAdditionalFixedSchemaModel() != null) {
      String fixedModelPath = profiles.getAdditionalFixedSchemaModel();
      TableStore fixedModelStore = new TableStoreForCsvFilesClasspath(fixedModelPath);
      Task importSchemaTask = new ImportSchemaTask(fixedModelStore, ontologySchema, false);
      importSchemaTask.setDescription("Import additional EMX into the ontology schema");
      parentTask.addSubTask(importSchemaTask);
      importSchemaTask.run();
    }

    schema.migrate(schemaMetadata);
    parentTask.addSubTask("Loaded tables and columns from profile(s)").complete();

    this.ontologySchemaName = ontologySchema.getName();

    if (profiles.getSetViewPermission() != null) {
      schema.addMember(profiles.getSetViewPermission(), Privileges.VIEWER.toString());
    }
    if (profiles.getSetEditPermission() != null) {
      schema.addMember(profiles.getSetEditPermission(), Privileges.EDITOR.toString());
    }

    for (String setting : profiles.getSettingsList()) {
      MolgenisIO.fromClasspathDirectory(setting, schema, false);
    }
  }

  void loadDemoData(Schema schema, Task parentTask) {
    String[] includeTableNames = getTypeOfTablesToInclude(schema);
    for (String example : profiles.getDemoDataList()) {
      TableStore demoDataStore = new TableStoreForCsvFilesClasspath(example);
      Task demoDataTask =
          new ImportDataTask(schema, demoDataStore, false, includeTableNames)
              .setDescription("Import demo data from profile");
      parentTask.addSubTask(demoDataTask);
      demoDataTask.run();
    }
  }

  private void loadOntologyData(Schema ontologySchema, Task parentTask) {
    TableStore store = new TableStoreForCsvFilesClasspath(ONTOLOGY_LOCATION);
    Task ontologyTask =
        new ImportOntologiesTask(
            ontologySchema, store, ONTOLOGY_LOCATION, ONTOLOGY_SEMANTICS_LOCATION);
    parentTask.addSubTask(ontologyTask);
    ontologyTask.run();
  }

  private Profiles getProfiles(
      Schema schema, SchemaFromProfile schemaFromProfile, Task parentTask) {
    Profiles profiles = schemaFromProfile.getProfiles();

    if (profiles.getFirstCreateSchemasIfMissing() != null) {
      for (CreateSchemas createSchemasIfMissing : profiles.getFirstCreateSchemasIfMissing()) {
        String missingSchemaName = createSchemasIfMissing.getName();
        Database db = schema.getDatabase();
        Schema createNewSchema = db.getSchema(missingSchemaName);
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
        parentTask.addSubTask(profileLoader);
        profileLoader.run();
      }
    }
    return profiles;
  }

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

  private Schema createSchema(String schema, Database db) {
    Schema createSchema = db.getSchema(schema);
    if (createSchema == null) {
      createSchema = db.createSchema(schema);
    }
    return createSchema;
  }
}
