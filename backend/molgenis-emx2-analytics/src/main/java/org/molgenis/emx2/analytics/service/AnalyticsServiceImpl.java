package org.molgenis.emx2.analytics.service;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.Trigger;
import org.molgenis.emx2.analytics.model.actions.CreateTriggerAction;
import repository.TriggerRepository;

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
}
