package org.molgenis.emx2.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jooq.DataType;
import org.jooq.JSONB;
import org.jooq.impl.SQLDataType;
import org.jooq.types.YearToSecond;
import org.molgenis.emx2.*;

public class TypeUtils {
  private static final MolgenisObjectMapper objectMapper = MolgenisObjectMapper.INTERNAL;

  private static final String LOOSE_PARSER_FORMAT =
      "[yyyy-MM-dd]['T'[HHmmss][HHmm][HH:mm:ss][HH:mm][.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]][OOOO][O][z][XXXXX][XXXX]['['VV']']";

  protected TypeUtils() {
    // hide public constructor
  }

  public static UUID toUuid(Object v) {
    if (v == null) return null;
    if (v instanceof String) {
      String value = toString(v);
      if (value != null) {
        return java.util.UUID.fromString(value);
      } else {
        return null;
      }
    }
    return (UUID) v;
  }

  public static UUID[] toUuidArray(Object v) {
    return (UUID[]) processArray(v, TypeUtils::toUuid, UUID[]::new, UUID.class);
  }

  public static String[] toStringArray(Object v) {
    String[] result = toTextArray(v);
    if (result != null) {
      return Arrays.stream(toTextArray(v))
          // we trim string values, but not text values
          .map(s -> s != null ? s.trim() : null)
          .toArray(String[]::new);
    } else {
      return result;
    }
  }

  public static String toString(Object v) {
    String value = toText(v);
    // we trim string values, but not text values
    // empty string is treated as null
    return value != null && !value.trim().equals("") ? value.trim() : null;
  }

