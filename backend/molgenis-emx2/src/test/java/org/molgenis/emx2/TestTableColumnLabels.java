package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestTableColumnLabels {
  @Test
  void testTableLabel() {
    TableMetadata tm = new TableMetadata("Foo");
    assertEquals(tm.getLabel(), "Foo");
    tm.setLabel("Bar", "en");
    assertEquals(tm.getLabel(), "Bar");
    tm.setLabel("", "en");
    assertEquals(tm.getLabel(), "Foo");
  }

  @Test
  void testTableColumn() {
    Column cm = new Column("Foo");
    assertEquals(cm.getLabel(), "Foo");
    cm.setLabel("Bar", "en");
    assertEquals(cm.getLabel(), "Bar");
    cm.setLabel("", "en");
    assertEquals(cm.getLabel(), "Foo");
  }
}
