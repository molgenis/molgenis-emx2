package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HpcExceptionTest {

  @Test
  void factoryMethods_setCorrectStatusAndTitle() {
    assertEquals(400, HpcException.badRequest("d", "r").getStatus());
    assertEquals(401, HpcException.unauthorized("d", "r").getStatus());
    assertEquals(403, HpcException.forbidden("d", "r").getStatus());
    assertEquals(404, HpcException.notFound("d", "r").getStatus());
    assertEquals(409, HpcException.conflict("d", "r").getStatus());
    assertEquals(413, HpcException.payloadTooLarge("d", "r").getStatus());
    assertEquals(500, HpcException.internal("d", "r").getStatus());
    assertEquals(503, HpcException.serviceUnavailable("d", "r").getStatus());
  }

  @Test
  void toProblemDetail_containsRequiredFields() {
    HpcException ex = HpcException.conflict("Cannot transition", "req-42");
    Map<String, Object> body = ex.toProblemDetail();
    assertEquals("about:blank", body.get("type"));
    assertEquals("Conflict", body.get("title"));
    assertEquals(409, body.get("status"));
    assertEquals("Cannot transition", body.get("detail"));
    assertEquals("urn:request:req-42", body.get("instance"));
  }

  @Test
  void toProblemDetail_omitsInstanceWhenNoRequestId() {
    HpcException ex = HpcException.badRequest("bad", null);
    Map<String, Object> body = ex.toProblemDetail();
    assertFalse(body.containsKey("instance"));
    assertEquals("Bad Request", body.get("title"));
  }

  @Test
  void causeCarryingConstructor_preservesCause() {
    RuntimeException cause = new RuntimeException("original");
    HpcException ex = HpcException.unauthorized("auth failed", "r1", cause);
    assertSame(cause, ex.getCause());
    assertEquals(401, ex.getStatus());
  }

  @Test
  void serviceUnavailable_withCause() {
    IllegalStateException cause = new IllegalStateException("key missing");
    HpcException ex = HpcException.serviceUnavailable("unavailable", "r2", cause);
    assertSame(cause, ex.getCause());
    assertEquals(503, ex.getStatus());
  }

  @Test
  void getters() {
    HpcException ex = new HpcException(418, "Teapot", "I'm a teapot", "req-1");
    assertEquals(418, ex.getStatus());
    assertEquals("Teapot", ex.getTitle());
    assertEquals("req-1", ex.getRequestId());
    assertEquals("I'm a teapot", ex.getMessage());
  }
}
