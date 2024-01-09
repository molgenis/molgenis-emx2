package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableListener;

public class ScriptTableListener extends TableListener {
  private TaskServiceScheduler scheduleService;

  public ScriptTableListener(TaskServiceScheduler scheduleService) {
    super(SYSTEM_SCHEMA, "Scripts");
    this.scheduleService = scheduleService;
  }

  @Override
  public Runnable afterSave(Row saved) {
    return () -> scheduleService.update(new ScriptTask(saved));
  }

  @Override
  public Runnable afterDelete(Row deleted) {
    return () -> scheduleService.unschedule(deleted.getString("name"));
  }
}
