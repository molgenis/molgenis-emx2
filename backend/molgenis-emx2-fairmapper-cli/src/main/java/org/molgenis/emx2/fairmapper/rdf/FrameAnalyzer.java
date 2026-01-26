package org.molgenis.emx2.fairmapper.rdf;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FrameAnalyzer {

  public Map<Integer, List<String>> analyze(JsonNode frame, int maxDepth) {
    Map<Integer, List<String>> predicatesByDepth = new HashMap<>();
    analyzeRecursive(frame, 0, maxDepth, predicatesByDepth);
    return predicatesByDepth;
  }

  private void analyzeRecursive(
      JsonNode node, int currentDepth, int maxDepth, Map<Integer, List<String>> result) {
    if (currentDepth > maxDepth || node == null || !node.isObject()) {
      return;
    }

    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      String key = entry.getKey();
      JsonNode value = entry.getValue();

      if (key.startsWith("@")) {
        continue;
      }

      if (value.isObject()) {
        JsonNode embedNode = value.get("@embed");
        if (embedNode != null && "@always".equals(embedNode.asText())) {
          result.computeIfAbsent(currentDepth, k -> new ArrayList<>()).add(key);
          analyzeRecursive(value, currentDepth + 1, maxDepth, result);
        }
      }
    }
  }
}
