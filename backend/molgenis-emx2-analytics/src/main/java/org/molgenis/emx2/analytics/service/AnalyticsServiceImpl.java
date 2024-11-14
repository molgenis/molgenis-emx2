package org.molgenis.emx2.analytics.service;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.Trigger;
import org.molgenis.emx2.analytics.model.actions.CreateTriggerAction;
import org.molgenis.emx2.analytics.model.actions.DeleteTriggerAction;
import org.molgenis.emx2.analytics.model.actions.UpdateTriggerAction;
import org.molgenis.emx2.analytics.repository.TriggerRepository;

public class AnalyticsServiceImpl implements AnalyticsService {

  private final TriggerRepository triggerRepository;

  public AnalyticsServiceImpl(TriggerRepository triggerRepository) {
    this.triggerRepository = triggerRepository;
  }

  @Override
  public void createTriggerForSchema(Schema schema, CreateTriggerAction createTriggerAction) {
    Trigger trigger =
        new Trigger(
            createTriggerAction.name(), createTriggerAction.cssSelector(), schema.getName(), null);
    this.triggerRepository.addTrigger(trigger);
  }

  @Override
  public boolean deleteTriggerForSchema(Schema schema, DeleteTriggerAction deleteTriggerAction) {
    return this.triggerRepository.deleteTrigger(deleteTriggerAction.name());
  }

  @Override
  public boolean updateTriggerForSchema(
      Schema schema, String triggerName, UpdateTriggerAction updateTriggerAction) {
    Trigger trigger =
        new Trigger(triggerName, updateTriggerAction.cssSelector(), schema.getName(), null);
    return triggerRepository.updateTrigger(trigger);
  }
}
