package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServeWebFileTest {

  @Test
  public void testServe_FileExists() {
    Context ctx = mock(Context.class);
    String path = "/test.txt";

    ServeWebFile.Serve(ctx, path);

    verify(ctx).contentType("text/plain");
    verify(ctx).result("test".getBytes());
  }

  @Test
  public void testServe_FileNotFound() {
    Context ctx = mock(Context.class);
    String path = "/non-existent-file.css";
    when(ctx.status(404)).thenReturn(ctx);

    ServeWebFile.Serve(ctx, path);

    verify(ctx).status(404);
    verify(ctx).result("File not found: " + ctx.path());
  }

  @Test
  public void testServeOnlyContext() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.pathParam("path...")).thenReturn("/my-app/index.html");

    String original = System.getProperty("user.dir");
    Path userDir = Paths.get(original, "..", "..").normalize();
    System.setProperty("user.dir", userDir.toString());

    try {
      ServeWebFile.Serve(ctx);

    } finally {
      System.setProperty("user.dir", original); // restore afterwards
    }
    /* Grab what was outputted */
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(ctx).result(captor.capture());

    byte[] bytes = captor.getValue();
    String htmlOutput = new String(bytes, StandardCharsets.UTF_8);
    System.out.println(htmlOutput); // You can inspect or assert against this

    assertTrue(htmlOutput.contains("<h1>Hello from custom app!</h1>"));
  }

  @Test
  public void testServeOnlyContext_AutoServeIndexHtml() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.pathParam("path...")).thenReturn("/my-app");

    String original = System.getProperty("user.dir");
    Path userDir = Paths.get(original, "..", "..").normalize();
    System.setProperty("user.dir", userDir.toString());

    try {
      ServeWebFile.Serve(ctx);

    } finally {
      System.setProperty("user.dir", original); // restore afterwards
    }
    /* Grab what was outputted */
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(ctx).result(captor.capture());

    byte[] bytes = captor.getValue();
    String htmlOutput = new String(bytes, StandardCharsets.UTF_8);
    System.out.println(htmlOutput); // You can inspect or assert against this

    assertTrue(htmlOutput.contains("<h1>Hello from custom app!</h1>"));
  }

  @Test
  public void testServeOnlyContext_FileNotFound() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.pathParam("path...")).thenReturn("/non-existent");

    ServeWebFile.Serve(ctx);

    verify(ctx).status(404);
    verify(ctx).result("File not found: " + ctx.path());
  }
}
