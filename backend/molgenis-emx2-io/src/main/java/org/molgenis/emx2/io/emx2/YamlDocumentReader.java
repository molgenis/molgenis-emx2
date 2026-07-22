package org.molgenis.emx2.io.emx2;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.molgenis.emx2.MolgenisException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Position-aware, strict YAML reader for the model bundle format. Wraps SnakeYAML's node graph so
 * every value keeps its real source line and column, and every mapping is checked against the
 * enumerated attribute surface. Later tickets (inheritance, reuse, dotted refs, i18n) extend the
 * allowed-key sets and add mapping behavior; the reading primitives here stay the same.
 */
public class YamlDocumentReader {

  static final char LOCALE_SEPARATOR = '@';
  private static final String LABEL = "label";
  private static final String DESCRIPTION = "description";

  private final String fileLabel;

  public YamlDocumentReader(String fileLabel) {
    this.fileLabel = fileLabel;
  }

  public String getFileLabel() {
    return fileLabel;
  }

  public Node compose(String content) {
    return new Yaml().compose(new StringReader(content));
  }

  public LinkedHashMap<String, Node> mapping(Node node, String path, Set<String> allowedKeys) {
    if (!(node instanceof MappingNode mappingNode)) {
      throw error("expected a mapping at '" + path + "'", node);
    }
    LinkedHashMap<String, Node> result = new LinkedHashMap<>();
    for (NodeTuple tuple : mappingNode.getValue()) {
      String key = scalar(tuple.getKeyNode(), path);
      if (!isAllowed(key, allowedKeys)) {
        throw error(
            "unknown attribute '" + key + "' at '" + path + "." + key + "'", tuple.getKeyNode());
      }
      result.put(key, tuple.getValueNode());
    }
    return result;
  }

  private static boolean isAllowed(String key, Set<String> allowedKeys) {
    if (allowedKeys.contains(key)) {
      return true;
    }
    int separator = key.indexOf(LOCALE_SEPARATOR);
    if (separator <= 0) {
      return false;
    }
    String base = key.substring(0, separator);
    return (LABEL.equals(base) || DESCRIPTION.equals(base)) && allowedKeys.contains(base);
  }

  public boolean isScalar(Node node) {
    return node instanceof ScalarNode;
  }

  public Set<String> mappingKeys(Node node) {
    LinkedHashMap<String, Node> keys = new LinkedHashMap<>();
    if (node instanceof MappingNode mappingNode) {
      for (NodeTuple tuple : mappingNode.getValue()) {
        keys.put(scalar(tuple.getKeyNode(), ""), tuple.getKeyNode());
      }
    }
    return keys.keySet();
  }

  public String scalar(Node node, String path) {
    if (!(node instanceof ScalarNode scalarNode)) {
      throw error("expected a scalar value at '" + path + "'", node);
    }
    return scalarNode.getValue();
  }

  public int integer(Node node, String path) {
    String raw = scalar(node, path);
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException exception) {
      throw error("expected a whole number at '" + path + "' but found '" + raw + "'", node);
    }
  }

  public List<Node> sequence(Node node, String path) {
    if (!(node instanceof SequenceNode sequenceNode)) {
      throw error("expected a list at '" + path + "'", node);
    }
    return sequenceNode.getValue();
  }

  public List<String> stringList(Node node, String path) {
    List<String> result = new ArrayList<>();
    for (Node item : sequence(node, path)) {
      result.add(scalar(item, path));
    }
    return result;
  }

  public LinkedHashMap<String, String> scalarMapping(Node node, String path) {
    if (!(node instanceof MappingNode mappingNode)) {
      throw error("expected a mapping at '" + path + "'", node);
    }
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    for (NodeTuple tuple : mappingNode.getValue()) {
      result.put(scalar(tuple.getKeyNode(), path), scalar(tuple.getValueNode(), path));
    }
    return result;
  }

  public MolgenisException error(String message, Node node) {
    Mark mark = node == null ? null : node.getStartMark();
    int line = mark == null ? -1 : mark.getLine() + 1;
    int column = mark == null ? -1 : mark.getColumn() + 1;
    return new MolgenisException(
        fileLabel + ": " + message + " (line " + line + ", column " + column + ")");
  }
}
