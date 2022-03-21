package org.molgenis.emx2.io;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.io.tablestore.TableStoreForURL;
import org.molgenis.emx2.tasks.Task;

public class ImportMultipleSchemaTask extends Task {
  private Database database;
  private List<SchemaDeclaration> schemaDeclarationList;

  public ImportMultipleSchemaTask(
      Database database, List<SchemaDeclaration> schemaDeclarationList, boolean strict) {
    super("Import from schema list", strict);
    Objects.requireNonNull(database, "database cannot be null");
    Objects.requireNonNull(schemaDeclarationList, "schemaDeclarations cannot be null");
    this.schemaDeclarationList = schemaDeclarationList;
    this.database = database;
  }

  @Override
  public void run() {
    Task commit = new Task("Committing, might take a while...");
    // setup
    Task createSchemas = new Task("create schema(s)", isStrict());
    this.addStep(createSchemas);
    Task importMetadata = new Task("import metadata into schema(s)", isStrict());
    this.addStep(importMetadata);
    Task importData = new Task("import data into schema(s)", isStrict());
    this.addStep(importData);
    try {
      this.database.tx(
          db -> {

            // first create the schemas, we assume schemas are ordered for dependencies (might be
            // todo
            // to automate)

            for (SchemaDeclaration schemaDeclaration : schemaDeclarationList) {
              db.createSchema(schemaDeclaration.getName());
              createSchemas.addStep("Created schema " + schemaDeclaration.getName()).complete();
            }
            createSchemas.complete();

            // then load only the metadata (this ensured ontology references to other schemas will
            // be
            // instantiated before loading)
            for (SchemaDeclaration schemaDeclaration : schemaDeclarationList) {
              for (URL url : schemaDeclaration.getSourceURLs()) {
                new ImportMetadataTask(
                        db.getSchema(schemaDeclaration.getName()),
                        new TableStoreForURL(url),
                        isStrict())
                    .run();
                importMetadata
                    .addStep(
                        String.format(
                            "Imported metadata for schema %s from URL %s",
                            schemaDeclaration.getName(), url))
                    .complete();
              }
            }
            importMetadata.complete();

            // finally load the data (this ensures cross links work)});
            for (SchemaDeclaration schemaDeclaration : schemaDeclarationList) {
              for (URL url : schemaDeclaration.getSourceURLs()) {
                ImportDataTask step =
                    new ImportDataTask(
                        db.getSchema(schemaDeclaration.getName()),
                        new TableStoreForURL(url),
                        isStrict());
                importData.addStep(step);
                step.run();
              }
            }
            importData.complete();

            // committing, might take a while
            this.addStep(commit);
          });
    } catch (Exception e) {
      this.error("Import failed: " + e.getMessage());
      throw e;
    }
    commit.complete();
    this.complete();
  }
}
