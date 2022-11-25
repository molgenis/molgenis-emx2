package org.molgenis.emx2.catalogue;

import static org.junit.Assert.*;

import java.net.http.HttpResponse;
import org.junit.Test;

public class CountRequestTest {

  @Test
  public void send() {
    HttpResponse response = new CountRequest().send();
    assertEquals(200, response.statusCode());
  }
}
