package org.molgenis.emx2.io;

import java.io.InputStreamReader;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.tasks.Task;

public class ImportDataModelTask extends Task {

  private final Schema schema;
  private final String template;
  private final boolean includeDemoData;

  public ImportDataModelTask(Schema schema, Boolean includeDemoData) {
    this(schema, null, includeDemoData);
  }

  public ImportDataModelTask(Schema schema, String template, Boolean includeDemoData) {
    this.schema = schema;
    this.template = template;
    this.includeDemoData = includeDemoData;
    this.setDescription("Creating schema: " + schema.getName());
  }

  @Override
  public void run() {
    this.start();
    try {
      Task task = DataModels.getImportTask(schema, template, includeDemoData);
      task.setDescription("Loading data model: " + template);
      this.addSubTask(task);
      task.run();

    } catch (Exception e) {
      this.completeWithError("Error importing data model: " + e.getMessage());
      throw (e);
    }
    this.complete();
  }

  public Schema getSchema() {
    return schema;
  }

  public boolean isIncludeDemoData() {
    return includeDemoData;
  }

  public void createSchema(String path) {
    createSchema(this.schema, path);
  }

  public static void createSchema(Schema schema, String path) {
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    ImportDataModelTask.class.getClassLoader().getResourceAsStream(path))));
    schema.migrate(metadata);
  }
}
