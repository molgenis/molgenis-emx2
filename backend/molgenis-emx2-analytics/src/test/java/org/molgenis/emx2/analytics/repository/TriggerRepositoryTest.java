package org.molgenis.emx2.analytics.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.Trigger;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
class TriggerRepositoryTest {

  Database database;
  Schema testTriggerRepo;
  TriggerRepository triggerRepository;

  @BeforeEach
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    testTriggerRepo =
        database.hasSchema("testTriggerRepo")
            ? database.getSchema("testTriggerRepo")
            : database.createSchema("testTriggerRepo");
    triggerRepository = new TriggerRepositoryImpl(database);
  }

  @Test
  void addAndGetTrigger() {
    try {
      Trigger t = new Trigger("triggerName", "triggerDescription", "testTriggerRepo", null);
      triggerRepository.addTrigger(t);
      assertEquals(
          Collections.singletonList(t), triggerRepository.getTriggersForSchema(testTriggerRepo));
    } catch (Exception e) {
      fail(e.getMessage());
    } finally {
      String deleteQuery =
          """
              DELETE
              FROM "_SYSTEM_"."AnalyticsTrigger"
              WHERE name LIKE 'triggerName' ESCAPE '#';
              """;
      ((SqlDatabase) database).getJooq().execute(deleteQuery);
    }
  }
}
