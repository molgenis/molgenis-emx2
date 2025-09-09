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
import org.molgenis.emx2.MolgenisException;

class HttpHeaderUtilsTest {
  private final MediaType json = MediaType.parse("application/json");
  private final MediaType plain = MediaType.parse("text/plain");
  private final MediaType plainCharset = MediaType.parse("text/plain; charset=utf-8");
  private final MediaType html = MediaType.parse("text/html");

  @Test
  void testContentType() {
    Context ctx = mock(Context.class);

    when(ctx.header(Header.ACCEPT))
        .thenReturn("application/json; q=0.5, text/plain; q=0.8, image/jpeg");
    assertEquals(json, getContentType(ctx, List.of(json, html)));

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

    when(ctx.header(Header.ACCEPT))
        .thenReturn("application/*; q=0.7, text/html; q=0.5, */*; q=0.8");
    assertEquals(plain, getContentType(ctx, List.of(json, html, plain)));

    // charset=utf-8 should take priority over non-charset version
    when(ctx.header(Header.ACCEPT))
        .thenReturn("text/*; q=0.4, text/plain; charset=utf-8; q=0.8, application/json; q=0.6");
    assertEquals(plain, getContentType(ctx, List.of(json, html, plain)));

    // "text/plain q=0.8" has a typo: missing ";"
    when(ctx.header(Header.ACCEPT)).thenReturn("text/plain q=0.8, text/html; q=0.5");
    assertThrows(MolgenisException.class, () -> getContentType(ctx, List.of(json, html, plain)));

    // if allowed media types is empty
    when(ctx.header(Header.ACCEPT)).thenReturn("image/jpeg");
    assertThrows(IllegalArgumentException.class, () -> getContentType(ctx, List.of()));

    // if allowed media types contain parameters
    when(ctx.header(Header.ACCEPT)).thenReturn("image/jpeg");
    assertThrows(
        IllegalArgumentException.class, () -> getContentType(ctx, List.of(plain, plainCharset)));

    // if media type negotiation fails due to non-supported media type
    when(ctx.header(Header.ACCEPT)).thenReturn("image/jpeg");
    assertNull(getContentType(ctx, List.of(json, html, plain)));

    // if media type negotiation fails due to non-supported charset
    when(ctx.header(Header.ACCEPT)).thenReturn("text/plain; charset=utf-16");
    assertNull(getContentType(ctx, List.of(html, plain)));
  }
}
