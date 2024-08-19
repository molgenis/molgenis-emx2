package org.molgenis.emx2.analytics.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.Trigger;
import org.molgenis.emx2.analytics.model.actions.CreateTriggerAction;
import org.molgenis.emx2.analytics.repository.TriggerRepository;

public class TestAnalyticsService {

  @Test
  public void createTriggerForSchema() {
    TriggerRepository triggerRepository = mock(TriggerRepository.class);
    Schema schema = mock(Schema.class);
    when(schema.getName()).thenReturn("test-schema");
    AnalyticsServiceImpl service = new AnalyticsServiceImpl(triggerRepository);
    service.createTriggerForSchema(schema, new CreateTriggerAction("triggerName", "my selector"));

    verify(triggerRepository, times(1))
        .addTrigger(new Trigger("triggerName", "my selector", "test-schema", null));
  }
}
