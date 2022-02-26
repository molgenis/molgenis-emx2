package org.molgenis.emx2.io;

import java.net.URL;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.tasks.Step;
import org.molgenis.emx2.tasks.Task;

public class ImportSchemaListTask extends Task {
  private Database database;
  private List<SchemaDeclaration> schemaDeclarationList;

  public ImportSchemaListTask(Database database, List<SchemaDeclaration> schemas, boolean strict) {
    super("Import from schema list", strict);
    this.schemaDeclarationList = schemas;
    this.database = database;
  }

  @Override
  public void run() {
    Step commit = new Step("Committing, might take a while...");
    this.database.tx(
        db -> {

          // first create the schemas
          Task createSchemas = new Task("create schema(s)", isStrict());
          this.add(createSchemas);
          for (SchemaDeclaration schemaDeclaration : schemaDeclarationList) {
            db.createSchema(schemaDeclaration.getName());
            createSchemas.step("Created schema " + schemaDeclaration.getName()).complete();
          }
          createSchemas.complete();

          // then load only the metadata (this ensured ontology references to other schemas will be
          // instantiated before loading)
          Task importMetadata = new Task("import metadata(s)", isStrict());
          this.add(importMetadata);
          for (SchemaDeclaration schemaDeclaration : schemaDeclarationList) {
            for (URL url : schemaDeclaration.getSourceURLs()) {
              new ImportURLTask(url, db.getSchema(schemaDeclaration.getName()), isStrict())
                  .setFilter(ImportSchemaTask.Filter.METADATA_ONLY)
                  .run();
              importMetadata
                  .step(
                      String.format(
                          "Imported metadata for schema %s from URL %s",
                          schemaDeclaration.getName(), url))
                  .complete();
            }
          }
          importMetadata.complete();

          // finally load the data (this ensures cross links work)});
          Task importData = new Task("import data(s)", true);
          this.add(importData);
          for (SchemaDeclaration schemaDeclaration : schemaDeclarationList) {
            for (URL url : schemaDeclaration.getSourceURLs()) {
              ImportSchemaTask step =
                  new ImportURLTask(url, db.getSchema(schemaDeclaration.getName()), isStrict())
                      .setFilter(ImportSchemaTask.Filter.DATA_ONLY);
              importData.add(step);
              step.run();
            }
          }
          importData.complete();

          // committing, might take a while
          this.add(commit);
        });
    // commit complete
    commit.complete();
  }
}
