package org.molgenis.emx2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.Test;

public class TestColumnOrderSubclass {

  @Test
  public void testColumnOrderSubclass() {

    SchemaMetadata s = new SchemaMetadata();

    // new method to add columns to subclass only while putting it in order of grand scheme of
    // things, while keeping old method working (where subclass columns are added at the end)
    s.create(
        table(
            "Person",
            column("id").setPkey(),
            column("name"),
            // column("dutchName").setTableName("DutchPerson"),
            column("dob").setType(ColumnType.DATE)),
        table("DutchPerson")
            .setInherit("Person")
            .setDescription("Description")
            .add(column("geboorteplaats")));

    // expect two tables, Person and DutchPerson
    assertTrue(s.getTableNames().contains("DutchPerson"));

    // expect DutchPerson to extend Person
    assertEquals("Person", s.getTableMetadata("DutchPerson").getInherit());

    // expect DutchPerson to have description
    // todo
    TableMetadata p = s.getTableMetadata("Person");

    // when asking columns of Person only, expect id, name, dob
    assertEquals(3, p.getColumnNames().size());
    assertEquals(0, p.getColumnNames().indexOf("id"));
    assertEquals(1, p.getColumnNames().indexOf("name"));
    assertEquals(2, p.getColumnNames().indexOf("dob"));
    p.getColumns();

    // when asking all columns of DutchPerson, incl subclass,
    // expect id(index=0), name, dutchName(index=2), dob, geboorteplaats(index=4)
    TableMetadata dp = s.getTableMetadata("DutchPerson");
    assertEquals(5, dp.getColumnNames().size());
    assertEquals(0, dp.getColumnNames().indexOf("id"));
    assertEquals(2, dp.getColumnNames().indexOf("dutchName"));
    assertEquals(4, dp.getColumnNames().indexOf("geboorteplaats"));

    // when asking private columns of DutchPerson expect id, dutchName, geboorteplaats
    assertEquals(3, dp.getLocalColumns().size());
    assertEquals("id", dp.getLocalColumns().get(0).getName());
    assertEquals("dutchName", dp.getLocalColumns().get(1).getName());
    assertEquals("geboorteplaats", dp.getLocalColumns().get(2).getName());

    // when asking except the primary key that we actually inherited
    assertEquals(2, dp.getNonInheritedColumns().size());
    assertEquals("dutchName", dp.getNonInheritedColumns().get(0).getName());
    assertEquals("geboorteplaats", dp.getNonInheritedColumns().get(1).getName());

    // id actually from person, but is mirrored locally so should be DutchPerson
    assertEquals("DutchPerson", dp.getLocalColumns().get(0).getTableName());
  }
}
