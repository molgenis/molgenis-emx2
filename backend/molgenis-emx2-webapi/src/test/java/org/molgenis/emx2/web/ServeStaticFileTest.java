package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServeStaticFileTest {
  static final String NOT_FOUND = "File not found: ";

  @Test
  void testServe_FileExists() {
    Context ctx = mock(Context.class);
    // serve(ctx, path) only serves resources under the internal apps root
    String path = "/public_html/apps/test-app/test-assets/styling.css";

    ServeStaticFile.serve(ctx, path);

    verify(ctx).header("Content-Type", "text/css");

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(ctx).result(captor.capture());
    String served = new String(captor.getValue(), StandardCharsets.UTF_8);
    assertTrue(served.contains("color:red"));
  }

  @Test
  void testServe_FileNotFound() {
    Context ctx = mock(Context.class);
    // in-contract path (under the internal root) but no such resource
    String path = "/public_html/apps/non-existent-file.css";
    when(ctx.status(404)).thenReturn(ctx);

    ServeStaticFile.serve(ctx, path);

    verify(ctx).status(404);
    verify(ctx).result(NOT_FOUND + ctx.path());
  }

  @Test
  void testServe_RejectsPathResolvingOutsideRoot() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    // normalizes to /test.txt, which resolves outside the internal apps root
    String path = "/public_html/apps/../../test.txt";

    ServeStaticFile.serve(ctx, path);

    verify(ctx).status(403);
    verify(ctx).result("Forbidden");
  }

  @Test
  void testServe_RejectsPathOutsideRoot() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    // this resource exists on the classpath, but is not under the internal root
    String path = "/test.txt";

    ServeStaticFile.serve(ctx, path);

    verify(ctx).status(403);
    verify(ctx).result("Forbidden");
  }

  @Test
  void testServeAppAsset_OnSchemaPath() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("/schema/test-app/test-assets/styling.css");

    ServeStaticFile.serve(ctx);

    verify(ctx).header("Content-Type", "text/css");
  }

  @Test
  void testServeUI_AutoServeIndexHtml() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("apps/ui/pet%20store/Order");

    ServeStaticFile.serve(ctx);

    // Grab what was outputted
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(ctx).result(captor.capture());

    byte[] bytes = captor.getValue();
    String htmlOutput = new String(bytes, StandardCharsets.UTF_8);
    System.out.println(htmlOutput);

    assertTrue(htmlOutput.contains("<p>UI file mapping works.</p>"));
  }

  @Test
  void testServeExternal_AutoServeIndexHtml() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("/schema/example-app");

    ServeStaticFile.serve(ctx);

    // Grab what was outputted
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(ctx).result(captor.capture());

    byte[] bytes = captor.getValue();
    String htmlOutput = new String(bytes, StandardCharsets.UTF_8);
    System.out.println(htmlOutput);

    assertTrue(htmlOutput.contains("<div id=\"app\"></div>"));
  }

  @Test
  void testServeOnlyContext_NotFoundOnPathTraversalAttempt() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("/apps/../../example-app");

    ServeStaticFile.serve(ctx);
    verify(ctx).status(403);
  }

  @Test
  void testServeOnlyContext_FileNotFound() {
    Context ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.path()).thenReturn("/non-existent-app");

    ServeStaticFile.serve(ctx);

    verify(ctx).status(404);
    verify(ctx, times(2)).result(NOT_FOUND + ctx.path());
  }
}
