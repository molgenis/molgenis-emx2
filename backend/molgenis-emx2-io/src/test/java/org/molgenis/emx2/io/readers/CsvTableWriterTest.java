package org.molgenis.emx2.io.readers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class CsvTableWriterTest {

  @Test
  void shouldRemovePrefixingUnderscores() throws IOException {
    StringWriter stringWriter = new StringWriter();
    CsvTableWriter.write(
        List.of(Row.row("_subject_", "foo", "___multiple", "bar")),
        List.of("_subject_", "___multiple"),
        stringWriter,
        ',');
    assertEquals(
        """
        _subject_,___multiple
        foo,bar
        """,
        stringWriter.toString());
  }

  @Test
  void shouldWriteHeaderOnlyWhenNoRows() throws IOException {
    StringWriter stringWriter = new StringWriter();
    CsvTableWriter.write(List.of(), List.of("col1", "col2"), stringWriter, ',');
    assertEquals(
        """
        col1,col2
        """,
        stringWriter.toString());
  }

  @Test
  void shouldWriteMultipleColumnsInOrder() throws IOException {
    StringWriter stringWriter = new StringWriter();
    CsvTableWriter.write(
        List.of(Row.row("col1", "a", "col2", "b")), List.of("col1", "col2"), stringWriter, ',');
    assertEquals(
        """
        col1,col2
        a,b
        """,
        stringWriter.toString());
  }

  @Test
  void shouldWriteMultipleRows() throws IOException {
    StringWriter stringWriter = new StringWriter();
    CsvTableWriter.write(
        List.of(Row.row("col1", "a"), Row.row("col1", "b")), List.of("col1"), stringWriter, ',');
    assertEquals(
        """
        col1
        a
        b
        """,
        stringWriter.toString());
  }

  @Test
  void shouldUseProvidedSeparator() throws IOException {
    StringWriter stringWriter = new StringWriter();
    CsvTableWriter.write(
        List.of(Row.row("col1", "a", "col2", "b")), List.of("col1", "col2"), stringWriter, ';');
    assertEquals(
        """
        col1;col2
        a;b
        """,
        stringWriter.toString());
  }

  @Test
  void shouldQuoteValuesContainingSeparator() throws IOException {
    StringWriter stringWriter = new StringWriter();
    CsvTableWriter.write(List.of(Row.row("col1", "a,b")), List.of("col1"), stringWriter, ',');
    assertEquals(
        """
        col1
        "a,b"
        """,
        stringWriter.toString());
  }

  @Test
  void shouldWriteEmptyStringForNullValue() throws IOException {
    StringWriter stringWriter = new StringWriter();
    Row row = Row.row("col1", null, "col2", "2");
    CsvTableWriter.write(List.of(row), List.of("col1", "col2"), stringWriter, ',');
    assertEquals(
        """
        col1,col2
        ,2
        """,
        stringWriter.toString());
  }
}
