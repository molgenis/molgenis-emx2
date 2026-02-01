package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.JavaScriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class MappingEngine {
  private static final Logger logger = LoggerFactory.getLogger(MappingEngine.class);
  private static final Pattern FOREACH_PATTERN = Pattern.compile("(\\w+)\\s+in\\s+(\\w+)");
  private static final int MAX_ROWS = 10000;
  private static final int MAX_NESTING_DEPTH = 10;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public JsonNode transform(String mappingYaml, JsonNode input) throws IOException {
    Yaml yaml = new Yaml();
    Map<String, Object> mappingDoc = yaml.load(mappingYaml);

    Map<String, String> prefixes = extractPrefixes(mappingDoc);
    Map<String, Object> contextDef = extractContext(mappingDoc);

    MappingScope baseScope = buildBaseScope(input);
    MappingScope contextScope = evaluateContext(baseScope, contextDef);

    ArrayNode graph = objectMapper.createArrayNode();

    for (Map.Entry<String, Object> entry : mappingDoc.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("_")) {
        continue;
      }

      String tableName = key;
      if (!(entry.getValue() instanceof Map)) {
        continue;
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> tableMapping = (Map<String, Object>) entry.getValue();

      processTable(tableName, tableMapping, input, contextScope, graph, 0);
    }

    ObjectNode result = objectMapper.createObjectNode();
    result.set("@context", buildJsonLdContext(prefixes));
    result.set("@graph", graph);

    return result;
  }

  private Map<String, String> extractPrefixes(Map<String, Object> mappingDoc) {
    Object prefixesObj = mappingDoc.get("_prefixes");
    if (prefixesObj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, String> prefixes = (Map<String, String>) prefixesObj;
      return prefixes;
    }
    return Collections.emptyMap();
  }

  private Map<String, Object> extractContext(Map<String, Object> mappingDoc) {
    Object contextObj = mappingDoc.get("_context");
    if (contextObj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> context = (Map<String, Object>) contextObj;
      return context;
    }
    return Collections.emptyMap();
  }

  private MappingScope buildBaseScope(JsonNode input) {
    MappingScope scope = new MappingScope();

    if (input.has("_request")) {
      scope.put("_request", convertToMap(input.get("_request")));
    }

    if (input.has("_params")) {
      scope.put("_params", convertToMap(input.get("_params")));
    }

    if (input.has("_schema")) {
      scope.put("_schema", convertToMap(input.get("_schema")));
    }

    if (input.has("_settings")) {
      JsonNode settingsArray = input.get("_settings");
      Map<String, String> settings = new LinkedHashMap<>();
      if (settingsArray.isArray()) {
        for (JsonNode item : settingsArray) {
          if (item.has("key") && item.has("value")) {
            settings.put(item.get("key").asText(), item.get("value").asText());
          }
        }
      }
      scope.put("settings", settings);
    }

    return scope;
  }

  private MappingScope evaluateContext(MappingScope baseScope, Map<String, Object> contextDef) {
    MappingScope contextScope = baseScope.child();

    for (Map.Entry<String, Object> entry : contextDef.entrySet()) {
      String varName = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof String && ((String) value).startsWith("=")) {
        String expr = ((String) value).substring(1);
        Object result = evaluateExpression(expr, baseScope);
        contextScope.put(varName, result);
      } else {
        contextScope.put(varName, value);
      }
    }

    return contextScope;
  }

  private void processTable(
      String tableName,
      Map<String, Object> tableMapping,
      JsonNode input,
      MappingScope contextScope,
      ArrayNode graph,
      int depth) {

    if (depth > MAX_NESTING_DEPTH) {
      throw new FairMapperException("Maximum nesting depth exceeded: " + MAX_NESTING_DEPTH);
    }

    JsonNode tableData = input.get(tableName);
    if (tableData == null || !tableData.isArray()) {
      return;
    }

    Map<String, Object> tableLet = extractLet(tableMapping);

    int rowCount = 0;
    for (JsonNode row : tableData) {
      if (++rowCount > MAX_ROWS) {
        throw new FairMapperException(
            "Maximum rows exceeded for table " + tableName + ": " + MAX_ROWS);
      }

      MappingScope rowDataScope = contextScope.child();
      Map<String, Object> rowData = convertToMap(row);
      for (Map.Entry<String, Object> entry : rowData.entrySet()) {
        rowDataScope.put(entry.getKey(), entry.getValue());
      }

      MappingScope rowScope = evaluateLet(rowDataScope, tableLet);

      ObjectNode rdfObject = processRow(tableMapping, rowScope, depth);
      if (rdfObject != null) {
        graph.add(rdfObject);
      }
    }
  }

  private Map<String, Object> extractLet(Map<String, Object> mapping) {
    Object letObj = mapping.get("_let");
    if (letObj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> let = (Map<String, Object>) letObj;
      return let;
    }
    return Collections.emptyMap();
  }

  private MappingScope evaluateLet(MappingScope parentScope, Map<String, Object> letDef) {
    MappingScope letScope = parentScope.child();

    for (Map.Entry<String, Object> entry : letDef.entrySet()) {
      String varName = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof String && ((String) value).startsWith("=")) {
        String expr = ((String) value).substring(1);
        Object result = evaluateExpression(expr, letScope);
        letScope.put(varName, result);
      } else {
        letScope.put(varName, value);
      }
    }

    return letScope;
  }

  private ObjectNode processRow(
      Map<String, Object> tableMapping, MappingScope rowScope, int depth) {
    String idExpr = (String) tableMapping.get("_id");
    if (idExpr == null) {
      return null;
    }

    Object idValue = evaluateValue(idExpr, rowScope);
    if (idValue == null) {
      logger.warn("Skipping row with null @id");
      return null;
    }

    ObjectNode rdfObject = objectMapper.createObjectNode();
    rdfObject.put("@id", idValue.toString());

    Object typeValue = tableMapping.get("_type");
    if (typeValue != null) {
      Object evaluatedType = evaluateValue(typeValue, rowScope);
      if (evaluatedType != null) {
        rdfObject.put("@type", evaluatedType.toString());
      }
    }

    for (Map.Entry<String, Object> entry : tableMapping.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("_")) {
        continue;
      }

      Object value = entry.getValue();
      processColumnMapping(key, value, rowScope, rdfObject, depth);
    }

    return rdfObject;
  }

  private void processColumnMapping(
      String columnName,
      Object mappingValue,
      MappingScope rowScope,
      ObjectNode rdfObject,
      int depth) {

    if (mappingValue instanceof String) {
      String predicate = (String) mappingValue;
      Object columnValue = rowScope.get(columnName);
      if (columnValue != null) {
        addProperty(rdfObject, predicate, columnValue);
      }
    } else if (mappingValue instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> mappingMap = (Map<String, Object>) mappingValue;

      String predicate = (String) mappingMap.get("predicate");
      if (predicate == null) {
        return;
      }

      if (mappingMap.containsKey("foreach")) {
        processForeach(columnName, mappingMap, rowScope, rdfObject, predicate, depth);
      } else if (mappingMap.containsKey("value")) {
        Object valueSpec = mappingMap.get("value");
        Map<String, Object> columnLet = extractLet(mappingMap);
        MappingScope columnScope = evaluateLet(rowScope, columnLet);

        Object evaluatedValue = evaluateValue(valueSpec, columnScope);
        if (evaluatedValue != null) {
          addProperty(rdfObject, predicate, evaluatedValue);
        }
      } else {
        Object columnValue = rowScope.get(columnName);
        if (columnValue != null) {
          addProperty(rdfObject, predicate, columnValue);
        }
      }
    }
  }

  private void processForeach(
      String columnName,
      Map<String, Object> mappingMap,
      MappingScope rowScope,
      ObjectNode rdfObject,
      String predicate,
      int depth) {

    String foreachExpr = (String) mappingMap.get("foreach");
    Matcher matcher = FOREACH_PATTERN.matcher(foreachExpr);
    if (!matcher.matches()) {
      throw new FairMapperException("Invalid foreach syntax: " + foreachExpr);
    }

    String loopVar = matcher.group(1);
    String arrayField = matcher.group(2);

    Object arrayObj = rowScope.get(arrayField);
    if (!(arrayObj instanceof List)) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Object> array = (List<Object>) arrayObj;

    Object valueSpec = mappingMap.get("value");
    if (valueSpec == null) {
      return;
    }

    Map<String, Object> columnLet = extractLet(mappingMap);

    ArrayNode arrayNode = objectMapper.createArrayNode();

    for (Object item : array) {
      MappingScope foreachScope = rowScope.child();
      foreachScope.put(loopVar, item);

      MappingScope columnScope = evaluateLet(foreachScope, columnLet);

      Object evaluatedValue = evaluateValue(valueSpec, columnScope);
      if (evaluatedValue != null) {
        if (evaluatedValue instanceof Map) {
          arrayNode.add(objectMapper.valueToTree(evaluatedValue));
        } else {
          arrayNode.add(evaluatedValue.toString());
        }
      }
    }

    if (arrayNode.size() > 0) {
      rdfObject.set(predicate, arrayNode);
    }
  }

  private Object evaluateValue(Object valueSpec, MappingScope scope) {
    if (valueSpec instanceof String) {
      String str = (String) valueSpec;
      if (str.startsWith("=")) {
        return evaluateExpression(str.substring(1), scope);
      } else {
        return str;
      }
    } else if (valueSpec instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) valueSpec;
      Map<String, Object> result = new LinkedHashMap<>();

      for (Map.Entry<String, Object> entry : map.entrySet()) {
        Object evaluatedValue = evaluateValue(entry.getValue(), scope);
        if (evaluatedValue != null) {
          result.put(entry.getKey(), evaluatedValue);
        }
      }
      return result;
    }
    return valueSpec;
  }

  private Object evaluateExpression(String expr, MappingScope scope) {
    try {
      return JavaScriptUtils.executeJavascriptOnMap(expr, scope.flatten());
    } catch (MolgenisException e) {
      throw new FairMapperException(
          "Expression evaluation failed: " + expr + " - " + e.getMessage());
    }
  }

  private void addProperty(ObjectNode node, String predicate, Object value) {
    if (value instanceof String) {
      node.put(predicate, (String) value);
    } else if (value instanceof Number) {
      if (value instanceof Integer) {
        node.put(predicate, (Integer) value);
      } else if (value instanceof Long) {
        node.put(predicate, (Long) value);
      } else if (value instanceof Double) {
        node.put(predicate, (Double) value);
      } else {
        node.put(predicate, value.toString());
      }
    } else if (value instanceof Boolean) {
      node.put(predicate, (Boolean) value);
    } else if (value instanceof Map) {
      node.set(predicate, objectMapper.valueToTree(value));
    } else if (value instanceof List) {
      node.set(predicate, objectMapper.valueToTree(value));
    } else {
      node.put(predicate, value.toString());
    }
  }

  private ObjectNode buildJsonLdContext(Map<String, String> prefixes) {
    ObjectNode context = objectMapper.createObjectNode();
    for (Map.Entry<String, String> entry : prefixes.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }
    return context;
  }

  private Map<String, Object> convertToMap(JsonNode node) {
    if (node == null || node.isNull()) {
      return Collections.emptyMap();
    }
    return objectMapper.convertValue(node, Map.class);
  }
}
