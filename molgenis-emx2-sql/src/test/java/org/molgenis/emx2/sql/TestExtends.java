package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.MolgenisException;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Operator.EQUALS;

public class TestExtends {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = DatabaseFactory.getTestDatabase();
  }

  @Test
  public void testExtends() {

    Schema s = db.createSchema("TestExtends");

    Table person = s.createTableIfNotExists("Person");

    // test if fails if no foreign key
    try {
      s.createTableIfNotExists("Employee").getMetadata().setInherit(person.getName());
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    try {
      s.createTableIfNotExists("Employee").getMetadata().setInherit("fake table");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // set pkey and a property
    person.getMetadata().addColumn("fullName", STRING).primaryKey();
    person.getMetadata().addColumn("birthDate", DATE).setNullable(true);

    // create first extended table
    TableMetadata employee =
        s.createTableIfNotExists("Employee").getMetadata().setInherit(person.getName());
    employee.addColumn("salary", INT);

    TableMetadata manager =
        s.createTableIfNotExists("Manager").getMetadata().setInherit(employee.getTableName());
    manager.addRefArray("directs", employee.getTableName()).setNullable(true);

    // try to add column that already exists in parent
    try {
      employee.addColumn("birthDate", DATE);
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to extend twice
    try {
      manager.setInherit("Student");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to change primary key
    try {
      manager.setPrimaryKey("salary");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }
    // create another extended table
    TableMetadata student =
        s.createTableIfNotExists("Student").getMetadata().setInherit(person.getName());
    student.addColumn("averageGrade", INT);

    // test insert, retrieve
    Table studentTable = s.getTable("Student");
    studentTable.insert(new Row().setString("fullName", "Donald Duck").setInt("averageGrade", 10));

    Table employeeTable = s.getTable("Employee");
    employeeTable.insert(
        new Row()
            .setString("fullName", "Katrien Duck")
            .setInt("salary", 100)
            .setDate("birthDate", LocalDate.of(2000, 12, 01)));

    Table managerTable = s.getTable("Manager");
    Row managerRow =
        new Row()
            .setString("fullName", "Dagobert Duck")
            .setInt("salary", 1000000)
            .setDate("birthDate", LocalDate.of(2000, 12, 01))
            .setStringArray("directs", "Katrien Duck");
    managerTable.insert(managerRow);

    Table personTable = s.getTable("Person");
    assertEquals(3, personTable.retrieve().size());
    assertEquals(1, studentTable.retrieve().size());
    assertEquals(2, employeeTable.retrieve().size());
    assertEquals(1, managerTable.retrieve().size());

    // retrieve
    assertEquals(
        (Integer) 1000000,
        employeeTable
            .query()
            .select("salary")
            .where("fullName", EQUALS, "Dagobert Duck")
            .retrieve()
            .get(0)
            .getInteger("salary"));

    // TODO test RLS

    // test search
    assertEquals(1, personTable.search("Dagobert").retrieve().size());
    assertEquals(1, employeeTable.search("Dagobert").retrieve().size());

    // update
    managerRow.setDate("birthDate", LocalDate.of(1900, 12, 01));
    managerTable.update(managerRow);
    assertEquals(LocalDate.of(1900, 12, 01), managerTable.retrieve().get(0).getDate("birthDate"));

    // delete
    managerTable.delete(managerRow);
    assertEquals(2, personTable.retrieve().size());
    assertEquals(1, studentTable.retrieve().size());
    assertEquals(1, employeeTable.retrieve().size());
    assertEquals(0, managerTable.retrieve().size());
  }
}
