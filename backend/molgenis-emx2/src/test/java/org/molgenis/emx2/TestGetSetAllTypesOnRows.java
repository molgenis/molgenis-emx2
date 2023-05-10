package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TestGetSetAllTypesOnRows {

  @Test
  public void test1() {
    List<ColumnType> columnTypes =
        Arrays.asList(STRING, INT, DECIMAL, BOOL, UUID, TEXT, DATE, DATETIME);

    SchemaMetadata m = new SchemaMetadata("test1");
    addContents(m, columnTypes);

    SchemaMetadata m2 = new SchemaMetadata("test1");
    addContents(m2, columnTypes);

    // System.out.println("No diff: " + m.diff(m2));

    assertTrue(m.getTableNames().contains("TypeTest"));
    assertEquals(1, m.getTableNames().size());

    // System.out.println("model print: " + m.print());
    TableMetadata t = m.getTableMetadata("TypeTest");
    assertEquals("TypeTest", t.getTableName());
    assertEquals(3 * columnTypes.size(), t.getColumns().size());
    assertEquals(BOOL, t.getColumn("testBOOL").getColumnType());

    // System.out.println("table print " + t.toString() + "\n: " + t.print());

    m2.create(table("OtherTable"));
    // System.out.println("Now we expect diff: " + m.diff(m2));

    m.drop("TypeTest");

    TableMetadata test = m.getTableMetadata("TypeTest");
    assertEquals(0, m.getTableNames().size());
  }

  @Test
  public void rowCanSetGetSimpleTypes() {
    Row r = new Row();

    // int
    r.setString("test", "1");
    assertEquals(1, (int) r.getInteger("test"));
    assertNull(r.getInteger("testnull"));
    try {
      r.setString("test", "a");
      assertEquals(1, (int) r.getInteger("test"));
      fail("shouldn't be able to get 'a' to int ");
    } catch (Exception e) {
    }

    // decimal
    r.setString("test", "1.0");
    assertEquals(1.0, (double) r.getDecimal("test"));
    assertNull(r.getDecimal("testnull"));
    try {
      r.setString("test", "a");
      r.getDecimal("test");
      fail("shouldn't be able to get 'a' to decimal ");
    } catch (Exception e) {
    }

    // bool
    r.setBool("test", true);
    assertTrue(r.getBoolean("test"));

    r.setBool("testnull", null);
    assertNull(r.getBoolean("testnull"));

    r.setString("test", "true");
    assertTrue(r.getBoolean("test"));

    try {
      r.setString("test", "a");
      r.getBoolean("test");
      fail("shouldn't be able to get 'a' to boolean ");
    } catch (Exception e) {
    }

    // uuid
    java.util.UUID uuid = java.util.UUID.randomUUID();
    r.setString("test", uuid.toString());
    assertEquals(uuid, r.getUuid("test"));
    assertNull(r.getUuid("testnull"));
    try {
      r.setString("test", "a");
      r.getUuid("test");
      fail("shouldn't be able to get 'a' to uuid ");
    } catch (Exception e) {
    }

    // date
    String dateString = "2012-10-03";
    r.setString("test", dateString);
    LocalDate date = LocalDate.parse(dateString);
    assertEquals(date, r.getDate("test"));
    assertNull(r.getDate("testnull"));
    try {
      r.setString("test", "a");
      r.getDate("test");
      fail("shouldn't be able to get 'a' to date ");
    } catch (Exception e) {
    }

    // datetime
    String dateTimeString = "2012-10-03T18:00";
    r.setString("test", dateTimeString);
    LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);
    assertEquals(dateTime, r.getDateTime("test"));
    assertNull(r.getDate("testnull"));
    try {
      r.setString("test", "a");
      r.getDateTime("test");
      fail("shouldn't be able to get 'a' to datetime ");
    } catch (Exception e) {
    }

    // string if not a string
    r.setInt("test", 1);
    assertEquals("1", r.getString("test"));
    assertNull(r.getString("testnull"));
  }

  @Test
  public void rowCanGetSetArrayTypeColumns() {
    Row r = new Row();

    // cast UUID[1] from some Object
    r.setString("test", "cfb11a12-dad6-4b98-a48b-9a32f60a742f");
    assertEquals(
        new java.util.UUID[] {java.util.UUID.fromString("cfb11a12-dad6-4b98-a48b-9a32f60a742f")}
            [0].toString(),
        r.getUuidArray("test")[0].toString());

    // cast int from some object
    r.setString("test", "blaat");
    try {
      assertEquals(new Integer[] {9}, r.getIntegerArray("test"));
      fail("cannot convert, should fail");
    } catch (Exception e) {
    }

    r.set("test", new Boolean[] {true, false});
    assertArrayEquals(new Boolean[] {true, false}, r.getBooleanArray("test"));

    r.set("test", new String[] {"true", "false"});
    assertArrayEquals(new Boolean[] {true, false}, r.getBooleanArray("test"));

    r.set("test", "true");
    assertArrayEquals(new Boolean[] {true}, r.getBooleanArray("test"));

    r.setString("test", "9.3");
    assertArrayEquals(new Double[] {9.3}, r.getDecimalArray("test"));

    OffsetDateTime odt = OffsetDateTime.of(2018, 12, 12, 12, 12, 12, 12, ZoneOffset.UTC);
    r.set("test", odt);
    assertArrayEquals(new LocalDateTime[] {odt.toLocalDateTime()}, r.getDateTimeArray("test"));
  }

  private void addContents(SchemaMetadata m, List<ColumnType> columnTypes) {
    TableMetadata t = m.create(table("TypeTest"));
    for (ColumnType columnType : columnTypes) {
      t.add(column("test" + columnType).setType(columnType).setRequired(true));
      t.add(column("test" + columnType + "_nullable").setType(columnType));
      t.add(column("test" + columnType + "_readonly").setType(columnType).setReadonly(true));
    }
  }
}
