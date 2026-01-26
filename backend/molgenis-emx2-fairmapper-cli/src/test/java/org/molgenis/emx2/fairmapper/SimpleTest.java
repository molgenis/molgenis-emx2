package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class SimpleTest {
  @Test
  void testJsltBasic() throws Exception {
    String jslt = "{\"result\": .input + 1}";
    String input = "{\"input\": 5}";

    ObjectMapper mapper = new ObjectMapper();
    Expression expr = Parser.compileString(jslt);
    JsonNode inputNode = mapper.readTree(input);
    JsonNode output = expr.apply(inputNode);

    assertEquals(6, output.get("result").asInt());
  }

  @Test
  void testJsltEmptyArrayPreserved() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    // with withObjectFilter("true"), empty arrays are preserved
    String jslt = "{\"arr\": [for (.items) .]}";
    String input = "{\"items\": []}";
    Expression expr = new Parser(new StringReader(jslt)).withObjectFilter("true").compile();
    JsonNode output = expr.apply(mapper.readTree(input));

    assertTrue(output.has("arr"), "empty array should be preserved");
    assertTrue(output.get("arr").isArray(), "arr should be array");
    assertEquals(0, output.get("arr").size(), "arr should be empty");
  }
}
