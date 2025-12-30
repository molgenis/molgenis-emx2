package org.molgenis.emx2.web;

import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;

class StaticFileMapperTest {

  @Test
  public void testAddFileToContext_FileExists_WithMimeType() {
    Context ctx = mock(Context.class);
    String path = "/index.html";
    String mimeType = "text/html";

    StaticFileMapper.addFileToContext(ctx, path, mimeType);

    verify(ctx).contentType(mimeType);
    verify(ctx).result("<html></html>".getBytes());
  }

  @Test
  public void testAddFileToContext_FileExists_WithoutMimeType() {
    Context ctx = mock(Context.class);
    String path = "/test.txt";

    StaticFileMapper.addFileToContext(ctx, path, null);

    verify(ctx).contentType("text/plain");
    verify(ctx).result("test".getBytes());
  }

  @Test
  public void testAddFileToContext_FileNotFound() {
    Context ctx = mock(Context.class);
    String path = "/non-existent-file.css";
    when(ctx.status(404)).thenReturn(ctx);

    StaticFileMapper.addFileToContext(ctx, path, "text/plain");

    verify(ctx).status(404);
    verify(ctx).result("File not found: " + ctx.path());
  }
}
