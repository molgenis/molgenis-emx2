package org.molgenis.emx2.analytics.service;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.Trigger;
import repository.TriggerRepository;

public class AnalyticsServiceImpl implements AnalyticsService {

  private final TriggerRepository triggerRepository;

  public AnalyticsServiceImpl(TriggerRepository triggerRepository) {
    this.triggerRepository = triggerRepository;
  }

  @Override
  public void createTriggerForSchema(Schema schema, String label, String selector, String app) {
    Trigger trigger = new Trigger(label, selector, schema.getName(), app);
    this.triggerRepository.addTrigger(trigger);
  }
}
