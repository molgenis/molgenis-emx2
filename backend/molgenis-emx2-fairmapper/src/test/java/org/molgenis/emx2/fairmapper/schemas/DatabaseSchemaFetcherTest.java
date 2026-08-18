package org.molgenis.emx2.fairmapper.schemas;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class DatabaseSchemaFetcherTest {

  private static final String SCHEMA_NAME = DatabaseSchemaFetcherTest.class.getSimpleName();
  private static final Database DATABASE = TestDatabaseFactory.getTestDatabase();

  private static DatabaseSchemaFetcher fetcher;

  @BeforeAll
  static void setupSchema() {
    DATABASE.createSchema(SCHEMA_NAME);
    fetcher = new DatabaseSchemaFetcher(DATABASE);
  }

  @Test
  void givenSchemaName_whenExists_thenReturn() {
    Optional<SchemaMetadata> fetch = fetcher.fetch(SCHEMA_NAME);
    assertTrue(fetch.isPresent());
    assertEquals(SCHEMA_NAME, fetch.get().getName());
  }

  @Test
  void givenSchemaName_whenNoSchemaExists_thenReturnEmpty() {
    assertTrue(fetcher.fetch("non-existent-schema").isEmpty());
  }
}
