package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.ColumnType.*;

public class TestExtends {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testExtends() {

    Schema s = db.createSchema("TestExtends");

    Table person = s.createTableIfNotExists("Person");

    // test if fails if no foreign key
    try {
      s.createTableIfNotExists("Employee").getMetadata().inherits(person.getName());
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    try {
      s.createTableIfNotExists("Employee").getMetadata().inherits("fake table");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // set pkey and a property
    person.getMetadata().addColumn("fullName", STRING).primaryKey();
    person.getMetadata().addColumn("birthDate", DATE).setNullable(true);

    // create first extended table
    TableMetadata employee =
        s.createTableIfNotExists("Employee").getMetadata().inherits(person.getName());
    employee.addColumn("salary", INT);

    // try to add column that already exists in parent
    try {
      employee.addColumn("birthDate", DATE);
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // create another extended table
    TableMetadata student =
        s.createTableIfNotExists("Student").getMetadata().inherits(person.getName());
    employee.addColumn("averageGrade", INT);

    // test insert, update, delete, retrieve

    // TODO test RLS
  }
}
