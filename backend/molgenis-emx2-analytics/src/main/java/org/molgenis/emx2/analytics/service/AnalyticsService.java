package org.molgenis.emx2.analytics.service;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.actions.CreateTriggerAction;
import org.molgenis.emx2.analytics.model.actions.DeleteTriggerAction;
import org.molgenis.emx2.analytics.model.actions.UpdateTriggerAction;

public interface AnalyticsService {
  void createTriggerForSchema(Schema schema, CreateTriggerAction createTriggerAction);

  boolean deleteTriggerForSchema(Schema schema, DeleteTriggerAction deleteTriggerAction);

  boolean updateTriggerForSchema(
      Schema schema, String triggerName, UpdateTriggerAction updateTriggerAction);
}
