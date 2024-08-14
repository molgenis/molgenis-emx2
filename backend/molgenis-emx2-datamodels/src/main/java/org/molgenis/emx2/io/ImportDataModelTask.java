package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.tasks.Task;

public class ImportDataModelTask extends Task {

  private Schema schema;
  private String template;
  private boolean includeDemoData;

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
      Task task = DataModels.getTask(schema, template, includeDemoData);
      task.setDescription("Loading data model: " + template);
      this.addSubTask(task);
      task.run();

    } catch (Exception e) {
      this.setError("Error importing data model: " + e.getMessage());
    }
    this.complete();
  }
}
