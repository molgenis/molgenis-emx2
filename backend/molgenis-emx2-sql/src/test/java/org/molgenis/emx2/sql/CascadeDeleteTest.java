package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

class CascadeDeleteTest {
  private static Schema schema;

  @Test
  void shouldCascadeDeleteRefRowWhenRefSourceIsDeleted() {
    createTestSchema(true);

    // check the data is present before dropping the table
    Table participants = schema.getTable("Participants");
    Table samples = schema.getTable("Samples");
    assertEquals(1, participants.retrieveRows().size());
    assertEquals(1, samples.retrieveRows().size());

    // delete the participant row, which should cascade delete the sample row
    participants.delete(participants.retrieveRows());
    assertEquals(0, participants.retrieveRows().size());
    assertEquals(0, samples.retrieveRows().size());
  }

  @Test
  void shouldNotCascadeDeleteRefRowWhenRefSourceIsDeleted() {
    createTestSchema(false);

    // check the data is present before dropping the table
    Table participants = schema.getTable("Participants");
    Table samples = schema.getTable("Samples");
    assertEquals(1, participants.retrieveRows().size());
    assertEquals(1, samples.retrieveRows().size());

    // delete the participant row, which should NOT cascade delete the sample row
    assertThrowsExactly(
        SqlMolgenisException.class, () -> participants.delete(participants.retrieveRows()));
    // expect the participant not to be deleted, due to the foreign key constraint
    assertEquals(1, participants.retrieveRows().size());
    // expect the sample to still be present, since the participant was not deleted
    assertEquals(1, samples.retrieveRows().size());
  }

  private void createTestSchema(boolean cascadeDelete) {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
    Table participants =
        schema.create(table("Participants").add(column("participantId").setPkey()));

    Table samples =
        schema.create(
            table("Samples")
                .add(column("sampleId").setPkey())
                .add(
                    column("participantId")
                        .setType(org.molgenis.emx2.ColumnType.REF)
                        .setRefTable(participants.getName())
                        .setRefSchemaName(schema.getName())
                        .setCascadeDelete(cascadeDelete)));

    participants.insert(Row.row("participantId", "P1"));
    samples.insert(Row.row("sampleId", "S1", "participantId", "P1"));
  }
}
