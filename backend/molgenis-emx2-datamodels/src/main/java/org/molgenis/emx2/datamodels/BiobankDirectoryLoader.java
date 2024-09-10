package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.ImportSchemaTask;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesClasspath;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.Task;

public class BiobankDirectoryLoader extends ImportDataModelTask {

  public static final String ONTOLOGIES = "DirectoryOntologies";

  private boolean staging = false;

  public BiobankDirectoryLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    String location = "biobank-directory/";
    if (this.staging) {
      location = "biobank-directory/stagingArea/";
    }

    Task ontologyTask = addSubTask("Loading tables for ontologies").start();
    Database db = getSchema().getDatabase();
    Schema ontologySchema = db.getSchema(ONTOLOGIES);
    if (ontologySchema == null) {
      createSchema(db.createSchema(ONTOLOGIES), "biobank-directory/ontologies/molgenis.csv");
      Schema ontologies = db.getSchema(ONTOLOGIES);
      ontologies.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
    }

    // create biobank-directory or staging schema (which will create tables in ontology schema)
    createSchema(getSchema(), location + "molgenis.csv");
    ontologyTask.complete();

    if (!this.staging) {
      getSchema().addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
    }

    if (ontologySchema == null || !this.staging) {
      TableStore store = new TableStoreForCsvFilesClasspath("biobank-directory/ontologies");
      Task ontologyDataTask =
          new ImportSchemaTask(store, db.getSchema(ONTOLOGIES), false)
              .setDescription("Import ontologies from profile");
      this.addSubTask(ontologyDataTask);
      ontologyDataTask.run();
    }

    // optionally, load demo data
    if (isIncludeDemoData()) {
      TableStore demoDataStore = new TableStoreForCsvFilesClasspath(location + "demo");
      Task demoDataTask =
          new ImportSchemaTask(demoDataStore, getSchema(), false)
              .setDescription("Import demo data from profile");
      this.addSubTask(demoDataTask);
      demoDataTask.run();
    }
    this.complete();
  }

  public ImportDataModelTask setStaging(boolean staging) {
    this.staging = staging;
    return this;
  }
}
