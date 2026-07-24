package org.molgenis.emx2.io.emx2;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;

public final class ModelSchemaRules {

  public enum AttributeType {
    STRING,
    INTEGER,
    BOOLEAN,
    BOOLEAN_OR_EXPRESSION,
    STRING_ARRAY,
    OBJECT,
    COLUMN_TYPE_ENUM
  }

  static final String NAME = "name";
  static final String TYPE = "type";
  static final String REF_TABLE = "refTable";
  static final String REF_LINK = "refLink";
  static final String REF_LABEL = "refLabel";
  static final String REF_BACK = "refBack";
  static final String VALUES = "values";
  static final String SECTION = "section";
  static final String HEADING = "heading";
  static final String COLUMNS = "columns";
  static final String TABLES = "tables";
  static final String IMPORTS = "imports";
  static final String EXTENDS = "extends";
  static final String TABLE_TYPE = "tableType";
  static final String SUBCLASSES = "subclasses";
  static final String MODULES = "modules";
  static final String SETTINGS = "settings";
  static final String NAMESPACES = "namespaces";
  static final String VERSION = "version";
  static final String FORMAT_VERSION = "formatVersion";
  static final String LABEL = "label";
  static final String DESCRIPTION = "description";
  static final String SEMANTICS = "semantics";
  static final String PROFILES = "profiles";
  static final String DROP = "drop";

  static final Set<String> REFERENCE_ONLY_ATTRIBUTES =
      Set.of(REF_TABLE, REF_LINK, REF_LABEL, REF_BACK);
  static final Set<String> VALUE_ONLY_ATTRIBUTES = Set.of(VALUES);

  private static final Map<String, AttributeType> COLUMN_ATTRIBUTES = buildColumnAttributes();

  private ModelSchemaRules() {}

  private static Map<String, AttributeType> buildColumnAttributes() {
    Map<String, AttributeType> attributes = new LinkedHashMap<>();
    attributes.put(NAME, AttributeType.STRING);
    attributes.put(TYPE, AttributeType.COLUMN_TYPE_ENUM);
    attributes.put(REF_TABLE, AttributeType.STRING);
    attributes.put(REF_LINK, AttributeType.STRING);
    attributes.put(REF_LABEL, AttributeType.STRING);
    attributes.put(REF_BACK, AttributeType.STRING);
    attributes.put("key", AttributeType.INTEGER);
    attributes.put("required", AttributeType.BOOLEAN_OR_EXPRESSION);
    attributes.put("readonly", AttributeType.BOOLEAN);
    attributes.put("defaultValue", AttributeType.STRING);
    attributes.put("validation", AttributeType.STRING);
    attributes.put("visible", AttributeType.STRING);
    attributes.put("computed", AttributeType.STRING);
    attributes.put(SEMANTICS, AttributeType.STRING_ARRAY);
    attributes.put(VALUES, AttributeType.STRING_ARRAY);
    attributes.put(PROFILES, AttributeType.STRING_ARRAY);
    attributes.put("previousNames", AttributeType.STRING_ARRAY);
    attributes.put(DESCRIPTION, AttributeType.STRING);
    attributes.put(LABEL, AttributeType.STRING);
    attributes.put("formLabel", AttributeType.STRING);
    attributes.put("subclass", AttributeType.STRING);
    attributes.put("module", AttributeType.STRING);
    attributes.put(DROP, AttributeType.BOOLEAN);
    verifyCovers(attributes.keySet(), Emx2Yaml.COLUMN_KEYS, "column");
    return attributes;
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

  public static Map<String, AttributeType> columnAttributes() {
    return COLUMN_ATTRIBUTES;
  }

  public static Set<String> baseColumnAttributes() {
    Set<String> base = new LinkedHashSet<>(COLUMN_ATTRIBUTES.keySet());
    base.removeAll(REFERENCE_ONLY_ATTRIBUTES);
    base.removeAll(VALUE_ONLY_ATTRIBUTES);
    return base;
  }

  public static AttributeType typeOf(String attribute) {
    return COLUMN_ATTRIBUTES.get(attribute);
  }

  public static List<String> columnTypeNames() {
    return names(
        columnType -> columnType != ColumnType.HEADING && columnType != ColumnType.SECTION);
  }

  public static List<String> tableTypeNames() {
    return List.of("data", "module", "ontology");
  }

  public static List<String> referenceTypeNames() {
    return names(ColumnType::isReference);
  }

  public static List<String> valueTypeNames() {
    return names(ColumnType::isEnum);
  }

  private static List<String> names(java.util.function.Predicate<ColumnType> predicate) {
    return java.util.Arrays.stream(ColumnType.values())
        .filter(predicate)
        .map(columnType -> columnType.name().toLowerCase())
        .sorted()
        .toList();
  }
}
