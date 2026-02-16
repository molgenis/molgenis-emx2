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
    assertTrue(jooq.meta().getSequences().isEmpty());
    SqlSequence.create(jooq, schema.getName(), "create_sequence", 12345);

    Sequence<?> actual = jooq.meta().getSequences().getFirst();
    assertSequenceMatches(actual, DSL.name(schema.getName(), "create_sequence"), 12345L);
  }

  @Test
  void givenSequence_thenExists() {
    assertFalse(SqlSequence.exists(jooq, schema.getName(), "exists"));
    SqlSequence.create(jooq, schema.getName(), "exists", 321);
    assertTrue(SqlSequence.exists(jooq, schema.getName(), "exists"));
  }

  @Test
  void givenExistingSequence_thenReturnCurrentValue() {
    SqlSequence sequence = SqlSequence.create(jooq, schema.getName(), "current_value", 123);
    assertEquals(0, sequence.currentValue());
    sequence.nextValue();
    assertEquals(1, sequence.currentValue());
  }

  @Test
  void givenNonExistingSequence_whenCurrentValue_thenThrow() {
    SqlSequence sequence = new SqlSequence(jooq, schema.getName(), "non-existing");
    assertThrows(MolgenisException.class, sequence::currentValue);
  }

  @Test
  void givenExistingSequence_thenReturnNextValue() {
    SqlSequence sequence = SqlSequence.create(jooq, schema.getName(), "next_value", 123);
    assertEquals(1, sequence.nextValue());
    assertEquals(2, sequence.nextValue());
    assertEquals(3, sequence.nextValue());
  }

  @Test
  void givenNonExistingSchema_whenRequestingNextValue_thenThrow() {
    SqlSequence sequence = new SqlSequence(jooq, schema.getName(), "non-existing");
    assertThrows(MolgenisException.class, sequence::nextValue);
  }

  @Test
  void givenSequence_thenGetLimit() {
    SqlSequence sequence = SqlSequence.create(jooq, schema.getName(), "get_limit", 1234);
    assertEquals(1234, sequence.limit());
  }

  @Test
  void givenNonExistingSequence_whenRequestingLimit_thenThrow() {
    SqlSequence sequence = new SqlSequence(jooq, schema.getName(), "get_limit");
    assertThrows(MolgenisException.class, sequence::limit);
  }

  @Test
  void givenSequence_thenDelete() {
    SqlSequence sequence = SqlSequence.create(jooq, schema.getName(), "delete", 1234);
    sequence.delete();
    assertFalse(SqlSequence.exists(jooq, schema.getName(), "delete"));
  }

  private void assertSequenceMatches(Sequence<?> seq, Name name, long maxValue) {
    assertEquals(schema.getName(), seq.getSchema().getName());
    assertEquals(seq.getQualifiedName(), name);
    assertEquals(maxValue, DSL.val(seq.getMaxvalue()).getValue());
  }
}
