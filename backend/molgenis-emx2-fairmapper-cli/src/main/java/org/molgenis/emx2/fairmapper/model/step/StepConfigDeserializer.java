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

      if (stepNode.has("fetch")) {
        steps.add(
            new FetchStep(
                stepNode.get("fetch").asText(),
                stepNode.has("accept") ? stepNode.get("accept").asText() : null,
                stepNode.has("frame") ? stepNode.get("frame").asText() : null,
                stepNode.has("maxDepth") ? stepNode.get("maxDepth").asInt() : null,
                stepNode.has("maxCalls") ? stepNode.get("maxCalls").asInt() : null,
                tests));
      } else if (stepNode.has("transform")) {
        steps.add(new TransformStep(stepNode.get("transform").asText(), tests));
      } else if (stepNode.has("query")) {
        steps.add(new QueryStep(stepNode.get("query").asText(), tests));
      } else if (stepNode.has("mutate")) {
        steps.add(new MutateStep(stepNode.get("mutate").asText()));
      } else if (stepNode.has("output-rdf")) {
        String format =
            stepNode.get("output-rdf").isTextual()
                ? stepNode.get("output-rdf").asText()
                : stepNode.get("output-rdf").get("format").asText();
        steps.add(new OutputRdfStep(format, tests));
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
