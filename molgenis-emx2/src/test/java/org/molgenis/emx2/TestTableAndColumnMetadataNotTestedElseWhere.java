package org.molgenis.emx2;

import org.junit.Test;
import org.molgenis.emx2.utils.MolgenisException;

import static junit.framework.TestCase.*;
import static org.molgenis.emx2.ColumnType.STRING;

public class TestTableAndColumnMetadataNotTestedElseWhere {
  @Test
  public void testDuplicateTableError() {
    try {
      SchemaMetadata s = new SchemaMetadata();
      s.createTable(new TableMetadata(s, "test"));
      s.createTable(new TableMetadata(s, "test"));
      fail("should not be able to add same table twice");
    } catch (MolgenisException me) {
      System.out.println("Error correctly:\n" + me);
    }
  }

  @Test
  public void testDuplicateColumnError() {
    try {
      SchemaMetadata s = new SchemaMetadata();
      TableMetadata t = new TableMetadata(s, "test");
      t.addColumn(new Column(t, "test", STRING));
      System.out.println(t);

      t.addColumn(new Column(t, "test", STRING));
      fail("should not be able to add same column twice");
    } catch (MolgenisException me) {
      System.out.println("Error correctly:\n" + me);
    }
  }

  @Test
  public void testRemoveUnknownColumnError() {
    try {
      SchemaMetadata s = new SchemaMetadata();
      TableMetadata t = new TableMetadata(s, "test");
      t.removeColumn("test");
      fail("should not be able to silently delete column that doesn't exists");
    } catch (MolgenisException me) {
      System.out.println("Error correctly:\n" + me);
    }
  }

  @Test
  public void testRemoveUnknownUniqueError() {
    try {
      SchemaMetadata s = new SchemaMetadata();
      TableMetadata t = new TableMetadata(s, "test");
      t.addUnique("test");
      fail("should not be able to set unique on not existing column");
    } catch (MolgenisException me) {
      System.out.println("Error correctly:\n" + me);
    }
  }

  @Test
  public void testUniques() {

    SchemaMetadata s = new SchemaMetadata();
    TableMetadata t = new TableMetadata(s, "test");

    t.addColumn(new Column(t, "a", STRING));
    t.addColumn(new Column(t, "b", STRING));

    t.addUnique("a", "b");
    assertTrue(t.isUnique("b", "a")); // order doesn't matter
    t.removeUnique("b", "a");
    assertEquals(0, t.getUniques().size());
  }

  @Test
  public void testPrimaryKey() {

    SchemaMetadata s = new SchemaMetadata();
    TableMetadata t = new TableMetadata(s, "test");

    t.addColumn(new Column(t, "a", STRING));
    t.addColumn(new Column(t, "b", STRING));

    t.setPrimaryKey("a", "b");
    assertTrue(t.isPrimaryKey("b", "a")); // order doesn't matter
  }
}
