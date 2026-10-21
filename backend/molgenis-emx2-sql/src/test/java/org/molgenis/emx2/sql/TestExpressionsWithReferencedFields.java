package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

class TestExpressionsWithReferencedFields {

  private static final String SCHEMA_NAME =
      TestExpressionsWithReferencedFields.class.getSimpleName();

  private final Database database = TestDatabaseFactory.getTestDatabase();
  private Schema schema;

  @BeforeEach
  void setup() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Hospital", column("name").setPkey(), column("city")),
        table(
            "Doctor",
            column("id").setPkey(),
            column("name"),
            column("specialty"),
            column("hospital").setType(REF).setRefTable("Hospital"),
            column("hospital city").setComputed("hospital.city")),
        table(
            "Patient",
            column("id").setPkey(),
            column("name"),
            column("doctor").setType(REF).setRefTable("Doctor"),
            column("doctor name").setComputed("doctor.name"),
            column("hospital city").setComputed("doctor?.hospital?.city"),
            column("consulted doctors").setType(REF_ARRAY).setRefTable("Doctor"),
            column("consulted specialties")
                .setComputed("consultedDoctors.map(d => d.specialty).join(',')")),
        table(
            "Visit",
            column("patient").setType(REF).setRefTable("Patient").setKey(1),
            column("date").setType(DATE).setKey(1),
            column("notes")),
        table(
            "Treatment",
            column("id").setPkey(),
            column("visit").setType(REF).setRefTable("Visit"),
            column("visit notes").setComputed("visit.notes"),
            column("treated in").setComputed("visit.patient.doctor.hospital.city")));

    schema.getTable("Hospital").insert(row("name", "UMCG", "city", "Groningen"));
    schema.getTable("Hospital").insert(row("name", "Erasmus MC", "city", "Rotterdam"));
    schema
        .getTable("Doctor")
        .insert(
            row("id", "d1", "name", "Dr. Bob", "specialty", "cardiology", "hospital", "UMCG"),
            row("id", "d2", "name", "Dr. Ann", "specialty", "neurology", "hospital", "Erasmus MC"));
  }

  @Test
  void computesFromFieldOfReferencedRow() {
    assertEquals("Groningen", get("Doctor", "id", "d1").getString("hospital city"));
    assertEquals("Rotterdam", get("Doctor", "id", "d2").getString("hospital city"));
  }

  @Test
  void computesFromFieldsOfNestedReferences() {
    schema.getTable("Patient").insert(row("id", "p1", "name", "Piet", "doctor", "d2"));

    Row patient = get("Patient", "id", "p1");
    assertEquals("Dr. Ann", patient.getString("doctor name"));
    assertEquals("Rotterdam", patient.getString("hospital city"));
  }

  @Test
  void computesFromFieldsOfEveryRowInARefArray() {
    schema
        .getTable("Patient")
        .insert(row("id", "p1", "name", "Piet", "consulted doctors", "d2,d1"));

    assertEquals(
        "neurology,cardiology", get("Patient", "id", "p1").getString("consulted specialties"));
  }

  @Test
  void computesFromReferenceWithCompositeKey() {
    schema.getTable("Patient").insert(row("id", "p1", "name", "Piet", "doctor", "d1"));
    schema
        .getTable("Visit")
        .insert(row("patient", "p1", "date", "2026-10-01", "notes", "first visit"));
    schema
        .getTable("Treatment")
        .insert(row("id", "t1", "visit.patient", "p1", "visit.date", "2026-10-01"));

    Row treatment = get("Treatment", "id", "t1");
    assertEquals("first visit", treatment.getString("visit notes"));
    assertEquals("Groningen", treatment.getString("treated in"));
  }

  @Test
  void computesFromRowsInsertedEarlierInTheSameTransaction() {
    database.tx(
        transaction -> {
          Schema transactionSchema = transaction.getSchema(SCHEMA_NAME);
          transactionSchema
              .getTable("Hospital")
              .insert(row("name", "Martini", "city", "Groningen"));
          transactionSchema
              .getTable("Doctor")
              .insert(row("id", "d3", "name", "Dr. Kim", "hospital", "Martini"));
        });

    assertEquals("Groningen", get("Doctor", "id", "d3").getString("hospital city"));
  }

  @Test
  void computesWithOnlyKeyFieldsWhenReferencedRowIsInsertedLater() {
    database.tx(
        transaction -> {
          Schema transactionSchema = transaction.getSchema(SCHEMA_NAME);
          transactionSchema
              .getTable("Doctor")
              .insert(row("id", "d3", "name", "Dr. Kim", "hospital", "Martini"));
          transactionSchema
              .getTable("Hospital")
              .insert(row("name", "Martini", "city", "Groningen"));
        });

    assertNull(get("Doctor", "id", "d3").getString("hospital city"));
  }

  @Test
  void computesFromReferencedRowOnUpdate() {
    schema.getTable("Doctor").update(row("id", "d1", "name", "Dr. Bob", "hospital", "Erasmus MC"));

    assertEquals("Rotterdam", get("Doctor", "id", "d1").getString("hospital city"));
  }

  @Test
  void validatesWithFieldOfReferencedRow() {
    schema
        .getTable("Patient")
        .getMetadata()
        .add(column("local").setType(BOOL).setValidation("doctor.hospital.city == 'Groningen'"));

    schema.getTable("Patient").insert(row("id", "p1", "doctor", "d1", "local", "true"));
    assertThrows(
        MolgenisException.class,
        () -> schema.getTable("Patient").insert(row("id", "p2", "doctor", "d2", "local", "true")));
  }

  private Row get(String tableName, String key, String value) {
    return schema.getTable(tableName).where(f(key, EQUALS, value)).retrieveRows().getFirst();
  }
}
