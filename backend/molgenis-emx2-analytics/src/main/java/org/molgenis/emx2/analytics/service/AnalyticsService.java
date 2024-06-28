package org.molgenis.emx2.analytics.service;

import org.molgenis.emx2.Schema;

public interface AnalyticsService {
  void createTriggerForSchema(Schema schema, String label, String selector, String app);
}