  public static Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof String) {
      String value = toString(v);
      if (value != null) {
        return Integer.parseInt(value);
      } else {
        return null;
      }
    }
    if (v instanceof Long longValue) {
      if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE)
        throw new MolgenisException("Cannot cast '" + v + " to integer, it is too large");
      return ((Long) v).intValue();
    }
    if (v instanceof Double) return (int) Math.round((Double) v);
    return (Integer) v;
  }

  public static Long toLong(Object v) {
    if (v == null) return null;
    if (v instanceof String) {
      String value = toString(v);
      if (value != null) {
        return Long.parseLong(value);
      } else {
        return null;
      }
    }
    if (v instanceof Long) return (Long) v;
    if (v instanceof Double) return Math.round((Double) v);
    return Long.parseLong(v.toString());
  }

  public static Integer[] toIntArray(Object v) {
    return (Integer[]) processArray(v, TypeUtils::toInt, Integer[]::new, Integer.class);
  }

  public static Long[] toLongArray(Object v) {
    return (Long[]) processArray(v, TypeUtils::toLong, Long[]::new, Long.class);
  }

  private static Object[] processArray(
      Object v, UnaryOperator<Object> f, IntFunction<Object[]> m, Class<?> c) {
    if (v == null) return null; // NOSONAR
    else if (v.getClass().isArray() && v.getClass().getComponentType().equals(c.getClass()))
      return (Object[]) v;
    else if (v instanceof String) {
      String value = toString(v);
      if (value != null) {
        v = splitCsvString(value);
      } else {
        return null;
      }
    } else if (v.getClass().isArray()) v = Arrays.asList((Object[]) v);
    if (v instanceof List) {
      return ((List<Object>) v).stream().map(f).toArray(m);
    }
    Object result = Array.newInstance(c, 1);
    Array.set(result, 0, f.apply(v));
    return (Object[]) result;
  }

  public static byte[] toBinary(Object v) {
    if (v == null) return null; // NOSONAR
    return (byte[]) v;
  }

  public static Boolean toBool(Object v) {
    if (v == null) return null; // NOSONAR
    if (v instanceof String) {
      String value = toString(v);
      if (value == null) {
        return null; // NOSONAR
      }
      if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) {
        return true;
      }
      if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)) {
        return false;
      }
    }
    try {
      return (Boolean) v;
    } catch (Exception e) {
      throw new MolgenisException("Cannot cast value '" + v + "' to boolean");
    }
  }

  public static Boolean[] toBoolArray(Object v) {
    return (Boolean[]) processArray(v, TypeUtils::toBool, Boolean[]::new, Boolean.class);
  }

  public static Double toDecimal(Object v) {
    if (v == null) return null;
    if (v instanceof String string) {
      if ("".equals(string)) {
        return null;
      } else {
        return Double.parseDouble(string);
      }
    }
    if (v instanceof BigDecimal bigDecimal) return bigDecimal.doubleValue();
    if (v instanceof Integer integer) return Double.valueOf(integer);
    return (Double) v;
  }

  public static Double[] toDecimalArray(Object v) {
    return (Double[]) processArray(v, TypeUtils::toDecimal, Double[]::new, Double.class);
  }

  public static LocalDate toDate(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDate) return (LocalDate) v;
    if (v instanceof OffsetDateTime) return ((OffsetDateTime) v).toLocalDate();
    // otherwise try to use string value
    String value = toString(v);
    if (value != null) {
      LocalDateTime ldt = toDateTime(v);
      if (ldt != null) return ldt.toLocalDate();
    }
    return null;
  }

  public static LocalDate[] toDateArray(Object v) {
    return (LocalDate[]) processArray(v, TypeUtils::toDate, LocalDate[]::new, LocalDate.class);
  }

  public static YearToSecond toYearToSecond(Object v) {
    if (v == null) return null;
    if (v instanceof YearToSecond yearToSecond) return yearToSecond;
    if (v instanceof Period period) return YearToSecond.valueOf(period);
    return YearToSecond.valueOf(toPeriod(v));
  }

  public static YearToSecond[] toYearToSecondArray(Object v) {
    return (YearToSecond[])
        processArray(v, TypeUtils::toYearToSecond, YearToSecond[]::new, YearToSecond.class);
  }

  public static Period toPeriod(Object v) {
    if (v == null) return null;
    if (v instanceof Period) return (Period) v;
    String value = toString(v);
    if (value != null) {
      return Period.parse(value);
    }
    return null;
  }

  public static Period[] toPeriodArray(Object v) {
    return (Period[]) processArray(v, TypeUtils::toPeriod, Period[]::new, Period.class);
  }

  public static LocalDateTime toDateTime(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDateTime) return (LocalDateTime) v;
    if (v instanceof OffsetDateTime) return ((OffsetDateTime) v).toLocalDateTime();
    if (v instanceof Timestamp) return ((Timestamp) v).toLocalDateTime();
    // try string
    String value = toString(v);
    if (value != null) {
      // add 'T' because loose users of iso8601 (postgres!) use space instead of T
      value = value.replace(" ", "T");
      TemporalAccessor temporalAccessor =
          DateTimeFormatter.ofPattern(LOOSE_PARSER_FORMAT)
              .parseBest(value, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
      if (temporalAccessor instanceof ZonedDateTime) {
        return ((ZonedDateTime) temporalAccessor).toLocalDateTime();
      } else if (temporalAccessor instanceof LocalDateTime) {
        return (LocalDateTime) temporalAccessor;
      } else if (temporalAccessor instanceof LocalDate) {
        return ((LocalDate) temporalAccessor).atStartOfDay();
      }
      return LocalDateTime.parse(value);
    } else {
      return null;
    }
  }

  public static LocalDateTime[] toDateTimeArray(Object v) {
    return (LocalDateTime[])
        processArray(v, TypeUtils::toDateTime, LocalDateTime[]::new, LocalDateTime.class);
  }

  public static JSONB toJsonb(Object v) {
    if (v == null) return null;
    if (v instanceof JSONB) { // Ensures JSONB is validated
      v = v.toString();
    }
    if (v instanceof String) {
      String value = toString(v);
      if (value != null) {
        try {
          v = objectMapper.getReader().readTree(value);
        } catch (Exception e) {
          throw new MolgenisException("Invalid json", e);
        }
      } else {
        return null;
      }
    }
    if (v instanceof JsonNode) {
      return org.jooq.JSONB.valueOf(objectMapper.validate((JsonNode) v).toString());
    }

    // Other input is invalid (no casting due to ensuring validateJson() is executed).
    throw new ClassCastException("Cannot cast '" + v.toString() + "' to JSONB");
  }

  public static String toText(Object v) {
    if (v == null) return null;
    if (v instanceof String) {
      if (((String) v).trim().equals("")) {
        return null;
      }
      // we trim string values, but not text values
      return (String) v;
    }
    if (v instanceof Object[]) return joinCsvString((Object[]) v);
    if (v instanceof Collection) return joinCsvString(((Collection) v).toArray());
    return v.toString();
  }

  public static String[] toTextArray(Object v) {
    return (String[]) processArray(v, TypeUtils::toString, String[]::new, String.class);
  }

  public static ColumnType typeOf(Class<?> klazz) {
    for (ColumnType t : ColumnType.values()) {
      if (t.getType().equals(klazz)) return t;
    }
    throw new MolgenisException(
        "Unknown type: Can not determine typeOf(Class). No MOLGENIS type is defined to match "
            + klazz.getCanonicalName());
  }

  public static ColumnType getArrayType(ColumnType columnType) {
    return switch (columnType.getBaseType()) {
      case UUID -> ColumnType.UUID_ARRAY;
      case STRING -> ColumnType.STRING_ARRAY;
      case BOOL -> ColumnType.BOOL_ARRAY;
      case INT -> ColumnType.INT_ARRAY;
      case DECIMAL -> ColumnType.DECIMAL_ARRAY;
      case TEXT -> ColumnType.TEXT_ARRAY;
      case DATE -> ColumnType.DATE_ARRAY;
      case DATETIME -> ColumnType.DATETIME_ARRAY;
      case PERIOD -> ColumnType.PERIOD_ARRAY;
      default ->
          throw new UnsupportedOperationException(
              "Unsupported array columnType found:" + columnType);
    };
  }

  private static String joinCsvString(Object[] v) {
    return Stream.of(v)
        .map(
            s -> {
              if (s == null) return "";
              String str = s.toString();
              if (str.contains(",")) return "\"" + s.toString() + "\"";
              else return str;
            })
        .collect(Collectors.joining(","));
  }

  private static List<String> splitCsvString(String value) {
    // thanks stackoverflow
    ArrayList<String> result = new ArrayList<>();
    boolean notInsideComma = true;
    int start = 0;
    for (int i = 0; i < value.length() - 1; i++) {
      if (value.charAt(i) == ',' && notInsideComma) {
        String v = trimQuotes(value.substring(start, i));
        if (!"".equals(v)) result.add(v != null ? v.trim() : null);
        start = i + 1;
      } else if (value.charAt(i) == '"') notInsideComma = !notInsideComma;
    }
    String v = trimQuotes(value.substring(start));
    if (v != null && !"".equals(v)) result.add(v.trim());
    return result;
  }

  private static String trimQuotes(String value) {
    if (value == null || !value.contains("\"")) return value;
    value = value.trim();
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }
    value = value.trim();
    return value;
  }

  public static DataType toJooqType(ColumnType type) {
    return switch (type.getBaseType()) {
      case FILE -> SQLDataType.BINARY;
      case UUID -> SQLDataType.UUID;
      case UUID_ARRAY -> SQLDataType.UUID.getArrayDataType();
      case STRING, EMAIL, HYPERLINK -> SQLDataType.VARCHAR(255);
      case STRING_ARRAY, EMAIL_ARRAY, HYPERLINK_ARRAY ->
          SQLDataType.VARCHAR(255).getArrayDataType();
      case INT -> SQLDataType.INTEGER;
      case INT_ARRAY -> SQLDataType.INTEGER.getArrayDataType();
      case LONG -> SQLDataType.BIGINT;
      case LONG_ARRAY -> SQLDataType.BIGINT.getArrayDataType();
      case BOOL -> SQLDataType.BOOLEAN;
      case BOOL_ARRAY -> SQLDataType.BOOLEAN.getArrayDataType();
      case DECIMAL -> SQLDataType.DOUBLE;
      case DECIMAL_ARRAY -> SQLDataType.DOUBLE.getArrayDataType();
      case TEXT -> SQLDataType.VARCHAR;
      case TEXT_ARRAY -> SQLDataType.VARCHAR.getArrayDataType();
      case DATE -> SQLDataType.DATE;
      case DATE_ARRAY -> SQLDataType.DATE.getArrayDataType();
      case DATETIME -> SQLDataType.TIMESTAMP;
      case DATETIME_ARRAY -> SQLDataType.TIMESTAMP.getArrayDataType();
      case PERIOD -> SQLDataType.INTERVAL.asConvertedDataType(new PeriodConverter());
      case PERIOD_ARRAY ->
          SQLDataType.INTERVAL.asConvertedDataType(new PeriodConverter()).getArrayDataType();
      case JSON -> SQLDataType.JSONB;
      default ->
          // should never happen
          throw new IllegalArgumentException("jooqTypeOf(type) : unsupported type '" + type + "'");
    };
  }

  public static Object getTypedValue(Object v, ColumnType columnType) {
    return switch (columnType.getBaseType()) {
      case UUID -> TypeUtils.toUuid(v);
      case UUID_ARRAY -> TypeUtils.toUuidArray(v);
      case STRING, EMAIL, HYPERLINK, FILE -> TypeUtils.toString(v);
      case STRING_ARRAY, EMAIL_ARRAY, HYPERLINK_ARRAY -> TypeUtils.toStringArray(v);
      case BOOL -> TypeUtils.toBool(v);
      case BOOL_ARRAY -> TypeUtils.toBoolArray(v);
      case INT -> TypeUtils.toInt(v);
      case INT_ARRAY -> TypeUtils.toIntArray(v);
      case LONG -> TypeUtils.toLong(v);
      case LONG_ARRAY -> TypeUtils.toLongArray(v);
      case DECIMAL -> TypeUtils.toDecimal(v);
      case DECIMAL_ARRAY -> TypeUtils.toDecimalArray(v);
      case TEXT -> TypeUtils.toText(v);
      case TEXT_ARRAY -> TypeUtils.toTextArray(v);
      case DATE -> TypeUtils.toDate(v);
      case DATE_ARRAY -> TypeUtils.toDateArray(v);
      case DATETIME -> TypeUtils.toDateTime(v);
      case DATETIME_ARRAY -> TypeUtils.toDateTimeArray(v);
      case PERIOD -> TypeUtils.toPeriod(v);
      case PERIOD_ARRAY -> TypeUtils.toPeriodArray(v);
      case JSON -> TypeUtils.toJsonb(v);

      default ->
          throw new UnsupportedOperationException(
              "Unsupported columnType columnType found:" + columnType);
    };
  }

  public static String convertToCamelCase(String value) {
    // main purpose is to remove spaces because not allowed in identifiers
    if (value != null) {
      String[] words = value.split("\\s+");
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < words.length; i++) {
        if (i == 0) {
          // first character is lowercase, we don't touch other characters
          result.append(words[i].substring(0, 1).toLowerCase());
        } else {
          // all other words have first letter character case
          result.append(words[i].substring(0, 1).toUpperCase());
        }
        if (words[i].length() > 1) {
          result.append(words[i].substring(1));
        }
      }
      return result.toString().trim();
    } else {
      return null;
    }
  }

  public static String convertToPascalCase(String value) {
    // main purpose is to remove spaces because not allowed in identifiers
    if (value != null) {
      String[] words = value.split("\\s+");
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < words.length; i++) {
        // all other words have first character uppercase, other characters not touched
        result.append(words[i].substring(0, 1).toUpperCase());
        if (words[i].length() > 1) {
          result.append(words[i].substring(1));
        }
      }
      return result.toString().trim();
    } else {
      return null;
    }
  }

  public static boolean isNull(Object value, ColumnType type) {
    Object typedValue = getTypedValue(value, type);
    if (type.isArray()) {
      return typedValue == null || ((Object[]) typedValue).length == 0;
    }
    return typedValue == null;
  }

  public static LocalDateTime millisecondsToLocalDateTime(long milliseconds) {
    if (milliseconds > 0) {
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
    } else {
      return null;
    }
  }

  public static List<Row> convertToRows(TableMetadata metadata, List<Map<String, Object>> map) {
    return convertToRows(metadata, map, false);
  }

  public static List<Row> convertToPrimaryKeyRows(
      TableMetadata metadata, List<Map<String, Object>> map) {
    return convertToRows(metadata, map, true);
  }

  private static List<Row> convertToRows(
      TableMetadata metadata, List<Map<String, Object>> map, boolean primaryKeyOnly) {
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> field : map) {
      Row row = new Row();
      List<Column> columns =
          primaryKeyOnly ? metadata.getPrimaryKeyColumns() : metadata.getColumns();
      for (Column column : metadata.getColumns()) {
        if (field.containsKey(column.getIdentifier())) {
          Object fieldValue = field.get(column.getIdentifier());
          addFieldObjectToRow(column, fieldValue, row);
        }
      }
      rows.add(row);
    }
    return rows;
  }

  public static void addFieldObjectToRow(Column column, Object object, Row row) {
    if (column.isRef()) {
      convertRefToRow((Map<String, Object>) object, row, column);
    } else if (column.isReference()) {
      // REFBACK, REF_ARRAY
      convertRefArrayToRow((List<Map<String, Object>>) object, row, column);
    } else if (column.isFile()) {
      BinaryFileWrapper bfw = (BinaryFileWrapper) object;
      if (bfw == null || !bfw.isSkip()) {
        // also necessary in case of 'null' to ensure all file metadata fields are made empty
        // skip is used when use submitted only metadata (that they received in query)
        row.setBinary(column.getName(), (BinaryFileWrapper) object);
      }
    } else {
      row.set(column.getName(), object);
    }
  }

  protected static void convertRefArrayToRow(
      List<Map<String, Object>> list, Row row, Column column) {

    List<Reference> refs = column.getReferences();
    for (Reference ref : refs) {
      if (!ref.isOverlapping()) {
        if (!list.isEmpty()) {
          row.set(ref.getName(), getRefValueFromList(ref.getPath(), list));
        } else {
          row.set(ref.getName(), new ArrayList<>());
        }
      }
    }
  }

  private static List<Object> getRefValueFromList(
      List<String> path, List<Map<String, Object>> list) {
    List<Object> result = new ArrayList<>();
    for (Map<String, Object> map : list) {
      Object value = getRefValueFromMap(path, map);
      if (value != null) {
        result.add(value);
      }
    }
    return result;
  }

  private static Object getRefValueFromMap(List<String> path, Map<String, Object> map) {
    if (path.size() == 1) {
      return map.get(path.get(0));
    } else {
      // should be > 1 and value should be of type map
      Object value = map.get(path.get(0));
      if (value != null) {
        return getRefValueFromMap(path.subList(1, path.size()), (Map<String, Object>) value);
      }
      return null;
    }
  }

  protected static void convertRefToRow(Map<String, Object> map, Row row, Column column) {
    for (Reference ref : column.getReferences()) {
      if (!ref.isOverlapping()) {
        String name = ref.getName();
        if (map == null) {
          row.set(name, null);
        } else {
          row.set(ref.getName(), getRefValueFromMap(ref.getPath(), map));
        }
      }
    }
  }
}
