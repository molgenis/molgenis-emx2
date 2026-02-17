package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServeStaticFileTest {

  @Test
  public void testServe_FileExists() {
    Context ctx = mock(Context.class);
    String path = "/test.txt";

    ServeStaticFile.serve(ctx, path);

    verify(ctx).contentType("text/plain");
    verify(ctx).result("test".getBytes());
  }

  @Test
  public void testServe_FileNotFound() {
    Context ctx = mock(Context.class);
    String path = "/non-existent-file.css";
    when(ctx.status(404)).thenReturn(ctx);

    ServeStaticFile.serve(ctx, path);

    verify(ctx).status(404);
    verify(ctx).result("File not found: " + ctx.path());
  }

  @Test
  public void testServeOnlyContext() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("my-app/index.html");

    ServeStaticFile.serve(ctx);

    /* Grab what was outputted */
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(ctx).result(captor.capture());

    byte[] bytes = captor.getValue();
    String htmlOutput = new String(bytes, StandardCharsets.UTF_8);
    System.out.println(htmlOutput);

    assertTrue(htmlOutput.contains("<h1>Hello from custom app!</h1>"));
  }

  @Test
  public void testServeOnlyContext_AutoServeIndexHtml() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("/my-app");

    ServeStaticFile.serve(ctx);

    /* Grab what was outputted */
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(ctx).result(captor.capture());

    byte[] bytes = captor.getValue();
    String htmlOutput = new String(bytes, StandardCharsets.UTF_8);
    System.out.println(htmlOutput);

    assertTrue(htmlOutput.contains("<h1>Hello from custom app!</h1>"));
  }

  @Test
  public void testServeOnlyContext_FileNotFound() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("/non-existent");

    ServeStaticFile.serve(ctx);

    verify(ctx).status(404);
    verify(ctx).result("File not found: /non-existent");
  }
}
