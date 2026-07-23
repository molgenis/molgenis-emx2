package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.io.emx2.ModelSchemaRules.AttributeType;

public final class ModelSchemaGenerator {

  public static final String ARTIFACT_RESOURCE = "/molgenis-model.schema.json";

  private static final String SCHEMA = "$schema";
  private static final String ID = "$id";
  private static final String TITLE = "title";
  private static final String DEFS = "$defs";
  private static final String REF = "$ref";
  private static final String TYPE = "type";
  private static final String PROPERTIES = "properties";
  private static final String ITEMS = "items";
  private static final String REQUIRED = "required";
  private static final String ENUM = "enum";
  private static final String ANY_OF = "anyOf";
  private static final String ALL_OF = "allOf";
  private static final String IF = "if";
  private static final String THEN = "then";
  private static final String PATTERN_PROPERTIES = "patternProperties";
  private static final String PATTERN = "pattern";
  private static final String UNEVALUATED = "unevaluatedProperties";
  private static final String ADDITIONAL_PROPERTIES = "additionalProperties";
  private static final String VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+$";

  private static final String T_STRING = "string";
  private static final String T_ARRAY = "array";
  private static final String T_OBJECT = "object";
  private static final String T_INTEGER = "integer";
  private static final String T_BOOLEAN = "boolean";

  private static final String DEF_COLUMN_ENTRY = "columnEntry";
  private static final String DEF_COLUMN_DEFINITION = "columnDefinition";
  private static final String DEF_HEADING = "heading";
  private static final String DEF_SUBTABLE = "subtable";
  private static final String DEF_TABLE_FILE = "tableFile";
  private static final String DEF_SHARED_FILE = "sharedFile";
  private static final String DEF_TABLE_ENTRY = "tableEntry";
  private static final String DEF_COMPANION_ENTRY = "companionEntry";
  private static final String DEF_BUNDLE = "bundle";

  private static final String LABEL_PATTERN = "^label@[A-Za-z][A-Za-z0-9-]*$";
  private static final String DESCRIPTION_PATTERN = "^description@[A-Za-z][A-Za-z0-9-]*$";

  private ModelSchemaGenerator() {}

  public static String generate() {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put(SCHEMA, "https://json-schema.org/draft/2020-12/schema");
    root.put(ID, "https://molgenis.org/molgenis-model.schema.json");
    root.put(TITLE, "MOLGENIS model bundle");

    Map<String, Object> defs = new LinkedHashMap<>();
    defs.put(DEF_COLUMN_ENTRY, columnEntry());
    defs.put(DEF_COLUMN_DEFINITION, columnDefinition());
    defs.put(DEF_HEADING, heading());
    defs.put(DEF_SUBTABLE, subtable());
    defs.put(DEF_TABLE_FILE, tableFile());
    defs.put(DEF_SHARED_FILE, sharedFile());
    defs.put(DEF_TABLE_ENTRY, tableEntry());
    defs.put(DEF_COMPANION_ENTRY, companionEntry());
    defs.put(DEF_BUNDLE, bundle());
    root.put(DEFS, defs);

    root.put(ANY_OF, List.of(ref(DEF_BUNDLE), ref(DEF_TABLE_FILE), ref(DEF_SHARED_FILE)));
    return serialize(root);
  }

  private static Map<String, Object> columnEntry() {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put(ANY_OF, List.of(scalarString(), ref(DEF_COLUMN_DEFINITION), ref(DEF_HEADING)));
    return entry;
  }

  private static Map<String, Object> columnDefinition() {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (String attribute : ModelSchemaRules.baseColumnAttributes()) {
      properties.put(attribute, propertySchema(ModelSchemaRules.typeOf(attribute)));
    }

    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(TYPE, T_OBJECT);
    definition.put(REQUIRED, List.of(ModelSchemaRules.NAME));
    definition.put(PROPERTIES, properties);
    definition.put(PATTERN_PROPERTIES, localizedPatterns());
    definition.put(ALL_OF, List.of(referenceConditional(), valueConditional()));
    definition.put(UNEVALUATED, false);
    return definition;
  }

