package org.molgenis.emx2.semantics.beaconv2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Configuration;
import org.molgenis.emx2.json.JsonUtil;

public class Beaconv2_ConfigurationTest {

  @Test
  public void testConfiguration() throws Exception {
    Configuration c = new Configuration();
    String json = JsonUtil.getWriter().writeValueAsString(c);

    // first line
    assertTrue(json.contains("\"meta\" : {"));

    // last line (except for closing braces)
    assertTrue(json.contains("\"label\" : \"Sequencing run\""));

    // return config schema
    assertTrue(
        json.contains(
            "    \"returnedSchemas\" : [\n"
                + "      {\n"
                + "        \"entityType\" : \"configuration\","));

    // check ids of all possible return types
    assertTrue(json.contains("\"id\" : \"analysis\","));
    assertTrue(json.contains("\"id\" : \"biosample\","));
    assertTrue(json.contains("\"id\" : \"cohort\","));
    assertTrue(json.contains("\"id\" : \"dataset\","));
    assertTrue(json.contains("\"id\" : \"genomicVariant\","));
    assertTrue(json.contains("\"id\" : \"individual\","));
    assertTrue(json.contains("\"id\" : \"run\","));
  }
}
