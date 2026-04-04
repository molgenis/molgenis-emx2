package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.BETWEEN;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestSubtables {

  private static final String SCHEMA_PICK_ONE = "TestSubtablesPickOne";
  private static final String SCHEMA_PICK_MANY = "TestSubtablesPickMany";

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Schema createPickOneSchema() {
    Schema schema = db.dropCreateSchema(SCHEMA_PICK_ONE);

    schema.create(
        table("Experiments")
            .add(column("experiment id").setType(STRING).setPkey())
            .add(column("name").setType(STRING))
            .add(column("date").setType(DATE))
            .add(column("experiment type").setType(EXTENSION)));

    schema.create(
        table("sampling")
            .setTableType(TableType.INTERNAL)
            .setInheritNames("Experiments")
            .add(column("sample type").setType(STRING))
            .add(column("tissue type").setType(STRING)));

    schema.create(
        table("sequencing")
            .setTableType(TableType.INTERNAL)
            .setInheritNames("Experiments")
            .add(column("library strategy").setType(STRING))
            .add(column("read length").setType(INT)));

    schema.create(
        table("WGS")
            .setInheritNames("sampling", "sequencing")
            .add(column("coverage").setType(DECIMAL)));

    schema.create(
        table("WES")
            .setInheritNames("sampling", "sequencing")
            .add(column("capture kit").setType(STRING)));

    schema.create(
        table("Imaging")
            .setInheritNames("Experiments")
            .add(column("modality").setType(STRING))
            .add(column("body part").setType(STRING)));

    return schema;
  }

  private Schema createPickManySchema() {
    Schema schema = db.dropCreateSchema(SCHEMA_PICK_MANY);

    schema.create(
        table("Observations")
            .add(column("obs id").setType(STRING).setPkey())
            .add(column("date").setType(DATE))
            .add(column("observation types").setType(EXTENSION_ARRAY)));

    schema.create(
        table("Dermatology")
            .setInheritNames("Observations")
            .add(column("BSA").setType(DECIMAL))
            .add(column("lesion type").setType(STRING)));

    schema.create(
        table("Neurology")
            .setInheritNames("Observations")
            .add(column("motor score").setType(INT))
            .add(column("cognitive score").setType(INT)));

    return schema;
  }

  // -------------------------------------------------------------------------
  // Table Creation Tests
  // -------------------------------------------------------------------------

  @Test
  public void testSubtableCreation() {
    Schema schema = createPickOneSchema();

    assertNotNull(schema.getTable("Experiments"), "Experiments table should exist");
    assertNotNull(schema.getTable("sampling"), "sampling block table should exist");
    assertNotNull(schema.getTable("sequencing"), "sequencing block table should exist");
    assertNotNull(schema.getTable("WGS"), "WGS subtable should exist");
    assertNotNull(schema.getTable("Imaging"), "Imaging subtable should exist");

    TableMetadata experimentsMeta = schema.getTable("Experiments").getMetadata();
    assertNull(
        experimentsMeta.getColumn(MG_TABLECLASS),
        "Experiments should not have mg_tableclass column");

    TableMetadata samplingMeta = schema.getTable("sampling").getMetadata();
    assertNull(samplingMeta.getColumn(MG_TABLECLASS), "sampling should not have mg_tableclass");

    TableMetadata wgsMeta = schema.getTable("WGS").getMetadata();
    assertNull(wgsMeta.getColumn(MG_TABLECLASS), "WGS should not have mg_tableclass");

    assertNotNull(experimentsMeta.getColumn("experiment type"), "EXTENSION column should exist");
    assertEquals(
        EXTENSION,
        experimentsMeta.getColumn("experiment type").getColumnType(),
        "experiment type should be EXTENSION type");

    assertTrue(
        ((SqlSchema) schema)
            .getJooq()
            .meta()
            .getSchemas(SCHEMA_PICK_ONE)
            .get(0)
            .getTable("sampling")
            .getReferences()
            .get(0)
            .toString()
            .contains("references \"" + SCHEMA_PICK_ONE + "\".\"Experiments\""),
        "sampling should have FK to Experiments");

    assertTrue(
        ((SqlSchema) schema)
                .getJooq()
                .meta()
                .getSchemas(SCHEMA_PICK_ONE)
                .get(0)
                .getTable("WGS")
                .getReferences()
                .stream()
                .anyMatch(ref -> ref.toString().contains("\"" + SCHEMA_PICK_ONE + "\"")),
        "WGS should have FK reference");
  }

  @Test
  public void testProfilesColumnCreation() {
    Schema schema = createPickManySchema();

    TableMetadata observationsMeta = schema.getTable("Observations").getMetadata();
    assertNotNull(
        observationsMeta.getColumn("observation types"), "EXTENSION_ARRAY column should exist");
    assertEquals(
        EXTENSION_ARRAY,
        observationsMeta.getColumn("observation types").getColumnType(),
        "observation types should be EXTENSION_ARRAY type");

    assertNull(
        schema.getTable("Dermatology").getMetadata().getColumn(MG_TABLECLASS),
        "Dermatology should not have mg_tableclass");
    assertNull(
        schema.getTable("Neurology").getMetadata().getColumn(MG_TABLECLASS),
        "Neurology should not have mg_tableclass");

    assertTrue(
        ((SqlSchema) schema)
            .getJooq()
            .meta()
            .getSchemas(SCHEMA_PICK_MANY)
            .get(0)
            .getTable("Dermatology")
            .getReferences()
            .get(0)
            .toString()
            .contains("references \"" + SCHEMA_PICK_MANY + "\".\"Observations\""),
        "Dermatology should have FK to Observations");
  }

  // -------------------------------------------------------------------------
  // Row Management Tests (pick-one)
  // -------------------------------------------------------------------------

  @Test
  public void testInsertWithProfile() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP001")
            .setString("name", "WGS run 1")
            .setDate("date", LocalDate.of(2024, 1, 15))
            .setString("experiment type", "WGS")
            .setString("sample type", "blood")
            .setString("tissue type", "whole blood")
            .setString("library strategy", "WGS")
            .setInt("read length", 150)
            .setDecimal("coverage", 30.0));

    assertEquals(1, experiments.retrieveRows().size(), "Experiments should have 1 row");
    assertEquals(
        1, schema.getTable("sampling").retrieveRows().size(), "sampling should have 1 row for WGS");
    assertEquals(
        1,
        schema.getTable("sequencing").retrieveRows().size(),
        "sequencing should have 1 row for WGS");
    assertEquals(1, schema.getTable("WGS").retrieveRows().size(), "WGS should have 1 row");
    assertEquals(0, schema.getTable("Imaging").retrieveRows().size(), "Imaging should be empty");

    Row samplingRow = schema.getTable("sampling").retrieveRows().get(0);
    assertEquals("blood", samplingRow.getString("sample type"));

    Row wgsRow = schema.getTable("WGS").retrieveRows().get(0);
    assertEquals(30.0, wgsRow.getDecimal("coverage"), 0.001);
  }

  @Test
  public void testInsertWithNullProfile() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_NULL")
            .setString("name", "Untyped experiment")
            .setDate("date", LocalDate.of(2024, 2, 1)));

    assertEquals(1, experiments.retrieveRows().size(), "Experiments should have 1 row");
    assertEquals(0, schema.getTable("sampling").retrieveRows().size(), "sampling should be empty");
    assertEquals(
        0, schema.getTable("sequencing").retrieveRows().size(), "sequencing should be empty");
    assertEquals(0, schema.getTable("WGS").retrieveRows().size(), "WGS should be empty");
    assertEquals(0, schema.getTable("Imaging").retrieveRows().size(), "Imaging should be empty");
  }

  @Test
  public void testUpdateProfileChange() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    Row row =
        new Row()
            .setString("experiment id", "EXP_CHANGE")
            .setString("name", "Profile change test")
            .setDate("date", LocalDate.of(2024, 3, 1))
            .setString("experiment type", "WGS")
            .setString("sample type", "saliva")
            .setString("library strategy", "WGS")
            .setInt("read length", 100)
            .setDecimal("coverage", 25.0);
    experiments.insert(row);

    assertEquals(1, schema.getTable("WGS").retrieveRows().size());
    assertEquals(1, schema.getTable("sampling").retrieveRows().size());
    assertEquals(0, schema.getTable("Imaging").retrieveRows().size());

    row.setString("experiment type", "Imaging")
        .setString("modality", "MRI")
        .setString("body part", "brain");
    experiments.update(row);

    assertEquals(0, schema.getTable("WGS").retrieveRows().size(), "WGS row should be deleted");
    assertEquals(
        0, schema.getTable("sampling").retrieveRows().size(), "sampling row should be deleted");
    assertEquals(
        0, schema.getTable("sequencing").retrieveRows().size(), "sequencing row should be deleted");
    assertEquals(1, schema.getTable("Imaging").retrieveRows().size(), "Imaging row should exist");
    assertEquals(1, experiments.retrieveRows().size(), "Experiments row should still exist");

    Row experimentRow = experiments.retrieveRows().get(0);
    assertEquals("Imaging", experimentRow.getString("experiment type"));
  }

  @Test
  public void testDeleteParentCascades() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_CASCADE")
            .setString("name", "Cascade test")
            .setDate("date", LocalDate.of(2024, 4, 1))
            .setString("experiment type", "WGS")
            .setString("sample type", "tissue")
            .setString("library strategy", "WGS")
            .setInt("read length", 150)
            .setDecimal("coverage", 40.0));

    assertEquals(1, schema.getTable("WGS").retrieveRows().size());
    assertEquals(1, schema.getTable("sampling").retrieveRows().size());
    assertEquals(1, schema.getTable("sequencing").retrieveRows().size());

    experiments.delete(row("experiment id", "EXP_CASCADE"));

    assertEquals(0, experiments.retrieveRows().size(), "Experiments row should be gone");
    assertEquals(0, schema.getTable("WGS").retrieveRows().size(), "WGS cascaded delete");
    assertEquals(0, schema.getTable("sampling").retrieveRows().size(), "sampling cascaded delete");
    assertEquals(
        0, schema.getTable("sequencing").retrieveRows().size(), "sequencing cascaded delete");
  }

  @Test
  public void testInvalidProfileName() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    assertThrows(
        MolgenisException.class,
        () ->
            experiments.insert(
                new Row()
                    .setString("experiment id", "EXP_INVALID")
                    .setString("experiment type", "NonExistent")),
        "Should throw for unknown profile name");
  }

  // -------------------------------------------------------------------------
  // Row Management Tests (pick-many)
  // -------------------------------------------------------------------------

  @Test
  public void testInsertWithMultipleProfiles() {
    Schema schema = createPickManySchema();
    Table observations = schema.getTable("Observations");

    observations.insert(
        new Row()
            .setString("obs id", "OBS001")
            .setDate("date", LocalDate.of(2024, 5, 1))
            .setStringArray("observation types", "Dermatology", "Neurology")
            .setDecimal("BSA", 12.5)
            .setString("lesion type", "plaque")
            .setInt("motor score", 8)
            .setInt("cognitive score", 25));

    assertEquals(1, observations.retrieveRows().size(), "Observations should have 1 row");
    assertEquals(
        1, schema.getTable("Dermatology").retrieveRows().size(), "Dermatology should have 1 row");
    assertEquals(
        1, schema.getTable("Neurology").retrieveRows().size(), "Neurology should have 1 row");
  }

  @Test
  public void testRemoveProfileEntry() {
    Schema schema = createPickManySchema();
    Table observations = schema.getTable("Observations");

    Row row =
        new Row()
            .setString("obs id", "OBS_REMOVE")
            .setDate("date", LocalDate.of(2024, 6, 1))
            .setStringArray("observation types", "Dermatology", "Neurology")
            .setDecimal("BSA", 5.0)
            .setString("lesion type", "papule")
            .setInt("motor score", 6)
            .setInt("cognitive score", 20);
    observations.insert(row);

    assertEquals(1, schema.getTable("Dermatology").retrieveRows().size());
    assertEquals(1, schema.getTable("Neurology").retrieveRows().size());

    row.setStringArray("observation types", "Dermatology");
    observations.update(row);

    assertEquals(
        1, schema.getTable("Dermatology").retrieveRows().size(), "Dermatology row should remain");
    assertEquals(
        0, schema.getTable("Neurology").retrieveRows().size(), "Neurology row should be deleted");
  }

  @Test
  public void testSetEmptyProfiles() {
    Schema schema = createPickManySchema();
    Table observations = schema.getTable("Observations");

    Row row =
        new Row()
            .setString("obs id", "OBS_EMPTY")
            .setDate("date", LocalDate.of(2024, 7, 1))
            .setStringArray("observation types", "Dermatology", "Neurology")
            .setDecimal("BSA", 3.0)
            .setInt("motor score", 4);
    observations.insert(row);

    assertEquals(1, schema.getTable("Dermatology").retrieveRows().size());
    assertEquals(1, schema.getTable("Neurology").retrieveRows().size());

    row.set("observation types", null);
    observations.update(row);

    assertEquals(
        0, schema.getTable("Dermatology").retrieveRows().size(), "Dermatology should be cleared");
    assertEquals(
        0, schema.getTable("Neurology").retrieveRows().size(), "Neurology should be cleared");
    assertEquals(1, observations.retrieveRows().size(), "Observations row should still exist");
  }

  // -------------------------------------------------------------------------
  // Shared Blocks
  // -------------------------------------------------------------------------

  @Test
  public void testSharedBlockIndependentRows() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_WGS")
            .setString("name", "WGS experiment")
            .setDate("date", LocalDate.of(2024, 8, 1))
            .setString("experiment type", "WGS")
            .setString("sample type", "blood")
            .setString("library strategy", "WGS")
            .setInt("read length", 150)
            .setDecimal("coverage", 35.0));

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_WES")
            .setString("name", "WES experiment")
            .setDate("date", LocalDate.of(2024, 8, 2))
            .setString("experiment type", "WES")
            .setString("sample type", "saliva")
            .setString("library strategy", "WES")
            .setInt("read length", 100)
            .setString("capture kit", "Agilent SureSelect"));

    assertEquals(2, experiments.retrieveRows().size(), "Both experiments should exist");
    assertEquals(
        2, schema.getTable("sampling").retrieveRows().size(), "sampling has 2 independent rows");
    assertEquals(
        2,
        schema.getTable("sequencing").retrieveRows().size(),
        "sequencing has 2 independent rows");
    assertEquals(1, schema.getTable("WGS").retrieveRows().size(), "WGS has 1 row");
    assertEquals(1, schema.getTable("WES").retrieveRows().size(), "WES has 1 row");
  }

  // -------------------------------------------------------------------------
  // Querying
  // -------------------------------------------------------------------------

  @Test
  public void testQueryParentJoinsChildren() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_Q1")
            .setString("name", "Query WGS")
            .setDate("date", LocalDate.of(2024, 9, 1))
            .setString("experiment type", "WGS")
            .setString("sample type", "blood")
            .setString("library strategy", "WGS")
            .setInt("read length", 150)
            .setDecimal("coverage", 45.0));

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_Q2")
            .setString("name", "Query Imaging")
            .setDate("date", LocalDate.of(2024, 9, 2))
            .setString("experiment type", "Imaging")
            .setString("modality", "CT")
            .setString("body part", "chest"));

    List<Row> rows =
        experiments
            .query()
            .select(
                s("experiment id"),
                s("experiment type"),
                s("coverage"),
                s("modality"),
                s("body part"),
                s("sample type"))
            .retrieveRows();

    assertEquals(2, rows.size(), "Should return both rows");

    Row wgsRow =
        rows.stream().filter(r -> "EXP_Q1".equals(r.getString("experiment id"))).findFirst().get();
    Row imagingRow =
        rows.stream().filter(r -> "EXP_Q2".equals(r.getString("experiment id"))).findFirst().get();

    assertEquals(45.0, wgsRow.getDecimal("coverage"), 0.001, "WGS row should have coverage");
    assertEquals("blood", wgsRow.getString("sample type"), "WGS row should have sample type");
    assertNull(wgsRow.getString("modality"), "WGS row should not have modality");

    assertEquals("CT", imagingRow.getString("modality"), "Imaging row should have modality");
    assertEquals("chest", imagingRow.getString("body part"), "Imaging row should have body part");
    assertNull(imagingRow.getDecimal("coverage"), "Imaging row should not have coverage");
  }

  @Test
  public void testFilterOnSubtableColumn() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_FILT1")
            .setString("experiment type", "WGS")
            .setString("sample type", "blood")
            .setString("library strategy", "WGS")
            .setInt("read length", 150)
            .setDecimal("coverage", 10.0));

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_FILT2")
            .setString("experiment type", "WGS")
            .setString("sample type", "tissue")
            .setString("library strategy", "WGS")
            .setInt("read length", 150)
            .setDecimal("coverage", 30.0));

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_FILT3")
            .setString("experiment type", "Imaging")
            .setString("modality", "PET")
            .setString("body part", "whole body"));

    List<Row> highCoverageRows =
        experiments.query().where(f("coverage", BETWEEN, 20.01, 1e308)).retrieveRows();

    assertEquals(1, highCoverageRows.size(), "Only one WGS row has coverage > 20");
    assertEquals("EXP_FILT2", highCoverageRows.get(0).getString("experiment id"));
  }

  @Test
  public void testQuerySubtableDirectly() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_DIRECT1")
            .setString("experiment type", "WGS")
            .setString("sample type", "blood")
            .setString("library strategy", "WGS")
            .setInt("read length", 150)
            .setDecimal("coverage", 55.0));

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_DIRECT2")
            .setString("experiment type", "Imaging")
            .setString("modality", "MRI")
            .setString("body part", "spine"));

    Table wgsTable = schema.getTable("WGS");
    List<Row> wgsRows = wgsTable.retrieveRows();

    assertEquals(1, wgsRows.size(), "WGS table should only contain WGS rows");
    assertEquals("EXP_DIRECT1", wgsRows.get(0).getString("experiment id"));
    assertNotNull(wgsRows.get(0).getDecimal("coverage"), "WGS row should have coverage");
  }

  // -------------------------------------------------------------------------
  // Metadata Persistence
  // -------------------------------------------------------------------------

  @Test
  public void testInheritNamesPersisted() {
    createPickOneSchema();

    Schema reloaded = db.getSchema(SCHEMA_PICK_ONE);

    TableMetadata wgsMeta = reloaded.getTable("WGS").getMetadata();
    String[] inheritNames = wgsMeta.getInheritNames();
    assertNotNull(inheritNames, "WGS inheritName should not be null");
    assertEquals(2, inheritNames.length, "WGS should inherit from 2 blocks");

    List<String> names = List.of(inheritNames);
    assertTrue(names.contains("sampling"), "WGS should inherit sampling");
    assertTrue(names.contains("sequencing"), "WGS should inherit sequencing");

    TableMetadata imagingMeta = reloaded.getTable("Imaging").getMetadata();
    String[] imagingInherit = imagingMeta.getInheritNames();
    assertNotNull(imagingInherit);
    assertEquals(1, imagingInherit.length);
    assertEquals("Experiments", imagingInherit[0]);
  }

  @Test
  public void testInternalTableTypePersisted() {
    createPickOneSchema();

    Schema reloaded = db.getSchema(SCHEMA_PICK_ONE);

    assertEquals(
        TableType.INTERNAL,
        reloaded.getTable("sampling").getMetadata().getTableType(),
        "sampling should have TableType.INTERNAL after reload");
    assertEquals(
        TableType.INTERNAL,
        reloaded.getTable("sequencing").getMetadata().getTableType(),
        "sequencing should have TableType.INTERNAL after reload");
    assertEquals(
        TableType.DATA,
        reloaded.getTable("WGS").getMetadata().getTableType(),
        "WGS should have TableType.DATA");
  }

  // -------------------------------------------------------------------------
  // Search
  // -------------------------------------------------------------------------

  @Test
  public void testSearchFindsColumnsFromAllParents() {
    Schema schema = createPickOneSchema();
    Table experiments = schema.getTable("Experiments");

    experiments.insert(
        new Row()
            .setString("experiment id", "EXP_SEARCH")
            .setString("name", "Searchable experiment")
            .setString("experiment type", "WGS")
            .setString("sample type", "UniqueBloodSample")
            .setString("library strategy", "UniqueLibStrategy")
            .setInt("read length", 150)
            .setDecimal("coverage", 30.0));

    // WGS inherits sampling -> Experiments; searching WGS should find data in parent columns
    Table wgs = schema.getTable("WGS");

    List<Row> foundViaExperimentsColumn = wgs.search("Searchable experiment").retrieveRows();
    assertEquals(
        1, foundViaExperimentsColumn.size(), "WGS search should find data in Experiments columns");

    List<Row> foundViaSamplingColumn = wgs.search("UniqueBloodSample").retrieveRows();
    assertEquals(
        1, foundViaSamplingColumn.size(), "WGS search should find data in sampling parent columns");

    List<Row> foundViaSequencingColumn = wgs.search("UniqueLibStrategy").retrieveRows();
    assertEquals(
        1,
        foundViaSequencingColumn.size(),
        "WGS search should find data in sequencing parent columns");
  }

  // -------------------------------------------------------------------------
  // Inheritance Validation
  // -------------------------------------------------------------------------

  @Test
  void testMultiParentMustShareSameRoot() {
    Schema schema = db.dropCreateSchema("TestSubtablesMultiRoot");

    schema.create(table("RootA").add(column("id").setType(STRING).setPkey()));
    schema.create(table("RootB").add(column("id").setType(STRING).setPkey()));
    schema.create(table("ChildA").setInheritNames("RootA").add(column("fieldA").setType(STRING)));

    assertThrows(
        MolgenisException.class,
        () ->
            schema.create(
                table("NewTable")
                    .setInheritNames("ChildA", "RootB")
                    .add(column("extra").setType(STRING))),
        "Should throw when parents have different root tables");

    db.dropSchema("TestSubtablesMultiRoot");
  }

  @Test
  void testCannotRerootTableWithSubclasses() {
    Schema schema = db.dropCreateSchema("TestSubtablesReroot");

    schema.create(table("TableA").add(column("id").setType(STRING).setPkey()));
    schema.create(table("TableB").setInheritNames("TableA").add(column("fieldB").setType(STRING)));
    schema.create(table("TableC").add(column("id").setType(STRING).setPkey()));

    assertThrows(
        MolgenisException.class,
        () -> schema.getTable("TableA").getMetadata().setInheritNames("TableC"),
        "Should throw when table has subclasses and would become a child");

    db.dropSchema("TestSubtablesReroot");
  }

  // -------------------------------------------------------------------------
  // Backward Compatibility
  // -------------------------------------------------------------------------

  @Test
  public void testExistingInheritsStillWork() {
    Schema schema = db.dropCreateSchema("TestSubtablesCompat");

    Table person =
        schema.create(
            table("Person")
                .add(column("fullName").setPkey())
                .add(column("birthDate").setType(DATE)));

    Table employee =
        schema.create(
            table("Employee").setInheritNames(person.getName()).add(column("salary").setType(INT)));

    assertNotNull(
        employee.getMetadata().getColumn(MG_TABLECLASS),
        "Old-style extends should still create mg_tableclass");

    Table personTable = schema.getTable("Person");
    Table employeeTable = schema.getTable("Employee");

    employeeTable.insert(
        new Row()
            .setString("fullName", "Jane Doe")
            .setDate("birthDate", LocalDate.of(1990, 6, 15))
            .setInt("salary", 50000));

    assertEquals(1, personTable.retrieveRows().size(), "Person should have 1 row via inheritance");
    assertEquals(1, employeeTable.retrieveRows().size(), "Employee should have 1 row");

    assertEquals(
        1,
        personTable
            .query()
            .where(f(MG_TABLECLASS, EQUALS, schema.getName() + ".Employee"))
            .retrieveRows()
            .size(),
        "Filtering by mg_tableclass should still work");

    employeeTable.getMetadata().drop();
    personTable.getMetadata().drop();
  }
}
