package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;

class HpcHeadersTest {

  @Test
  void validateAll_acceptsValidHeaders() {
    Context ctx = mock(Context.class);
    when(ctx.header(ApiVersion.HEADER_NAME)).thenReturn(ApiVersion.CURRENT);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("550e8400-e29b-41d4-a716-446655440000");
    when(ctx.header(HpcHeaders.TIMESTAMP)).thenReturn("1700000000");

    assertDoesNotThrow(() -> HpcHeaders.validateAll(ctx));
  }

  @Test
  void validateAll_throwsWhenRequestIdMissing() {
    Context ctx = mock(Context.class);
    when(ctx.header(ApiVersion.HEADER_NAME)).thenReturn(ApiVersion.CURRENT);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> HpcHeaders.validateAll(ctx));
    assertTrue(ex.getMessage().contains("Missing required header"));
  }

  @Test
  void validateAll_throwsForInvalidRequestIdFormat() {
    Context ctx = mock(Context.class);
    when(ctx.header(ApiVersion.HEADER_NAME)).thenReturn(ApiVersion.CURRENT);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("not-a-uuid");
    when(ctx.header(HpcHeaders.TIMESTAMP)).thenReturn("1700000000");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> HpcHeaders.validateAll(ctx));
    assertTrue(ex.getMessage().contains("Invalid X-Request-Id"));
  }

  @Test
  void validateAll_throwsForInvalidTimestamp() {
    Context ctx = mock(Context.class);
    when(ctx.header(ApiVersion.HEADER_NAME)).thenReturn(ApiVersion.CURRENT);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("550e8400-e29b-41d4-a716-446655440000");
    when(ctx.header(HpcHeaders.TIMESTAMP)).thenReturn("not-a-number");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> HpcHeaders.validateAll(ctx));
    assertTrue(ex.getMessage().contains("Invalid X-Timestamp"));
  }

  @Test
  void validateAll_throwsForNegativeTimestamp() {
    Context ctx = mock(Context.class);
    when(ctx.header(ApiVersion.HEADER_NAME)).thenReturn(ApiVersion.CURRENT);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("550e8400-e29b-41d4-a716-446655440000");
    when(ctx.header(HpcHeaders.TIMESTAMP)).thenReturn("-1");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> HpcHeaders.validateAll(ctx));
    assertTrue(ex.getMessage().contains("Invalid X-Timestamp"));
  }

  @Test
  void validateAll_throwsWhenApiVersionMissing() {
    Context ctx = mock(Context.class);
    when(ctx.header(ApiVersion.HEADER_NAME)).thenReturn(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> HpcHeaders.validateAll(ctx));
    assertTrue(ex.getMessage().contains(ApiVersion.HEADER_NAME));
  }

  @Test
  void requireWorkerId_returnsValue() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.WORKER_ID)).thenReturn("worker-1");
    assertEquals("worker-1", HpcHeaders.requireWorkerId(ctx));
  }

  @Test
  void requireWorkerId_throwsWhenMissing() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.WORKER_ID)).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () -> HpcHeaders.requireWorkerId(ctx));
  }

  @Test
  void getWorkerId_returnsNullWhenMissing() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.WORKER_ID)).thenReturn(null);
    assertNull(HpcHeaders.getWorkerId(ctx));
  }
}
