package org.molgenis.emx2.analytics.repository;

import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.Trigger;

public interface TriggerRepository {
  Trigger addTrigger(Trigger trigger);

  List<Trigger> getTriggersForSchema(Schema schema);

  boolean deleteTrigger(String triggerName);

  boolean updateTrigger(Trigger trigger);
}
