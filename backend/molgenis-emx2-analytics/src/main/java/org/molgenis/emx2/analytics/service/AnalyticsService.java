package org.molgenis.emx2.analytics.service;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.actions.CreateTriggerAction;

public interface AnalyticsService {
  void createTriggerForSchema(Schema schema, CreateTriggerAction createTriggerAction);
}
