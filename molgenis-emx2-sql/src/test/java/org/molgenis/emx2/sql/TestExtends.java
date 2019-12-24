// package org.molgenis.emx2.sql;
//
// import org.junit.BeforeClass;
// import org.junit.Test;
// import org.molgenis.emx2.*;
// import org.molgenis.emx2.MolgenisException;
//
// import java.time.LocalDate;
//
// import static junit.framework.TestCase.fail;
// import static org.junit.Assert.assertEquals;
// import static org.molgenis.emx2.Column.column;
// import static org.molgenis.emx2.ColumnType.*;
// import static org.molgenis.emx2.Operator.EQUALS;
// import static org.molgenis.emx2.TableMetadata.table;
//
// public class TestExtends {
//  private static Database db;
//
//  @BeforeClass
//  public static void setUp() {
//    db = TestDatabaseFactory.getTestDatabase();
//  }
//
//  @Test
//  public void testExtends() {
//
//    Schema s = db.createSchema("TestExtends");
//
//    Table person = s.create(table("Person"));
//
//    // test if fails if no foreign key
//    try {
//      s.create(table("Employee").setInherit(person.getName()));
//      fail("Should fail");
//    } catch (MolgenisException e) {
//      System.out.println("Errored correctly:\n" + e);
//    }
//
//    try {
//      s.create(table("Employee").setInherit("fake table"));
//      fail("Should fail");
//    } catch (MolgenisException e) {
//      System.out.println("Errored correctly:\n" + e);
//    }
//
//    // set pkey and a property
//    person.getMetadata().addColumn(column("fullName")).setPrimaryKey("fullName");
//    person.getMetadata().addColumn(column("birthDate").type(DATE).nullable(true));
//
//    // create first extended table
//    Table employee =
//        s.create(
//            table("Employee").setInherit(person.getName()).addColumn(column("salary").type(INT)));
//
//    Table manager =
//        s.create(
//            table("Manager")
//                .setInherit("Employee")
//
// .addColumn(column("directs").type(REF_ARRAY).refTable("Employee").nullable(true)));
//
//    // try to add column that already exists in parent
//    try {
//      employee.getMetadata().addColumn(column("birthDate").type(DATE));
//    } catch (MolgenisException e) {
//      System.out.println("Errored correctly:\n" + e);
//    }
//
//    // try to extend twice
//    try {
//      manager.getMetadata().setInherit("Student");
//    } catch (MolgenisException e) {
//      System.out.println("Errored correctly:\n" + e);
//    }
//
//    // try to change primary key
//    try {
//      manager.getMetadata().setPrimaryKey("salary");
//    } catch (MolgenisException e) {
//      System.out.println("Errored correctly:\n" + e);
//    }
//    // create another extended table
//    s.create(
//
// table("Student").setInherit(person.getName()).addColumn(column("averageGrade").type(INT)));
//
//    // test insert, retrieve
//    Table studentTable = s.getTable("Student");
//    studentTable.insert(new Row().setString("fullName", "Donald Duck").setInt("averageGrade",
// 10));
//
//    Table employeeTable = s.getTable("Employee");
//    employeeTable.insert(
//        new Row()
//            .setString("fullName", "Katrien Duck")
//            .setInt("salary", 100)
//            .setDate("birthDate", LocalDate.of(2000, 12, 01)));
//
//    Table managerTable = s.getTable("Manager");
//    Row managerRow =
//        new Row()
//            .setString("fullName", "Dagobert Duck")
//            .setInt("salary", 1000000)
//            .setDate("birthDate", LocalDate.of(2000, 12, 01))
//            .setStringArray("directs", "Katrien Duck");
//    managerTable.insert(managerRow);
//
//    Table personTable = s.getTable("Person");
//    assertEquals(3, personTable.retrieve().size());
//    assertEquals(1, studentTable.retrieve().size());
//    assertEquals(2, employeeTable.retrieve().size());
//    assertEquals(1, managerTable.retrieve().size());
//
//    // retrieve
//    assertEquals(
//        (Integer) 1000000,
//        employeeTable
//            .query()
//            .select("salary")
//            .where("fullName", EQUALS, "Dagobert Duck")
//            .retrieve()
//            .get(0)
//            .getInteger("salary"));
//
//    // TODO test RLS
//
//    // test search
//    assertEquals(1, personTable.search("Dagobert").retrieve().size());
//    assertEquals(1, employeeTable.search("Dagobert").retrieve().size());
//
//    // update
//    managerRow.setDate("birthDate", LocalDate.of(1900, 12, 01));
//    managerTable.update(managerRow);
//    assertEquals(LocalDate.of(1900, 12, 01), managerTable.retrieve().get(0).getDate("birthDate"));
//
//    // delete
//    managerTable.delete(managerRow);
//    assertEquals(2, personTable.retrieve().size());
//    assertEquals(1, studentTable.retrieve().size());
//    assertEquals(1, employeeTable.retrieve().size());
//    assertEquals(0, managerTable.retrieve().size());
//  }
// }
