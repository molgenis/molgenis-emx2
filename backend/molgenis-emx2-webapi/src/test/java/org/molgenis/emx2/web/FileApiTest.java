package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

class FileApiTest {

  @Test
  void serveFileReturnsRequestedFile() {
    Context ctx = mock(Context.class);
    Schema schema = mock(Schema.class);
    Table table = mock(Table.class);
    TableMetadata metadata = mock(TableMetadata.class);
    Column column = mock(Column.class);
    Query query = mock(Query.class, Answers.RETURNS_SELF);
    byte[] contents = "hello world".getBytes(StandardCharsets.UTF_8);
    Row row =
        Row.row(
            "attachment_filename", "report.pdf",
            "attachment_extension", "pdf",
            "attachment_mimetype", "application/pdf",
            "attachment_contents", contents);
    Field<?> primaryKey = DSL.field("id");

    when(ctx.pathParam("table")).thenReturn("documents");
    when(ctx.pathParam("column")).thenReturn("attachment");
    when(ctx.pathParam("id")).thenReturn("file-123");
    when(schema.getTable("documents")).thenReturn(table);
    when(schema.getName()).thenReturn("test");
    when(table.getMetadata()).thenReturn(metadata);
    when(metadata.getColumnByNameIncludingSubclasses("attachment")).thenReturn(column);
    when(metadata.getPrimaryKeyFields()).thenReturn(List.of(primaryKey));
    when(table.query()).thenReturn(query);
    when(query.retrieveRows()).thenReturn(List.of(row));

    try (MockedStatic<MolgenisWebservice> webservice = mockStatic(MolgenisWebservice.class)) {
      webservice.when(() -> MolgenisWebservice.getSchema(ctx)).thenReturn(schema);

      FileApi.serveFile(ctx);
    }

    verify(ctx).header("Content-Disposition", "inline; filename=report.pdf");
    verify(ctx).contentType("application/pdf");
    verify(ctx).result(contents);
  }

  @Test
  void serveFileThrowsWhenFileIdIsUnknown() {
    Context ctx = mock(Context.class);
    Schema schema = mock(Schema.class);
    Table table = mock(Table.class);
    TableMetadata metadata = mock(TableMetadata.class);
    Column column = mock(Column.class);
    Query query = mock(Query.class, Answers.RETURNS_SELF);
    Field<?> primaryKey = DSL.field("id");

    when(ctx.pathParam("table")).thenReturn("documents");
    when(ctx.pathParam("column")).thenReturn("attachment");
    when(ctx.pathParam("id")).thenReturn("missing-file");
    when(schema.getTable("documents")).thenReturn(table);
    when(schema.getName()).thenReturn("test");
    when(table.getMetadata()).thenReturn(metadata);
    when(metadata.getColumnByNameIncludingSubclasses("attachment")).thenReturn(column);
    when(metadata.getPrimaryKeyFields()).thenReturn(List.of(primaryKey));
    when(table.query()).thenReturn(query);
    when(query.retrieveRows()).thenReturn(List.of());

    try (MockedStatic<MolgenisWebservice> webservice = mockStatic(MolgenisWebservice.class)) {
      webservice.when(() -> MolgenisWebservice.getSchema(ctx)).thenReturn(schema);

      MolgenisException exception =
          assertThrows(MolgenisException.class, () -> FileApi.serveFile(ctx));
      assertEquals(
          "Download failed: file id 'missing-file' not found in table documents",
          exception.getMessage());
    }
  }
}
