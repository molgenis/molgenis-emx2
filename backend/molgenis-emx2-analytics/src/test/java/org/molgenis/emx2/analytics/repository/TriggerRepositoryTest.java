package org.molgenis.emx2.analytics.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.analytics.model.Trigger;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
class TriggerRepositoryTest {

  private static final String SCHEMA_NAME = TriggerRepositoryTest.class.getSimpleName();
  private static final String TRIGGER_NAME = "triggerName";

  static Database database;
  static Schema testTriggerRepo;
  static TriggerRepository triggerRepository;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    testTriggerRepo = database.dropCreateSchema(SCHEMA_NAME);
    triggerRepository = new TriggerRepositoryImpl(database);

    // AnalyticsTrigger's "name" pkey carries no FK to the schema, so dropCreateSchema does
    // not cascade away a trigger row a previous run against this database left behind
    Table triggers =
        database.getSchema(SYSTEM_SCHEMA).getTable(TriggerRepositoryImpl.TRIGGER_TABLE_NAME);
    List<Row> stale = triggers.where(f("name", EQUALS, TRIGGER_NAME)).retrieveRows();
    if (!stale.isEmpty()) {
      triggers.delete(stale);
    }
  }

  @Test
  void addAndGetTrigger() {
    Trigger t = new Trigger(TRIGGER_NAME, "triggerDescription", SCHEMA_NAME, null);
    triggerRepository.addTrigger(t);
    assertEquals(
        Collections.singletonList(t), triggerRepository.getTriggersForSchema(testTriggerRepo));
  }
}
