package org.molgenis.emx2.catalogue;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Test;

public class CountRequestTest {

  @Test
  public void send() throws IOException, URISyntaxException, InterruptedException {
    JsonNode jsonNode = new CountRequest().send();
    assertEquals("{\"data\":{\"Cohorts_agg\":{\"count\":61}}}", jsonNode.toString());
  }
}