  private static Map<String, Object> referenceConditional() {
    Map<String, Object> then = new LinkedHashMap<>();
    Map<String, Object> thenProperties = new LinkedHashMap<>();
    for (String attribute : new java.util.TreeSet<>(ModelSchemaRules.REFERENCE_ONLY_ATTRIBUTES)) {
      thenProperties.put(attribute, propertySchema(ModelSchemaRules.typeOf(attribute)));
    }
    then.put(PROPERTIES, thenProperties);
    return conditional(ModelSchemaRules.referenceTypeNames(), then);
  }

  private static Map<String, Object> valueConditional() {
    Map<String, Object> then = new LinkedHashMap<>();
    Map<String, Object> thenProperties = new LinkedHashMap<>();
    for (String attribute : ModelSchemaRules.VALUE_ONLY_ATTRIBUTES) {
      thenProperties.put(attribute, propertySchema(ModelSchemaRules.typeOf(attribute)));
    }
    then.put(PROPERTIES, thenProperties);
    return conditional(ModelSchemaRules.valueTypeNames(), then);
  }

  private static Map<String, Object> conditional(List<String> typeNames, Map<String, Object> then) {
    Map<String, Object> typeMatch = new LinkedHashMap<>();
    typeMatch.put(ENUM, typeNames);
    Map<String, Object> ifProperties = new LinkedHashMap<>();
    ifProperties.put(ModelSchemaRules.TYPE, typeMatch);
    Map<String, Object> ifClause = new LinkedHashMap<>();
    ifClause.put(REQUIRED, List.of(ModelSchemaRules.TYPE));
    ifClause.put(PROPERTIES, ifProperties);
    Map<String, Object> conditional = new LinkedHashMap<>();
    conditional.put(IF, ifClause);
    conditional.put(THEN, then);
    return conditional;
  }

