package org.molgenis.emx2.fairmapper.model.step;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public class StepConfigDeserializer extends JsonDeserializer<List<StepConfig>> {
  @Override
  public List<StepConfig> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    List<StepConfig> steps = new ArrayList<>();

    for (JsonNode stepNode : node) {
      List<TestCase> tests = parseTests(stepNode.get("tests"));

      if (stepNode.has("transform")) {
        steps.add(new TransformStep(stepNode.get("transform").asText(), tests));
      } else if (stepNode.has("query")) {
        steps.add(new QueryStep(stepNode.get("query").asText(), tests));
      } else if (stepNode.has("mutate")) {
        steps.add(new MutateStep(stepNode.get("mutate").asText()));
      } else if (stepNode.has("sql")) {
        steps.add(new SqlQueryStep(stepNode.get("sql").asText(), tests));
      } else if (stepNode.has("frame")) {
        Boolean unmapped = stepNode.has("unmapped") ? stepNode.get("unmapped").asBoolean() : null;
        steps.add(new FrameStep(stepNode.get("frame").asText(), unmapped, tests));
      } else if (stepNode.has("sparql")) {
        steps.add(new SparqlConstructStep(stepNode.get("sparql").asText(), tests));
      }
    }
    return steps;
  }

  private List<TestCase> parseTests(JsonNode testsNode) {
    if (testsNode == null || !testsNode.isArray()) return List.of();
    List<TestCase> tests = new ArrayList<>();
    for (JsonNode t : testsNode) {
      tests.add(
          new TestCase(
              t.has("input") ? t.get("input").asText() : null,
              t.has("output") ? t.get("output").asText() : null));
    }
    return tests;
  }
}
