package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Sequence;
import org.molgenis.emx2.sql.autoid.SqlSequence;

class SqlSchemaTest {

  private static final String SCHEMA_NAME = SqlSchemaTest.class.getSimpleName();

  private SqlDatabase db;
  private SchemaMetadata metadata;
  private Schema schema;

  @BeforeEach
  void setupDatabase() {
    db = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    metadata = schema.getMetadata();
  }

  @Test
  void getSettingValue() {
    String testSetting = "TEST_SETTING";
    String testValue = "TEST_VALUE";
    metadata.setSetting(testSetting, testValue);

    String result = schema.getSettingValue(testSetting);
    assertEquals(testValue, result);
  }

  @Test
  void getSettingValueGetNonExistingValue() {
    String testSetting = "NON_EXISTING_SETTING";
    assertThrows(MolgenisException.class, () -> schema.getSettingValue(testSetting));
  }

  @Test
  void givenSchema_thenGetSequences() {
    assertTrue(schema.getSequences().isEmpty());
    SqlSequence.create(db.getJooq(), schema.getName(), "test-sequence", 42);

    List<Sequence> sequences = schema.getSequences();
    assertFalse(sequences.isEmpty());

    Sequence sequence = sequences.getFirst();
    assertEquals(42, sequence.getLimit());
  }
}
