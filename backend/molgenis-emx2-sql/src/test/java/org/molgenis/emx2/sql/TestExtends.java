package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.MolgenisException;

import java.time.LocalDate;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.LIKE;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

public class TestExtends {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testExtends() {

    Schema s = db.createSchema("TestExtends");

    Table person = s.create(table("Person"));

    // test if fails if no primary key
    try {
      s.create(table("Employee").setInherit(person.getName()));
      fail("Should fail because does not have pkey");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    try {
      s.create(table("Employee").setInherit("fake table"));
      fail("Should fail");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // set pkey and a property
    person.getMetadata().add(column("fullName")).pkey("fullName");
    person.getMetadata().add(column("birthDate").type(DATE).nullable(true));

    // create first extended table
    Table employee =
        s.create(table("Employee").setInherit(person.getName()).add(column("salary").type(INT)));

    Table manager =
        s.create(
            table("Manager")
                .setInherit("Employee")
                .add(column("directs").type(REF_ARRAY).refTable("Employee").nullable(true)));

    Table ceo = s.create(table("CEO").setInherit("Manager"));

    // try to add column that already exists in parent
    try {
      employee.getMetadata().add(column("birthDate").type(DATE));
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to extend twice
    try {
      manager.getMetadata().setInherit("Student");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to change primary key
    try {
      manager.getMetadata().pkey("salary");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }
    // create another extended table
    s.create(table("Student").setInherit(person.getName()).add(column("averageGrade").type(INT)));

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
    assertEquals(3, personTable.getRows().size());
    assertEquals(1, studentTable.getRows().size());
    assertEquals(2, employeeTable.getRows().size());
    assertEquals(1, ceoTable.getRows().size());

    // retrieve
    assertEquals(
        (Integer) 1000000,
        employeeTable
            .query()
            .select("salary")
            .filter("fullName", EQUALS, "Dagobert Duck")
            .getRows()
            .get(0)
            .getInteger("salary"));

    // TODO test RLS

    // test search
    assertEquals(1, personTable.search("Dagobert").getRows().size());
    assertEquals(1, employeeTable.search("Dagobert").getRows().size());

    // update
    managerRow.setDate("birthDate", LocalDate.of(1900, 12, 01));
    ceoTable.update(managerRow);
    assertEquals(LocalDate.of(1900, 12, 01), ceoTable.getRows().get(0).getDate("birthDate"));

    // test graph query
    // simple
    String result = ceoTable.select(s("data", s("fullName"), s("salary"))).retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Dagobert"));
    // nested relation
    result =
        ceoTable
            .select(s("data", s("fullName"), s("salary"), s("directs", s("fullName"))))
            .retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));
    // filtering (erroroneous)
    result =
        ceoTable
            .select(s("data", s("fullName"), s("salary"), s("directs", s("fullName"))))
            .filter(f("directs", f("fullName", LIKE, "Pietje")))
            .retrieveJSON();
    System.out.println(result);
    assertFalse(result.contains("Katrien"));
    // filtering (correct)
    result =
        ceoTable
            .select(s("data", s("fullName"), s("salary"), s("directs", s("fullName"))))
            .filter(f("directs", f("fullName", LIKE, "Katrien")))
            .retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));

    // delete
    ceoTable.delete(managerRow);
    assertEquals(2, personTable.getRows().size());
    assertEquals(1, studentTable.getRows().size());
    assertEquals(1, employeeTable.getRows().size());
    assertEquals(0, ceoTable.getRows().size());
  }
}
