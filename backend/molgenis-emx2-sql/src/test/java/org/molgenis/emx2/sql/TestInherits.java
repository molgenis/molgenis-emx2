package org.molgenis.emx2.sql;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

import java.time.LocalDate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestInherits {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testExtends() {

    Schema s = db.dropCreateSchema("TestExtends");

    Table person = s.create(TableMetadata.table("Person"));

    // test if fails if no primary key
    try {
      s.create(TableMetadata.table("Employee").setInherit(person.getName()));
      fail("Should fail because does not have pkey");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    try {
      s.create(TableMetadata.table("Employee").setInherit("fake_table"));
      fail("Should fail");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // set pkey and a property
    person.getMetadata().add(Column.column("fullName").setPkey());
    person.getMetadata().add(Column.column("birthDate").setType(ColumnType.DATE));

    // create first extended table
    Table employee =
        s.create(
            TableMetadata.table("Employee")
                .setInherit(person.getName())
                .add(Column.column("salary").setType(ColumnType.INT)));

    Table manager =
        s.create(
            TableMetadata.table("Manager")
                .setInherit("Employee")
                .add(
                    Column.column("directs")
                        .setType(ColumnType.REF_ARRAY)
                        .setRefTable("Employee")));

    Table ceo = s.create(TableMetadata.table("CEO").setInherit("Manager"));

    // try to add column that already exists in parent
    try {
      employee.getMetadata().add(Column.column("birthDate").setType(ColumnType.DATE));
      fail("should fail: cannot add column to subclass that already exists in superclass");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to extend twice
    try {
      manager.getMetadata().setInherit("Student");
      fail("should fail: cannot extend another table");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // create another extended table
    s.create(
        TableMetadata.table("Student")
            .setInherit(person.getName())
            .add(Column.column("averageGrade").setType(ColumnType.INT)));

    // test insert, retrieve
    Table studentTable = s.getTable("Student");
    studentTable.insert(new Row().setString("fullName", "Donald Duck").setInt("averageGrade", 10));

    Table employeeTable = s.getTable("Employee");
    employeeTable.insert(
        new Row()
            .setString("fullName", "Katrien Duck")
            .setInt("salary", 100)
            .setDate("birthDate", LocalDate.of(2000, 12, 01)));

    Table ceoTable = s.getTable("CEO"); // we use CEO to make it more difficult
    Row managerRow =
        new Row()
            .setString("fullName", "Dagobert Duck")
            .setInt("salary", 1000000)
            .setDate("birthDate", LocalDate.of(2000, 12, 01))
            .setStringArray("directs", "Katrien Duck");
    ceoTable.insert(managerRow);

    Table personTable = s.getTable("Person");
    Assert.assertEquals(3, personTable.retrieveRows().size());
    Assert.assertEquals(1, studentTable.retrieveRows().size());
    Assert.assertEquals(2, employeeTable.retrieveRows().size());
    Assert.assertEquals(1, ceoTable.retrieveRows().size());

    // retrieve
    Assert.assertEquals(
        (Integer) 1000000,
        employeeTable
            .query()
            .select(SelectColumn.s("salary"))
            .where(FilterBean.f("fullName", Operator.EQUALS, "Dagobert Duck"))
            .retrieveRows()
            .get(0)
            .getInteger("salary"));

    // TODO test RLS

    // test search
    Assert.assertEquals(1, personTable.search("Dagobert").retrieveRows().size());
    Assert.assertEquals(1, employeeTable.search("Dagobert").retrieveRows().size());

    // update
    managerRow.setDate("birthDate", LocalDate.of(1900, 12, 01));
    ceoTable.update(managerRow);
    Assert.assertEquals(
        LocalDate.of(1900, 12, 01), ceoTable.retrieveRows().get(0).getDate("birthDate"));

    // test graph query
    // simple
    String result =
        ceoTable.select(SelectColumn.s("fullName"), SelectColumn.s("salary")).retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Dagobert"));
    // nested relation
    result =
        ceoTable
            .select(
                SelectColumn.s("fullName"),
                SelectColumn.s("salary"),
                SelectColumn.s("directs", SelectColumn.s("fullName")))
            .retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));
    // filtering (erroroneous)
    result =
        ceoTable
            .select(
                SelectColumn.s("fullName"),
                SelectColumn.s("salary"),
                SelectColumn.s("directs", SelectColumn.s("fullName")))
            .where(FilterBean.f("directs", FilterBean.f("fullName", Operator.LIKE, "Pietje")))
            .retrieveJSON();
    System.out.println(result);
    assertFalse(result.contains("Katrien"));
    // filtering (correct)
    result =
        ceoTable
            .select(
                SelectColumn.s("fullName"),
                SelectColumn.s("salary"),
                SelectColumn.s("directs", SelectColumn.s("fullName")))
            .where(FilterBean.f("directs", FilterBean.f("fullName", Operator.LIKE, "Katrien")))
            .retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));

    // filtering on mg_tableclass
    Assert.assertEquals(
        1,
        personTable
            .query()
            .where(
                FilterBean.f(Constants.MG_TABLECLASS, Operator.EQUALS, s.getName() + ".Employee"))
            .retrieveRows()
            .size());

    // delete
    ceoTable.delete(managerRow);
    Assert.assertEquals(2, personTable.retrieveRows().size());
    Assert.assertEquals(1, studentTable.retrieveRows().size());
    Assert.assertEquals(1, employeeTable.retrieveRows().size());
    Assert.assertEquals(0, ceoTable.retrieveRows().size());

    // test multi add
    personTable.insert(
        Row.row(Constants.MG_TABLECLASS, "Manager", "fullName", "popeye"),
        Row.row(Constants.MG_TABLECLASS, "Employee", "fullName", "goofy"));
    Assert.assertEquals(4, personTable.retrieveRows().size());
    Assert.assertEquals(1, studentTable.retrieveRows().size());
    Assert.assertEquals(3, employeeTable.retrieveRows().size());
    Assert.assertEquals(1, manager.retrieveRows().size());
    Assert.assertEquals(0, ceoTable.retrieveRows().size());

    try {
      personTable.insert(Row.row(Constants.MG_TABLECLASS, "Wrong", "fullName", "popeye"));
      fail("should error");
    } catch (Exception e) {
      System.out.println("Errored correctly: " + e.getMessage());
    }

    try {
      personTable.insert(Row.row(Constants.MG_TABLECLASS, "Blaat.Wrong", "fullName", "popeye"));
      fail("should error");
    } catch (Exception e) {
      System.out.println("Errored correctly: " + e.getMessage());
    }
  }
}
