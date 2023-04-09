package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import java.util.UUID;
import org.molgenis.emx2.*;

/** starts scripts and submits them to the taskService as a job */
public class ScriptService {
  private Schema systemSchema;
  private TaskService taskService;

  public ScriptService(Schema systemSchema, TaskService taskService) {
    this.taskService = taskService;
  }

  public String startScriptTask(String name) {
    String id = UUID.randomUUID().toString();
    // retrieve the script from databbase
    Row scriptMetadata =
        systemSchema.getTable("Scripts").where(f("name", EQUALS, name)).retrieveRows().get(0);
    if (scriptMetadata != null) {
      // submit the script
      taskService.submit(
          new ScriptTask(
              name, scriptMetadata.getString("script"), scriptMetadata.getText("parameters")));
    } else {
      throw new MolgenisException("Script execution failed: " + name + " not found");
    }
    return id;
  }
}
