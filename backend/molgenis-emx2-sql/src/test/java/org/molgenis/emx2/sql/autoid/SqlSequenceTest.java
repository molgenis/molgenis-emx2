package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.jooq.Sequence;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SqlSequenceTest {

  private static final String SCHEMA_NAME = SqlSequenceTest.class.getSimpleName();

  private static SqlDatabase database;
  private Schema schema;
  private DSLContext jooq;

  @BeforeAll
  static void setup() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setupSchema() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
    jooq = database.getJooq();
  }

  @Test
  void givenColumn_thenCreateSequence() {
    assertTrue(jooq.meta().getSchemas(SCHEMA_NAME).getFirst().getSequences().isEmpty());
    SqlSequence.create(jooq, SCHEMA_NAME, "create_sequence", 12345);

    Sequence<?> actual = jooq.meta().getSchemas(SCHEMA_NAME).getFirst().getSequences().getFirst();
    assertSequenceMatches(actual, DSL.name(SCHEMA_NAME, "create_sequence"), 12345L);
  }

  @Test
  void givenSequence_thenExists() {
    assertFalse(SqlSequence.exists(jooq, SCHEMA_NAME, "exists"));
    SqlSequence.create(jooq, SCHEMA_NAME, "exists", 321);
    assertTrue(SqlSequence.exists(jooq, SCHEMA_NAME, "exists"));
  }

  @Test
  void givenExistingSequence_thenReturnGetCurrentValue() {
    SqlSequence sequence = SqlSequence.create(jooq, SCHEMA_NAME, "current_value", 123);
    assertEquals(0, sequence.getCurrentValue());
    sequence.getNextValue();
    assertEquals(1, sequence.getCurrentValue());
  }

  @Test
  void givenNonExistingSequence_whenGetCurrentValue_thenThrow() {
    SqlSequence sequence = new SqlSequence(jooq, SCHEMA_NAME, "non-existing");
    assertThrows(MolgenisException.class, sequence::getCurrentValue);
  }

  @Test
  void givenExistingSequence_thenReturnGetNextValue() {
    SqlSequence sequence = SqlSequence.create(jooq, SCHEMA_NAME, "next_value", 123);
    assertEquals(1, sequence.getNextValue());
    assertEquals(2, sequence.getNextValue());
    assertEquals(3, sequence.getNextValue());
  }

  @Test
  void givenNonExistingSchema_whenRequestingGetNextValue_thenThrow() {
    SqlSequence sequence = new SqlSequence(jooq, SCHEMA_NAME, "non-existing");
    assertThrows(MolgenisException.class, sequence::getNextValue);
  }

  @Test
  void givenSequence_thenGetLimit() {
    SqlSequence sequence = SqlSequence.create(jooq, SCHEMA_NAME, "get_limit", 1234);
    assertEquals(1234, sequence.getLimit());
  }

  @Test
  void givenNonExistingSequence_whenRequestingGetLimit_thenThrow() {
    SqlSequence sequence = new SqlSequence(jooq, SCHEMA_NAME, "get_limit");
    assertThrows(MolgenisException.class, sequence::getLimit);
  }

  @Test
  void givenSequence_thenDelete() {
    SqlSequence sequence = SqlSequence.create(jooq, SCHEMA_NAME, "delete", 1234);
    sequence.delete();
    assertFalse(SqlSequence.exists(jooq, SCHEMA_NAME, "delete"));
  }

  @Test
  void givenSequence_whenSettingValue_thenCurrentValueIsChanged() {
    SqlSequence sequence = SqlSequence.create(jooq, SCHEMA_NAME, "current_value", 1234);
    sequence.setCurrentValue(123);
    assertEquals(123, sequence.getCurrentValue());
  }

  @Test
  void givenSequence_whenSettingValueOutsideOfLimit_thenThrow() {
    SqlSequence sequence =
        SqlSequence.create(jooq, SCHEMA_NAME, "out_of_range_current_value", 1234);
    assertThrows(MolgenisException.class, () -> sequence.setCurrentValue(1235));
  }

  @Test
  void givenNonExistingSequence_whenSetting_thenThrow() {
    SqlSequence sequence = new SqlSequence(jooq, SCHEMA_NAME, "non_exisiting_current_value");
    assertThrows(MolgenisException.class, () -> sequence.setCurrentValue(10));
  }

  private void assertSequenceMatches(Sequence<?> seq, Name name, long maxValue) {
    assertEquals(SCHEMA_NAME, seq.getSchema().getName());
    assertEquals(seq.getQualifiedName(), name);
    assertEquals(maxValue, DSL.val(seq.getMaxvalue()).getValue());
  }
}
