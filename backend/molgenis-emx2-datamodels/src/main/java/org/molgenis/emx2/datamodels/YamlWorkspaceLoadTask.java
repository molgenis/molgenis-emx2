package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.tasks.Task;

/**
 * Task wrapper that creates a schema from a discovered {@code /templates} YAML workspace bundle, so
 * the {@code createSchema} mutation can load yaml templates by the same bare name that {@code
 * /api/templates} advertises. Enum templates take precedence; this is the fallback (see {@link
 * DataModels#getImportTask}).
 */
public class YamlWorkspaceLoadTask extends Task {

  private final Database database;
  private final String template;
  private final String schemaName;
  private final boolean includeDemoData;

  public YamlWorkspaceLoadTask(
      Database database, String template, String schemaName, boolean includeDemoData) {
    this.database = database;
    this.template = template;
    this.schemaName = schemaName;
    this.includeDemoData = includeDemoData;
  }

  @Override
  public void run() {
    this.start();
    try {
      new YamlWorkspaceLoader().create(database, template, schemaName, includeDemoData);
      this.complete();
    } catch (Exception exception) {
      this.completeWithError(exception.getMessage());
      throw new MolgenisException(
          "Failed to create schema from YAML template '" + template + "'", exception);
    }
  }
}
