package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.LIKE;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestInherits {

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void testExtends() {

    db.dropSchemaIfExists(
        TestInherits.class.getSimpleName() + "1"); // is a related schema, drop that first
    Schema s = db.dropCreateSchema(TestInherits.class.getSimpleName());

    Table person = s.create(table("Person"));

    // test if fails if no primary key
    try {
      s.create(table("Employee").setInheritName(person.getName()));
      fail("Should fail because does not have pkey");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    try {
      s.create(table("Employee").setInheritName("fake_table"));
      fail("Should fail");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // set pkey and a property
    person.getMetadata().add(column("fullName").setPkey());
    person.getMetadata().add(column("birthDate").setType(DATE));

    // create first extended table
    Table employee =
        s.create(
            table("Employee").setInheritName(person.getName()).add(column("salary").setType(INT)));

    // check that mg_tableclass column doesn't have a default (regression #2936)
    assertNull(employee.getMetadata().getColumn(MG_TABLECLASS).getDefaultValue());

    // check that reference to parent class exists (regression #3144)
    assertTrue(
        ((SqlSchema) s)
            .getJooq()
            .meta()
            .getSchemas("TestInherits")
            .get(0)
            .getTable("Employee")
            .getReferences()
            .get(0)
            .toString()
            .contains("references \"TestInherits\".\"Person\""));

    Table manager =
        s.create(
            table("Manager")
                .setInheritName("Employee")
                .add(column("directs").setType(REF_ARRAY).setRefTable("Employee")));

    Schema otherSchema = db.createSchema(TestInherits.class.getSimpleName() + "1");
    Table ceo =
        otherSchema.create(
            table("CEO")
                .setInheritName("Manager")
                .setImportSchema(s.getName())
                .add(column("title")));

    // try to add column that already exists in parent
    try {
      employee.getMetadata().add(column("birthDate").setType(DATE));
      fail("should fail: cannot add column to subclass that already exists in superclass");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to add column in superclass that already exists in any subclass
    try {
      person.getMetadata().add(column("salary").setType(DATE));
      fail("should fail: cannot add column in superclass to name that already exists in subclass");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to rename column in superclass that already exists in any subclass
    try {
      person.getMetadata().alterColumn("birthDate", column("salary").setType(DATE));
      fail("should fail: cannot rename column to superclass that already exists in subclass");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to alter name in superclass that already exists in any subclass IN OTHER SCHEMA
    try {
      person.getMetadata().add(column("title").setType(DATE));
      fail(
          "should fail: cannot add column in superclass to name that already exists in subclass IN OTHER SCHEMA");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to extend twice
    try {
      manager.getMetadata().setInheritName("Student");
      fail("should fail: cannot extend another table");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // create another extended table
    s.create(
        table("Student").setInheritName(person.getName()).add(column("averageGrade").setType(INT)));

    // test insert, retrieve
    Table studentTable = s.getTable("Student");
    studentTable.insert(new Row().setString("fullName", "Donald Duck").setInt("averageGrade", 10));

    Table employeeTable = s.getTable("Employee");
    employeeTable.insert(
        new Row()
            .setString("fullName", "Katrien Duck")
            .setInt("salary", 100)
            .setDate("birthDate", LocalDate.of(2000, 12, 01)));

    Table ceoTable = otherSchema.getTable("CEO"); // we use CEO to make it more difficult
    Row managerRow =
        new Row()
            .setString("fullName", "Dagobert Duck")
            .setInt("salary", 1000000)
            .setDate("birthDate", LocalDate.of(2000, 12, 01))
            .setStringArray("directs", "Katrien Duck");
    ceoTable.insert(managerRow);

    Table personTable = s.getTable("Person");
    assertEquals(3, personTable.retrieveRows().size());
    assertEquals(1, studentTable.retrieveRows().size());
    assertEquals(2, employeeTable.retrieveRows().size());
    assertEquals(1, ceoTable.retrieveRows().size());

    // retrieve
    assertEquals(
        (Integer) 1000000,
        employeeTable
            .query()
            .select(s("salary"))
            .where(f("fullName", EQUALS, "Dagobert Duck"))
            .retrieveRows()
            .get(0)
            .getInteger("salary"));

    // TODO test RLS

    // test search
    assertEquals(1, personTable.search("Dagobert").retrieveRows().size());
    assertEquals(1, employeeTable.search("Dagobert").retrieveRows().size());

    // update
    managerRow.setDate("birthDate", LocalDate.of(1900, 12, 01));
    ceoTable.update(managerRow);
    assertEquals(LocalDate.of(1900, 12, 01), ceoTable.retrieveRows().get(0).getDate("birthDate"));

    // test graph query
    // simple
    String result = ceoTable.select(s("fullName"), s("salary")).retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Dagobert"));
    // nested relation
    result =
        ceoTable.select(s("fullName"), s("salary"), s("directs", s("fullName"))).retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));
    // filtering (erroroneous)
    result =
        ceoTable
            .select(s("fullName"), s("salary"), s("directs", s("fullName")))
            .where(f("directs", f("fullName", LIKE, "Pietje")))
            .retrieveJSON();
    System.out.println(result);
    assertFalse(result.contains("Katrien"));
    // filtering (correct)
    result =
        ceoTable
            .select(s("fullName"), s("salary"), s("directs", s("fullName")))
            .where(f("directs", f("fullName", LIKE, "Katrien")))
            .retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));

    // filtering on mg_tableclass
    assertEquals(
        1,
        personTable
            .query()
            .where(f(MG_TABLECLASS, EQUALS, s.getName() + ".Employee"))
            .retrieveRows()
            .size());

    // verify that you can't delete a parent row when having child rows that are refered to,
    // regression #3144
    try {
      employee.delete(row("fullName", "Katrien Duck"));
      // refered to by the manager
      fail("should not be able to delete inherited rows when having reference");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("foreign key"));
      System.out.println("Errored correctly: " + e.getMessage());
    }

    // verify that you can't delete a parent row when having child rows that are refered to,
    // also not via the 'root' table
    // regression #3144
    try {
      person.delete(row("fullName", "Katrien Duck"));
      // refered to by the manager
      fail("should not be able to delete inherited rows when having reference");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("foreign key"));
      System.out.println("Errored correctly: " + e.getMessage());
    }

    // delete
    ceoTable.delete(managerRow);
    assertEquals(2, personTable.retrieveRows().size());
    assertEquals(1, studentTable.retrieveRows().size());
    assertEquals(1, employeeTable.retrieveRows().size());
    assertEquals(0, ceoTable.retrieveRows().size());

    // test multi add
    personTable.insert(
        row(MG_TABLECLASS, "Manager", "fullName", "popeye"),
        row(MG_TABLECLASS, "Employee", "fullName", "goofy"));
    assertEquals(4, personTable.retrieveRows().size());
    assertEquals(1, studentTable.retrieveRows().size());
    assertEquals(3, employeeTable.retrieveRows().size());
    assertEquals(1, manager.retrieveRows().size());
    assertEquals(0, ceoTable.retrieveRows().size());

    try {
      personTable.insert(row(MG_TABLECLASS, "Wrong", "fullName", "popeye"));
      fail("should error");
    } catch (Exception e) {
      System.out.println("Errored correctly: " + e.getMessage());
    }

    try {
      personTable.insert(row(MG_TABLECLASS, "Blaat.Wrong", "fullName", "popeye"));
      fail("should error");
    } catch (Exception e) {
      System.out.println("Errored correctly: " + e.getMessage());
    }

    // test that we cannot overwrite record from supertable using save
    // fixes #
    personTable.save(row("fullName", "testDuplicate"));
    try {
      studentTable.save(row("fullName", "testDuplicate"));
      fail("should not be able to overwrite existing person in super table person");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Duplicate key"));
      System.out.println("Errored correctly: " + e.getMessage());
    }

    // try to rename pkey column, should also change the pkey columns of sublcasss
    person
        .getMetadata()
        .alterColumn("fullName", person.getMetadata().getColumn("fullName").setName("displayName"));
    assertEquals(
        "displayName",
        ((SqlSchema) s)
            .getJooq()
            .meta()
            .getSchemas("TestInherits")
            .get(0)
            .getTable("Employee")
            .getPrimaryKey()
            .getFields()
            .get(0)
            .getName());

    // try to add faulty ref-table
    assertThrows(
        MolgenisException.class,
        () -> {
          s.create(table("will fail", column("id").setPkey(), column("ref").setType(REF)));
        });

    // can also drop the table without errors when trigger is removed
    ceoTable.getMetadata().drop();
    manager.getMetadata().drop();
    employeeTable.getMetadata().drop();
    studentTable.getMetadata().drop();
    personTable.getMetadata().drop();
    // todo add test that trigger actually is deleted
  }

  /**
   * When inserting rows into a table (A) that inherits a different one (B), we first insert values
   * into B, then insert into A afterward, using the values of B. We do this to ensure generated IDs
   * are reused across all inherited tables. With this test, we want to ensure that the ordering of
   * these values stay intact.
   */
  @Test
  void whenInsertingInheritedTable_thenReuseInheritedValues() {
    Schema schema = db.dropCreateSchema(TestInherits.class.getSimpleName() + "_insert");
    schema.create(
        table(
            "test_inheritance_A",
            new Column("a_id")
                .setType(ColumnType.AUTO_ID)
                .setComputed("${mg_autoid(length=10, format=numbers)}")
                .setPkey(),
            new Column("a_label").setType(ColumnType.INT)));

    Table tableB =
        schema.create(
            table("test_inheritance_B", new Column("b_label").setType(ColumnType.INT))
                .setInheritName("test_inheritance_A"));

    List<Row> rows =
        IntStream.range(0, 100).boxed().map(i -> Row.row("a_label", i, "b_label", i)).toList();

    tableB.insert(rows);

    for (Row row : tableB.retrieveRows()) {
      assertEquals(row.getInteger("a_label"), row.getInteger("b_label"));
    }
  }

  @Test
  void reimportWithDanglingParentGivesMessage() {
    String parentSchemaName = TestInherits.class.getSimpleName() + "_reimport_parent";
    String childSchemaName = TestInherits.class.getSimpleName() + "_reimport_child";
    db.dropSchemaIfExists(childSchemaName);
    Schema parentSchema = db.dropCreateSchema(parentSchemaName);
    parentSchema.create(table("Shape", column("name").setPkey()));
    Schema childSchema = db.createSchema(childSchemaName);
    childSchema.create(
        table("MyShape", column("size").setType(INT))
            .setImportSchema(parentSchemaName)
            .setInheritName("Shape"));

    SchemaMetadata reimport = new SchemaMetadata(childSchemaName);
    reimport.create(table("MyShape", column("size").setType(INT)).setInheritName("Shape"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> childSchema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot set tableExtends='Shape' in table '"
                    + childSchemaName
                    + ".MyShape': table not found or permission denied. If the table lives in another schema, provide refSchema."),
        exception.getMessage());
  }

  @Test
  void reimportWithMissingParentTableInOtherSchemaGivesMessage() {
    String parentSchemaName = TestInherits.class.getSimpleName() + "_missingtable_parent";
    String childSchemaName = TestInherits.class.getSimpleName() + "_missingtable_child";
    db.dropSchemaIfExists(childSchemaName);
    Schema parentSchema = db.dropCreateSchema(parentSchemaName);
    parentSchema.create(table("Shape", column("name").setPkey()));
    Schema childSchema = db.createSchema(childSchemaName);
    childSchema.create(
        table("MyShape", column("size").setType(INT))
            .setImportSchema(parentSchemaName)
            .setInheritName("Shape"));

    SchemaMetadata reimport = new SchemaMetadata(childSchemaName);
    reimport.create(
        table("MyShape", column("size").setType(INT))
            .setImportSchema(parentSchemaName)
            .setInheritName("NoSuchParentTable"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> childSchema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot set tableExtends='NoSuchParentTable' with refSchema='"
                    + parentSchemaName
                    + "' in table '"
                    + childSchemaName
                    + ".MyShape': table not found or permission denied."),
        exception.getMessage());
  }

  @Test
  void reimportWithMissingParentSchemaGivesMessage() {
    String parentSchemaName = TestInherits.class.getSimpleName() + "_missing_parent";
    String childSchemaName = TestInherits.class.getSimpleName() + "_missing_child";
    db.dropSchemaIfExists(childSchemaName);
    Schema parentSchema = db.dropCreateSchema(parentSchemaName);
    parentSchema.create(table("Shape", column("name").setPkey()));
    Schema childSchema = db.createSchema(childSchemaName);
    childSchema.create(
        table("MyShape", column("size").setType(INT))
            .setImportSchema(parentSchemaName)
            .setInheritName("Shape"));

    SchemaMetadata reimport = new SchemaMetadata(childSchemaName);
    reimport.create(
        table("MyShape", column("size").setType(INT))
            .setImportSchema("TestInheritsNoSuchSchema")
            .setInheritName("Shape"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> childSchema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot set tableExtends='Shape' with refSchema='TestInheritsNoSuchSchema' in table '"
                    + childSchemaName
                    + ".MyShape': schema 'TestInheritsNoSuchSchema' not found or permission denied."),
        exception.getMessage());
  }

  @Test
  void refSchemaChangeOnExistingTableIsRejected() {
    String parentA = TestInherits.class.getSimpleName() + "_refchange_a";
    String parentB = TestInherits.class.getSimpleName() + "_refchange_b";
    String childSchemaName = TestInherits.class.getSimpleName() + "_refchange_child";
    db.dropSchemaIfExists(childSchemaName);
    Schema schemaA = db.dropCreateSchema(parentA);
    schemaA.create(table("Shape", column("name").setPkey()));
    db.dropCreateSchema(parentB).create(table("Shape", column("name").setPkey()));
    Schema child = db.createSchema(childSchemaName);
    child.create(
        table("MyShape", column("size").setType(INT))
            .setImportSchema(parentA)
            .setInheritName("Shape"));

    SchemaMetadata reimport = new SchemaMetadata(childSchemaName);
    reimport.create(
        table("MyShape", column("size").setType(INT))
            .setImportSchema(parentB)
            .setInheritName("Shape"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> child.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot change refSchema of table '"
                    + childSchemaName
                    + ".MyShape': inheritance cannot be changed after the table is created."),
        exception.getMessage());
    assertEquals(
        parentA,
        db.getSchema(childSchemaName).getMetadata().getTableMetadata("MyShape").getImportSchema());
    db.getSchema(childSchemaName).getTable("MyShape").insert(row("name", "s1", "size", 3));
    assertEquals(1, db.getSchema(childSchemaName).getTable("MyShape").retrieveRows().size());
    assertDoesNotThrow(() -> db.dropSchema(childSchemaName));
  }

  @Test
  void tableExtendsChangeOnExistingTableIsRejected() {
    String schemaName = TestInherits.class.getSimpleName() + "_extendschange";
    Schema schema = db.dropCreateSchema(schemaName);
    schema.create(table("Shape", column("name").setPkey()));
    schema.create(table("OtherShape", column("othername").setPkey()));
    schema.create(table("MyShape", column("size").setType(INT)).setInheritName("Shape"));

    SchemaMetadata reimport = new SchemaMetadata(schemaName);
    reimport.create(table("Shape", column("name").setPkey()));
    reimport.create(table("OtherShape", column("othername").setPkey()));
    reimport.create(table("MyShape", column("size").setType(INT)).setInheritName("OtherShape"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> schema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot change tableExtends of table '"
                    + schemaName
                    + ".MyShape': inheritance cannot be changed after the table is created."),
        exception.getMessage());
    TableMetadata after = db.getSchema(schemaName).getMetadata().getTableMetadata("MyShape");
    assertEquals("Shape", after.getInheritName());
    assertTrue(after.getColumnNames().contains("name"), after.getColumnNames().toString());
    db.getSchema(schemaName).getTable("MyShape").insert(row("name", "s1", "size", 3));
    assertEquals(1, db.getSchema(schemaName).getTable("MyShape").retrieveRows().size());
    assertDoesNotThrow(() -> db.dropSchema(schemaName));
  }

  @Test
  void addingTableExtendsToExistingTableIsRejected() {
    String schemaName = TestInherits.class.getSimpleName() + "_addextends";
    Schema schema = db.dropCreateSchema(schemaName);
    schema.create(table("Shape", column("name").setPkey()));
    schema.create(table("MyShape", column("myid").setPkey(), column("size").setType(INT)));

    SchemaMetadata reimport = new SchemaMetadata(schemaName);
    reimport.create(table("Shape", column("name").setPkey()));
    reimport.create(table("MyShape", column("size").setType(INT)).setInheritName("Shape"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> schema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot set tableExtends of table '"
                    + schemaName
                    + ".MyShape': inheritance cannot be changed after the table is created."),
        exception.getMessage());

    TableMetadata after = db.getSchema(schemaName).getMetadata().getTableMetadata("MyShape");
    assertNull(after.getInheritName());
    assertFalse(after.getColumnNames().contains("name"), after.getColumnNames().toString());
    db.getSchema(schemaName).getTable("MyShape").insert(row("myid", "m1", "size", 3));
    assertEquals(1, db.getSchema(schemaName).getTable("MyShape").retrieveRows().size());
  }

  @Test
  void addingTableExtendsWithCollidingColumnIsRejectedWithSameMessage() {
    String schemaName = TestInherits.class.getSimpleName() + "_addextends_collide";
    Schema schema = db.dropCreateSchema(schemaName);
    schema.create(table("Shape", column("name").setPkey()));
    schema.create(table("MyShape", column("name").setPkey(), column("size").setType(INT)));

    SchemaMetadata reimport = new SchemaMetadata(schemaName);
    reimport.create(table("Shape", column("name").setPkey()));
    reimport.create(table("MyShape", column("size").setType(INT)).setInheritName("Shape"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> schema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot set tableExtends of table '"
                    + schemaName
                    + ".MyShape': inheritance cannot be changed after the table is created."),
        exception.getMessage());
    assertFalse(exception.getMessage().contains("already exists"), exception.getMessage());

    TableMetadata after = db.getSchema(schemaName).getMetadata().getTableMetadata("MyShape");
    assertNull(after.getInheritName());
    assertTrue(after.getColumnNames().contains("name"), after.getColumnNames().toString());
    db.getSchema(schemaName).getTable("MyShape").insert(row("name", "s1", "size", 3));
    assertEquals(1, db.getSchema(schemaName).getTable("MyShape").retrieveRows().size());
    assertDoesNotThrow(() -> db.dropSchema(schemaName));
  }

  @Test
  void addingRefSchemaToLocallyInheritingTableIsRejected() {
    String schemaName = TestInherits.class.getSimpleName() + "_addrefschema";
    String otherSchemaName = TestInherits.class.getSimpleName() + "_addrefschema_other";
    db.dropSchemaIfExists(schemaName);
    db.dropCreateSchema(otherSchemaName).create(table("Shape", column("name").setPkey()));
    Schema schema = db.createSchema(schemaName);
    schema.create(table("Shape", column("name").setPkey()));
    schema.create(table("MyShape", column("size").setType(INT)).setInheritName("Shape"));

    SchemaMetadata reimport = new SchemaMetadata(schemaName);
    reimport.create(table("Shape", column("name").setPkey()));
    reimport.create(
        table("MyShape", column("size").setType(INT))
            .setInheritName("Shape")
            .setImportSchema(otherSchemaName));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> schema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot change refSchema of table '"
                    + schemaName
                    + ".MyShape': inheritance cannot be changed after the table is created."),
        exception.getMessage());

    TableMetadata after = db.getSchema(schemaName).getMetadata().getTableMetadata("MyShape");
    assertNull(after.getImportSchema());
    assertEquals("Shape", after.getInheritName());
    db.getSchema(schemaName).getTable("MyShape").insert(row("name", "s1", "size", 3));
    assertEquals(1, db.getSchema(schemaName).getTable("MyShape").retrieveRows().size());
    assertDoesNotThrow(() -> db.dropSchema(schemaName));
  }

  @Test
  void removingTableExtendsFromExistingTableIsRejected() {
    String schemaName = TestInherits.class.getSimpleName() + "_removeextends";
    Schema schema = db.dropCreateSchema(schemaName);
    schema.create(table("Shape", column("name").setPkey()));
    schema.create(table("MyShape", column("size").setType(INT)).setInheritName("Shape"));

    SchemaMetadata reimport = new SchemaMetadata(schemaName);
    reimport.create(table("Shape", column("name").setPkey()));
    reimport.create(table("MyShape", column("size").setType(INT)));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> schema.migrate(reimport));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Cannot remove tableExtends of table '"
                    + schemaName
                    + ".MyShape': inheritance cannot be changed after the table is created."),
        exception.getMessage());

    TableMetadata after = db.getSchema(schemaName).getMetadata().getTableMetadata("MyShape");
    assertEquals("Shape", after.getInheritName());
    db.getSchema(schemaName).getTable("MyShape").insert(row("name", "s1", "size", 3));
    assertEquals(1, db.getSchema(schemaName).getTable("MyShape").retrieveRows().size());
    assertDoesNotThrow(() -> db.dropSchema(schemaName));
  }
}
