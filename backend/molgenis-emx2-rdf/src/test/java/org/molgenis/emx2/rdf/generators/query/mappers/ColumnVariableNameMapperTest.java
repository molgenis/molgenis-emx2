package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;

class ColumnVariableNameMapperTest {

  @Test
  void shouldGetColumnName() {
    Column column = Column.column("order_id");
    assertEquals("order_id", ColumnVariableNameMapper.columnToSparql(column));
  }

  @Test
  void shouldReplaceSpace() {
    String actual = ColumnVariableNameMapper.columnNameToSparql("delivery method");
    assertEquals("delivery___method", actual);
  }

  @Test
  void shouldReplaceDot() {
    String actual = ColumnVariableNameMapper.columnNameToSparql("delivery.method");
    assertEquals("delivery__method", actual);
  }

  @Test
  void shouldRevertChanges() {
    String actual = ColumnVariableNameMapper.sparqlToColumnName("a___b__c_d");
    assertEquals("a b.c_d", actual);
  }
}
