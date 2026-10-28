package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Query.Option.INCLUDE_FILE_CONTENTS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TestResourceLoader.getFile;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class SaveWithUpdateModeTest {

  private static final String SCHEMA_NAME = SaveWithUpdateModeTest.class.getSimpleName();
  private static Database database;
  private Schema schema;

  @BeforeAll
  static void setUpDatabase() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @AfterAll
  static void tearDownDatabase() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @BeforeEach
  void setUp() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(table("Hospital", column("name").setPkey(), column("city")));
    schema.create(
        table(
            "Doctor",
            column("id").setPkey(),
            column("name"),
            column("specialty"),
            column("hospital").setType(REF).setRefTable("Hospital")));
    schema.create(
        table(
            "Patient",
            column("id").setPkey(),
            column("name"),
            column("birthDate").setType(DATE),
            column("doctor").setType(REF).setRefTable("Doctor"),
            column("hospital").setType(REF).setRefTable("Hospital")));
    schema.getTable("Hospital").insert(row("name", "UMCG", "city", "Groningen"));
    schema.getTable("Doctor").insert(row("id", "d1", "name", "House", "hospital", "UMCG"));
  }

  @Test
  void givenExistingRow_whenSavingWithOmittedColumns_thenOmittedColumnsArePreserved() {
    Table patients = schema.getTable("Patient");
    patients.insert(
        row(
            "id",
            "p1",
            "name",
            "Jane",
            "birthDate",
            "1980-01-01",
            "doctor",
            "d1",
            "hospital",
            "UMCG"));

    patients.save(List.of(row("id", "p1", "birthDate", "1981-02-02")), UpdateMode.UPDATE);

    Row patient = patients.retrieveRows().getFirst();
    assertEquals("Jane", patient.getString("name"));
    assertEquals("1981-02-02", patient.getString("birthDate"));
    assertEquals("d1", patient.getString("doctor"));
    assertEquals("UMCG", patient.getString("hospital"));
  }

  @Test
  void givenExistingRow_whenSavingNullValue_thenColumnIsCleared() {
    Table patients = schema.getTable("Patient");
    patients.insert(row("id", "p1", "name", "Jane", "doctor", "d1"));

    patients.save(List.of(row("id", "p1", "doctor", null)), UpdateMode.UPDATE);

    Row patient = patients.retrieveRows().getFirst();
    assertEquals("Jane", patient.getString("name"));
    assertNull(patient.getString("doctor"));
  }

  @Test
  void givenNewAndExistingRows_whenSaving_thenNewRowsAreInsertedAndExistingRowsUpdated() {
    Table patients = schema.getTable("Patient");
    patients.insert(row("id", "p1", "name", "Jane", "hospital", "UMCG"));

    int count =
        patients.save(
            List.of(row("id", "p1", "doctor", "d1"), row("id", "p2", "doctor", "d1")),
            UpdateMode.UPDATE);

    assertEquals(2, count);
    List<Row> rows = patients.retrieveRows();
    assertEquals(2, rows.size());
    Row existing = rowWithId(rows, "p1");
    assertEquals("Jane", existing.getString("name"));
    assertEquals("UMCG", existing.getString("hospital"));
    assertEquals("d1", existing.getString("doctor"));
    Row inserted = rowWithId(rows, "p2");
    assertNull(inserted.getString("name"));
    assertEquals("d1", inserted.getString("doctor"));
  }

  @Test
  void givenInheritedTable_whenSavingWithOmittedColumns_thenParentColumnsArePreserved() {
    Table inpatients =
        schema.create(table("Inpatient").setInheritName("Patient").add(column("ward")));
    inpatients.insert(row("id", "p1", "name", "Jane", "hospital", "UMCG", "ward", "A1"));

    inpatients.save(
        List.of(row("id", "p1", "ward", "B2"), row("id", "p2", "name", "John", "ward", "C3")),
        UpdateMode.UPDATE);

    List<Row> rows = inpatients.retrieveRows();
    assertEquals(2, rows.size());
    Row existing = rowWithId(rows, "p1");
    assertEquals("Jane", existing.getString("name"));
    assertEquals("UMCG", existing.getString("hospital"));
    assertEquals("B2", existing.getString("ward"));
    Row inserted = rowWithId(rows, "p2");
    assertEquals("John", inserted.getString("name"));
    assertEquals("C3", inserted.getString("ward"));
  }

  @Test
  void givenRefInPrimaryKey_whenSavingWithOmittedColumns_thenOmittedColumnsArePreserved() {
    Table departments =
        schema.create(
            table(
                "Department",
                column("hospital").setType(REF).setRefTable("Hospital").setPkey(),
                column("name").setPkey(),
                column("floor").setType(INT),
                column("head").setType(REF).setRefTable("Doctor")));
    departments.insert(row("hospital", "UMCG", "name", "Cardiology", "floor", 3, "head", "d1"));

    departments.save(
        List.of(
            row("hospital", "UMCG", "name", "Cardiology", "floor", 4),
            row("hospital", "UMCG", "name", "Oncology", "floor", 5)),
        UpdateMode.UPDATE);

    List<Row> rows = departments.retrieveRows();
    assertEquals(2, rows.size());
    Row cardiology =
        rows.stream()
            .filter(r -> "Cardiology".equals(r.getString("name")))
            .findFirst()
            .orElseThrow();
    assertEquals(4, cardiology.getInteger("floor"));
    assertEquals("d1", cardiology.getString("head"));
  }

  @Test
  void givenFileColumn_whenSavingWithoutFileColumn_thenFileIsPreserved() {
    Table patients = schema.getTable("Patient");
    patients.getMetadata().add(column("scan").setType(FILE));
    patients.insert(row("id", "p1", "name", "Jane", "scan", getFile("testfiles/molgenis.png")));

    patients.save(List.of(row("id", "p1", "name", "Janet")), UpdateMode.UPDATE);

    Row patient = patients.retrieveRows(INCLUDE_FILE_CONTENTS).getFirst();
    assertEquals("Janet", patient.getString("name"));
    assertEquals("molgenis.png", patient.getString("scan_filename"));
    assertNotNull(patient.getBinary("scan_contents"));
  }

  @Test
  void givenOverwriteMode_whenSavingWithOmittedColumns_thenOmittedColumnsAreCleared() {
    Table patients = schema.getTable("Patient");
    patients.insert(row("id", "p1", "name", "Jane", "doctor", "d1"));

    patients.save(List.of(row("id", "p1", "doctor", "d1")), UpdateMode.OVERWRITE);

    assertNull(patients.retrieveRows().getFirst().getString("name"));
  }

  private static Row rowWithId(List<Row> rows, String id) {
    return rows.stream().filter(r -> id.equals(r.getString("id"))).findFirst().orElseThrow();
  }
}
