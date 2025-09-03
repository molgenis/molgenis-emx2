package org.molgenis.emx2.web.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.web.util.HttpHeaderUtils.getContentType;

import com.google.common.net.MediaType;
import io.javalin.http.Context;
import io.javalin.http.Header;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpHeaderUtilsTest {
  private final MediaType json = MediaType.parse("application/json");
  private final MediaType plain = MediaType.parse("text/plain");
  private final MediaType html = MediaType.parse("text/html");

  @Test
  void testContentType() throws Exception {
    Context ctx = mock(Context.class);

    when(ctx.header(Header.ACCEPT))
        .thenReturn("application/json; q=0.5, text/plain; q=0.8, image/jpeg");
    assertEquals(json, getContentType(ctx, List.of(json, html)));

    when(ctx.header(Header.ACCEPT)).thenReturn("application/json; q=0.5, text/*; q=0.8");
    assertEquals(html, getContentType(ctx, List.of(json, html)));

    when(ctx.header(Header.ACCEPT)).thenReturn("application/json; q=0.5, text/*; q=0.8");
    assertEquals(plain, getContentType(ctx, List.of(json, plain, html)));

    when(ctx.header(Header.ACCEPT)).thenReturn("application/json; q=0.5, text/*; q=0.8");
    assertEquals(html, getContentType(ctx, List.of(json, html, plain)));

    when(ctx.header(Header.ACCEPT))
        .thenReturn("application/json; q=0.5, text/*; q=0.8, text/html; q=0.5");
    assertEquals(plain, getContentType(ctx, List.of(json, html, plain)));

    when(ctx.header(Header.ACCEPT))
        .thenReturn("application/json; q=0.5, text/*; q=0.8, text/plain; q=0.9");
    assertEquals(plain, getContentType(ctx, List.of(json, html, plain)));
  }
}