  private static Map<String, Object> heading() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(ModelSchemaRules.SECTION, scalarString());
    properties.put(ModelSchemaRules.HEADING, scalarString());
    properties.put("visible", scalarString());
    properties.put(ModelSchemaRules.LABEL, scalarString());
    properties.put(ModelSchemaRules.DESCRIPTION, scalarString());
    properties.put(ModelSchemaRules.SEMANTICS, stringArray());
    properties.put(ModelSchemaRules.PROFILES, stringArray());
    properties.put("subclass", scalarString());
    properties.put("module", scalarString());
    verifyCovers(properties.keySet(), Emx2Yaml.HEADING_KEYS, DEF_HEADING);

    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(TYPE, T_OBJECT);
    definition.put(
        ANY_OF, List.of(requires(ModelSchemaRules.SECTION), requires(ModelSchemaRules.HEADING)));
    definition.put(PROPERTIES, properties);
    definition.put(PATTERN_PROPERTIES, localizedPatterns());
    definition.put(UNEVALUATED, false);
    return definition;
  }

  private static Map<String, Object> subtable() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(ModelSchemaRules.NAME, scalarString());
    properties.put(ModelSchemaRules.EXTENDS, stringOrStringArray());
    properties.put(ModelSchemaRules.LABEL, scalarString());
    properties.put(ModelSchemaRules.DESCRIPTION, scalarString());
    properties.put(ModelSchemaRules.TABLE_TYPE, tableTypeEnum());
    properties.put(ModelSchemaRules.SEMANTICS, stringArray());
    properties.put(ModelSchemaRules.PROFILES, stringArray());
    properties.put(ModelSchemaRules.DROP, scalarType(T_BOOLEAN));
    verifyCovers(properties.keySet(), Emx2Yaml.SUBTABLE_KEYS, DEF_SUBTABLE);

    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(TYPE, T_OBJECT);
    definition.put(REQUIRED, List.of(ModelSchemaRules.NAME));
    definition.put(PROPERTIES, properties);
    definition.put(PATTERN_PROPERTIES, localizedPatterns());
    definition.put(UNEVALUATED, false);
    return definition;
  }

  private static Map<String, Object> tableFile() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(ModelSchemaRules.NAME, scalarString());
    properties.put(ModelSchemaRules.LABEL, scalarString());
    properties.put(ModelSchemaRules.DESCRIPTION, scalarString());
    properties.put(ModelSchemaRules.SETTINGS, objectSchema());
    properties.put(ModelSchemaRules.COLUMNS, arrayOf(ref(DEF_COLUMN_ENTRY)));
    properties.put(ModelSchemaRules.EXTENDS, stringOrStringArray());
    properties.put(ModelSchemaRules.TABLE_TYPE, tableTypeEnum());
    properties.put(ModelSchemaRules.SUBCLASSES, arrayOf(ref(DEF_SUBTABLE)));
    properties.put(ModelSchemaRules.MODULES, arrayOf(ref(DEF_SUBTABLE)));
    properties.put(ModelSchemaRules.SEMANTICS, stringArray());
    properties.put(ModelSchemaRules.PROFILES, stringArray());
    properties.put(ModelSchemaRules.IMPORTS, stringArray());
    properties.put(ModelSchemaRules.DROP, scalarType(T_BOOLEAN));
    verifyCovers(properties.keySet(), Emx2Yaml.TABLE_KEYS, DEF_TABLE_FILE);

    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(TYPE, T_OBJECT);
    definition.put(REQUIRED, List.of(ModelSchemaRules.NAME));
    definition.put(PROPERTIES, properties);
    definition.put(PATTERN_PROPERTIES, localizedPatterns());
    definition.put(UNEVALUATED, false);
    return definition;
  }

  private static Map<String, Object> sharedFile() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(ModelSchemaRules.COLUMNS, arrayOf(ref(DEF_COLUMN_ENTRY)));

    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(TYPE, T_OBJECT);
    definition.put(REQUIRED, List.of(ModelSchemaRules.COLUMNS));
    definition.put(PROPERTIES, properties);
    definition.put(UNEVALUATED, false);
    return definition;
  }

  private static Map<String, Object> tableEntry() {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put(ANY_OF, List.of(scalarString(), ref(DEF_TABLE_FILE)));
    return entry;
  }

  private static Map<String, Object> companionEntry() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(ModelSchemaRules.TABLES, arrayOf(ref(DEF_TABLE_ENTRY)));
    properties.put(Emx2Yaml.KEY_DATA, stringOrStringArray());
    properties.put(Emx2Yaml.KEY_DEMO, stringOrStringArray());
    properties.put(ModelSchemaRules.SETTINGS, objectSchema());
    properties.put(ModelSchemaRules.VERSION, versionString());
    properties.put(Emx2Yaml.KEY_PERMISSIONS, objectSchema());
    properties.put(Emx2Yaml.KEY_BUNDLE, scalarString());
    verifyCovers(properties.keySet(), Emx2Yaml.COMPANION_KEYS, DEF_COMPANION_ENTRY);

    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(TYPE, T_OBJECT);
    definition.put(PROPERTIES, properties);
    definition.put(UNEVALUATED, false);
    return definition;
  }

  private static Map<String, Object> bundle() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(ModelSchemaRules.FORMAT_VERSION, integerSchema());
    properties.put(ModelSchemaRules.VERSION, versionString());
    properties.put(ModelSchemaRules.TABLES, arrayOf(ref(DEF_TABLE_ENTRY)));
    properties.put(ModelSchemaRules.SETTINGS, objectSchema());
    properties.put(ModelSchemaRules.IMPORTS, stringArray());
    properties.put(ModelSchemaRules.NAMESPACES, objectSchema());
    properties.put(Emx2Yaml.KEY_ADDITIONAL_SCHEMAS, mapOf(ref(DEF_COMPANION_ENTRY)));
    properties.put(Emx2Yaml.KEY_PERMISSIONS, objectSchema());
    properties.put(Emx2Yaml.KEY_DATA, stringOrStringArray());
    properties.put(Emx2Yaml.KEY_DEMO, stringOrStringArray());
    verifyCovers(properties.keySet(), Emx2Yaml.BUNDLE_KEYS, DEF_BUNDLE);

    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put(TYPE, T_OBJECT);
    definition.put(PROPERTIES, properties);
    definition.put(UNEVALUATED, false);
    return definition;
  }

  private static Map<String, Object> localizedPatterns() {
    Map<String, Object> patterns = new LinkedHashMap<>();
    patterns.put(LABEL_PATTERN, scalarString());
    patterns.put(DESCRIPTION_PATTERN, scalarString());
    return patterns;
  }

  private static Map<String, Object> requires(String key) {
    Map<String, Object> clause = new LinkedHashMap<>();
    clause.put(REQUIRED, List.of(key));
    return clause;
  }

  private static Map<String, Object> propertySchema(AttributeType attributeType) {
    return switch (attributeType) {
      case STRING -> scalarString();
      case INTEGER -> integerSchema();
      case BOOLEAN -> scalarType(T_BOOLEAN);
      case BOOLEAN_OR_EXPRESSION -> booleanOrExpression();
      case STRING_ARRAY -> stringArray();
      case OBJECT -> objectSchema();
      case COLUMN_TYPE_ENUM -> columnTypeEnum();
    };
  }

  private static Map<String, Object> scalarString() {
    return scalarType(T_STRING);
  }

  private static Map<String, Object> versionString() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(TYPE, T_STRING);
    schema.put(PATTERN, VERSION_PATTERN);
    return schema;
  }

  private static Map<String, Object> integerSchema() {
    return scalarType(T_INTEGER);
  }

  private static Map<String, Object> objectSchema() {
    return scalarType(T_OBJECT);
  }

  private static Map<String, Object> scalarType(String jsonType) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(TYPE, jsonType);
    return schema;
  }

  private static Map<String, Object> booleanOrExpression() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(TYPE, List.of(T_BOOLEAN, T_STRING));
    return schema;
  }

  private static Map<String, Object> columnTypeEnum() {
    return caseInsensitiveEnum(ModelSchemaRules.columnTypeNames());
  }

  private static Map<String, Object> stringArray() {
    return arrayOf(scalarString());
  }

  private static Map<String, Object> stringOrStringArray() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(ANY_OF, List.of(scalarString(), stringArray()));
    return schema;
  }

  private static Map<String, Object> tableTypeEnum() {
    return caseInsensitiveEnum(ModelSchemaRules.tableTypeNames());
  }

  private static Map<String, Object> caseInsensitiveEnum(List<String> values) {
    Map<String, Object> canonical = new LinkedHashMap<>();
    canonical.put(ENUM, values);
    Map<String, Object> caseInsensitive = new LinkedHashMap<>();
    caseInsensitive.put(TYPE, T_STRING);
    caseInsensitive.put(PATTERN, caseInsensitivePattern(values));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(ANY_OF, List.of(canonical, caseInsensitive));
    return schema;
  }

  private static String caseInsensitivePattern(List<String> values) {
    StringBuilder pattern = new StringBuilder("^(?:");
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        pattern.append('|');
      }
      for (char character : values.get(index).toCharArray()) {
        pattern
            .append('[')
            .append(Character.toLowerCase(character))
            .append(Character.toUpperCase(character))
            .append(']');
      }
    }
    return pattern.append(")$").toString();
  }

  private static Map<String, Object> arrayOf(Map<String, Object> itemSchema) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(TYPE, T_ARRAY);
    schema.put(ITEMS, itemSchema);
    return schema;
  }

  private static Map<String, Object> mapOf(Map<String, Object> valueSchema) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(TYPE, T_OBJECT);
    schema.put(ADDITIONAL_PROPERTIES, valueSchema);
    return schema;
  }

  private static Map<String, Object> ref(String definitionName) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put(REF, "#/" + DEFS + "/" + definitionName);
    return schema;
  }

  private static void verifyCovers(Set<String> described, Set<String> parsed, String context) {
    if (!described.equals(parsed)) {
      Set<String> missing = new LinkedHashSet<>(parsed);
      missing.removeAll(described);
      Set<String> extra = new LinkedHashSet<>(described);
      extra.removeAll(parsed);
      throw new MolgenisException(
          "JSON Schema rules drifted from the "
              + context
              + " parse surface; missing "
              + missing
              + ", extra "
              + extra);
    }
  }

  private static String serialize(Map<String, Object> tree) {
    DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    printer.indentObjectsWith(indenter);
    printer.indentArraysWith(indenter);
    try {
      return new ObjectMapper().writer(printer).writeValueAsString(tree) + "\n";
    } catch (Exception exception) {
      throw new MolgenisException("Failed to serialize the model JSON Schema", exception);
    }
  }
}
