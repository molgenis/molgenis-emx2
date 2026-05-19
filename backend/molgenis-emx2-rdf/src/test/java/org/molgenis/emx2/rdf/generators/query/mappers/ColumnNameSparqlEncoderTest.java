package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.generators.query.ColumnNameSparqlEncoder;

class ColumnNameSparqlEncoderTest {

  @Test
  void shouldDecodeSparqlVariable() {
    Column column = Column.column("order_id");
    assertEquals("order_id", ColumnNameSparqlEncoder.encodeSparqlVariable(column));
  }

  @Test
  void shouldReplaceSpace() {
    String actual = ColumnNameSparqlEncoder.encodeSparqlVariable("delivery method");
    assertEquals("delivery___method", actual);
  }

  @Test
  void shouldReplaceDot() {
    String actual = ColumnNameSparqlEncoder.encodeSparqlVariable("delivery.method");
    assertEquals("delivery__method", actual);
  }

  @Test
  void shouldRevertChanges() {
    String actual = ColumnNameSparqlEncoder.decodeSparqlVariable("a___b__c_d");
    assertEquals("a b.c_d", actual);
  }
}
