package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.*;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ColumnSequenceIdGeneratorTest {

  private static final String SCHEMA_NAME = ColumnSequenceIdGeneratorTest.class.getSimpleName();

  private static SqlDatabase database;
  private static DSLContext jooq;
  private Schema schema;

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
  void givenColumnWithoutComputed_thenThrow() {
    Column column = addColumnWithComputedToSchema(null);
    assertThrows(IllegalArgumentException.class, () -> new ColumnSequenceIdGenerator(column, jooq));
  }

  @Test
  void givenSequence_thenUseCompleteSet() {
    Column column = addColumnWithComputedToSchema("${mg_autoid(length=2, format=numbers)}");
    ColumnSequenceIdGenerator generator = new ColumnSequenceIdGenerator(column, jooq);

    List<String> expectedIds =
        IntStream.range(0, 100)
            .boxed()
            .map(String::valueOf)
            .map(id -> StringUtils.leftPad(id, 2, "0"))
            .toList();

    List<String> actualIds = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      actualIds.add(generator.generateId());
    }

    assertNotEquals(expectedIds, actualIds);
    List<String> sorted = actualIds.stream().sorted().toList();
    assertEquals(expectedIds, sorted);

    // Exhaust id
    assertThrows(
        MolgenisException.class, generator::generateId, "Unable to generate value for sequence");
  }

  @Test
  void givenColumnWithAutoId_thenSetUpSequenceWithRelatedName() {
    String computed = "FOO-${mg_autoid(length=3, format=numbers)}}-BAR";
    Column column = addColumnWithComputedToSchema(computed);
    new ColumnSequenceIdGenerator(column, jooq);
    String expectedSequenceName =
        SCHEMA_NAME + "-Person-id-" + HexFormat.of().toHexDigits(computed.hashCode());
    assertTrue(SqlSequence.exists(jooq, SCHEMA_NAME, expectedSequenceName));
  }

  @Test
  void whenMultipleFormatsProvided_thenThrow() {
    Column column =
        addColumnWithComputedToSchema(
            "FOO-${mg_autoid(length=3, format=numbers)}-${mg_autoid(length=5, format=numbers)}");
    assertThrows(MolgenisException.class, () -> new ColumnSequenceIdGenerator(column, jooq));
  }

  @Test
  void givenSchemaWithSettings_thenUseKeyFromSettingsForRandomizer() {
    Column column = addColumnWithComputedToSchema("FOO-${mg_autoid(length=4, format=numbers)}");
    ColumnSequenceIdGenerator generator = new ColumnSequenceIdGenerator(column, jooq);
    assertEquals("FOO-2871", generator.generateId());

    schema
        .getMetadata()
        .setSetting(AUTOID_RANDOMIZER_KEY_SETTING, "FD79FFE0C8F6051B42DC7A3B4660F244");

    generator = new ColumnSequenceIdGenerator(column, jooq);
    assertNotEquals("FOO-1819", generator.generateId());
  }

  private Column addColumnWithComputedToSchema(String computed) {
    return schema
        .create(
            TableMetadata.table(
                "Person", column("id", ColumnType.AUTO_ID).setPkey().setComputed(computed)))
        .getMetadata()
        .getColumn("id");
  }
}
