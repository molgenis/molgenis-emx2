package org.molgenis.emx2.web.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.web.util.HttpHeaderUtils.getContentType;

import com.google.common.net.MediaType;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.NotAcceptableResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpHeaderUtilsTest {
  private final MediaType json = MediaType.parse("application/json");
  private final MediaType plain = MediaType.parse("text/plain");
  private final MediaType plainCharset = MediaType.parse("text/plain; charset=utf-8");
  private final MediaType plainFormat = MediaType.parse("text/plain; format=flowed");
  private final MediaType plainCharsetFormat =
      MediaType.parse("text/plain; charset=utf-8; format=flowed");
  private final MediaType plainInvalidAllowed = MediaType.parse("text/plain; q=0.5");
  private final MediaType html = MediaType.parse("text/html");
  private final MediaType jpeg = MediaType.parse("image/jpeg");

  @Test
  void testContentType() throws Exception {
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

    when(ctx.header(Header.ACCEPT))
        .thenReturn("text/plain; q=0.5, text/plain; format=flowed; q=0.8, text/html; q=0.2");
    assertEquals(plain, getContentType(ctx, List.of(html, plain)));

    when(ctx.header(Header.ACCEPT))
        .thenReturn(
            "text/plain; q=0.2, text/plain; format=flowed; q=0.8, */*; q=0.1, text/html; q=0.5, text/*; q=0.7");
    assertEquals(plainFormat, getContentType(ctx, List.of(json, html, plain, plainFormat)));

    when(ctx.header(Header.ACCEPT))
        .thenReturn(
            "text/plain; format=flowed; q=0.8, text/plain; q=0.2, */*; q=0.1, text/html; q=0.5, text/*; q=0.7");
    assertEquals(plainFormat, getContentType(ctx, List.of(json, html, plain, plainFormat)));

    when(ctx.header(Header.ACCEPT))
        .thenReturn(
            "text/plain; format=flowed; charset=utf-8; q=0.8, text/plain; q=0.2, text/plain; format=flowed; q=0.6");
    assertEquals(
        plainCharsetFormat,
        getContentType(ctx, List.of(json, html, plain, plainFormat, plainCharsetFormat)));

    when(ctx.header(Header.ACCEPT))
        .thenReturn("text/*; charset=utf-8; q=0.8, text/*; q=0.6, text/plain; q=0.4");
    assertEquals(plainCharset, getContentType(ctx, List.of(plain, plainCharset, html)));

    when(ctx.header(Header.ACCEPT))
        .thenReturn("text/*; charset=utf-8; q=0.8, text/*; q=0.6, text/plain; q=0.4");
    assertEquals(html, getContentType(ctx, List.of(plain, html)));

    // "text/plain q=0.8" has a typo: missing ";"
    when(ctx.header(Header.ACCEPT)).thenReturn("text/plain q=0.8, text/html; q=0.5");
    assertThrows(
        IllegalArgumentException.class, () -> getContentType(ctx, List.of(json, html, plain)));

    // if media type negotiation fails
    when(ctx.header(Header.ACCEPT)).thenReturn("image/jpeg");
    assertNull(getContentType(ctx, List.of(json, html, plain)));
  }
}
