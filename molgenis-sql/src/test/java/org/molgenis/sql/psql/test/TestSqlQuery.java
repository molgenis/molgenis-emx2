package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.QueryBean;
import org.molgenis.beans.RowBean;

import java.sql.SQLException;
import java.util.List;

import static org.molgenis.Column.Type.STRING;

public class TestSqlQuery {

  public static Database db = null;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = SqlTestHelper.getEmptyDatabase();

    // create a table to test with
    String PERSON = "Person";
    Table person = db.getSchema().createTable(PERSON);
    person.addColumn("First Name", STRING);
    person.addRef("Father", person).setNullable(true);
    person.addColumn("Last Name", STRING);
    person.addUnique("First Name", "Last Name");

    Row father = new RowBean().setString("First Name", "Donald").setString("Last Name", "Duck");
    Row child =
        new RowBean()
            .setString("First Name", "Kwik")
            .setString("Last Name", "Duck")
            .setRef("Father", father);

    db.insert("Person", father);
    db.insert("Person", child);
  }

  @Test
  public void test1() throws MolgenisException {
    Query q = db.query("Person");
    q.select("First Name")
        .select("Last Name")
        .expand("Father")
        .include("First Name")
        .include("Last Name");
    q.where("Last Name").eq("Duck").and("Father", "Last Name").eq("Duck");

    List<Row> rows = q.retrieve();
    for (Row r : rows) {
      System.out.println(r);
    }
  }
}
