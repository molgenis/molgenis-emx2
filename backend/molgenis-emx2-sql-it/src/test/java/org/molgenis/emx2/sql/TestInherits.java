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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestInherits {
  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testExtends() {

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
            .contains("REFERENCES Person"));

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

    // test that we can delete sub via super
    int count = personTable.retrieveRows().size();
    personTable.delete(row("fullName", "popeye")); // is a manager!!!
    assertEquals(count - 1, personTable.retrieveRows().size());

    // can also drop the table without errors when trigger is removed
    ceoTable.getMetadata().drop();
    manager.getMetadata().drop();
    employeeTable.getMetadata().drop();
    studentTable.getMetadata().drop();
    personTable.getMetadata().drop();
    // todo add test that trigger actually is deleted
  }
}
