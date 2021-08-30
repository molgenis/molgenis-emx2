package org.molgenis.emx2.utils;

import static org.jooq.impl.DSL.cast;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
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
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;

public class TypeUtils {
  private static ObjectMapper json = new ObjectMapper();

  private static final String LOOSE_PARSER_FORMAT =
      "[yyyy-MM-dd]['T'[HHmmss][HHmm][HH:mm:ss][HH:mm][.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]][OOOO][O][z][XXXXX][XXXX]['['VV']']";

  protected TypeUtils() {
    // hide public constructor
  }

  public static UUID toUuid(Object v) {
    if (v == null) return null;
    if (v instanceof String) {
      if (((String) v).trim().equals("")) throw new MolgenisException("Cannot cast \"\" to UUID");
      return java.util.UUID.fromString((String) v);
    }
    return (UUID) v;
  }

  public static UUID[] toUuidArray(Object v) {
    return (UUID[]) processArray(v, TypeUtils::toUuid, UUID[]::new, UUID.class);
  }

  public static String[] toStringArray(Object v) {
    return (String[]) processArray(v, TypeUtils::toString, String[]::new, String.class);
  }

  public static String toString(Object v) {
    if (v == null) return null;
    if (v instanceof String) {
      if (((String) v).trim().equals("")) {
        return null;
      }
      return (String) v;
    }
    if (v instanceof Object[]) return joinCsvString((Object[]) v);
    if (v instanceof Collection) return joinCsvString(((Collection) v).toArray());
    return v.toString();
  }

  public static Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof String) {
      if (((String) v).trim().equals("")) return null;
      return Integer.parseInt(((String) v).trim());
    }
    if (v instanceof Long) {
      return ((Long) v).intValue();
    }
    if (v instanceof Double) return (int) Math.round((Double) v);
    return (Integer) v;
  }

  public static Integer[] toIntArray(Object v) {
    return (Integer[]) processArray(v, TypeUtils::toInt, Integer[]::new, Integer.class);
  }

  private static Object[] processArray(
      Object v, UnaryOperator<Object> f, IntFunction<Object[]> m, Class<?> c) {
    if (v == null) return null; // NOSONAR
    else if (v.getClass().isArray() && v.getClass().getComponentType().equals(c.getClass()))
      return (Object[]) v;
    else if (v instanceof String) {
      if (((String) v).trim().equals("")) return null; // NOSONAR
      v = splitCsvString((String) v);
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
      if ("true".equalsIgnoreCase(((String) v).trim())
          || "yes".equalsIgnoreCase(((String) v).trim())) return true;
      if ("false".equalsIgnoreCase(((String) v).trim())
          || "no".equalsIgnoreCase(((String) v).trim())) return false;
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
    if (v instanceof String) return Double.parseDouble((String) v);
    if (v instanceof BigDecimal) return ((BigDecimal) v).doubleValue();
    return (Double) v;
  }

  public static Double[] toDecimalArray(Object v) {
    return (Double[]) processArray(v, TypeUtils::toDecimal, Double[]::new, Double.class);
  }

  public static LocalDate toDate(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDate) return (LocalDate) v;
    if (v instanceof OffsetDateTime) return ((OffsetDateTime) v).toLocalDate();
    return LocalDate.parse(v.toString());
  }

  public static LocalDate[] toDateArray(Object v) {
    return (LocalDate[]) processArray(v, TypeUtils::toDate, LocalDate[]::new, LocalDate.class);
  }

  public static LocalDateTime toDateTime(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDateTime) return (LocalDateTime) v;
    if (v instanceof OffsetDateTime) return ((OffsetDateTime) v).toLocalDateTime();
    if (v instanceof Timestamp) return ((Timestamp) v).toLocalDateTime();
    // add 'T' because loose users of iso8601 (postgres!) use space instead of T
    String str = v.toString().replace(" ", "T");
    TemporalAccessor temporalAccessor =
        DateTimeFormatter.ofPattern(LOOSE_PARSER_FORMAT)
            .parseBest(str, ZonedDateTime::from, LocalDate::from);
    if (temporalAccessor instanceof ZonedDateTime) {
      return ((ZonedDateTime) temporalAccessor).toLocalDateTime();
    }
    return LocalDateTime.parse(str);
  }

  public static LocalDateTime[] toDateTimeArray(Object v) {
    return (LocalDateTime[])
        processArray(v, TypeUtils::toDateTime, LocalDateTime[]::new, LocalDateTime.class);
  }

  public static JSONB toJsonb(Object v) {
    if (v == null) return null;
    if (v instanceof String) return org.jooq.JSONB.valueOf((String) v);
    return (JSONB) v;
  }

  public static JSONB[] toJsonbArray(Object v) {
    // non standard so not using the generic function
    if (v == null) return null; // NOSONAR
    if (v instanceof String) {
      if (((String) v).trim().equals("")) return null; // NOSONAR
      v = List.of(JSONB.valueOf((String) v));
    }
    if (v instanceof String[]) {
      v = List.of((String[]) v);
    }
    if (v instanceof Serializable[]) v = List.of((Serializable[]) v);
    if (v instanceof Object[]) v = List.of((Object[]) v);
    if (v instanceof List) {
      return ((List<Object>) v).stream().map(TypeUtils::toJsonb).toArray(JSONB[]::new);
    }
    return (JSONB[]) v;
  }

  public static String toText(Object v) {
    return toString(v);
  }

  public static String[] toTextArray(Object v) {
    return toStringArray(v);
  }

  public static ColumnType typeOf(Class<?> klazz) {
    for (ColumnType t : ColumnType.values()) {
      if (t.getType().equals(klazz)) return t;
    }
    throw new MolgenisException(
        "Unknown type: Can not determine typeOf(Class). No MOLGENIS type is defined to match "
            + klazz.getCanonicalName());
  }

  public static ColumnType getNonArrayType(ColumnType columnType) {
    switch (columnType.getBaseType()) {
      case UUID_ARRAY:
        return ColumnType.UUID;
      case STRING_ARRAY:
        return ColumnType.STRING;
      case BOOL_ARRAY:
        return ColumnType.BOOL;
      case INT_ARRAY:
        return ColumnType.INT;
      case DECIMAL_ARRAY:
        return ColumnType.DECIMAL;
      case TEXT_ARRAY:
        return ColumnType.TEXT;
      case DATE_ARRAY:
        return ColumnType.DATE;
      case DATETIME_ARRAY:
        return ColumnType.DATETIME;
      default:
        throw new UnsupportedOperationException("Unsupported array columnType found:" + columnType);
    }
  }

  public static ColumnType getArrayType(ColumnType columnType) {
    switch (columnType.getBaseType()) {
      case UUID:
        return ColumnType.UUID_ARRAY;
      case STRING:
        return ColumnType.STRING_ARRAY;
      case BOOL:
        return ColumnType.BOOL_ARRAY;
      case INT:
        return ColumnType.INT_ARRAY;
      case DECIMAL:
        return ColumnType.DECIMAL_ARRAY;
      case TEXT:
        return ColumnType.TEXT_ARRAY;
      case DATE:
        return ColumnType.DATE_ARRAY;
      case DATETIME:
        return ColumnType.DATETIME_ARRAY;
      case JSONB:
        return ColumnType.JSONB_ARRAY;
      default:
        throw new UnsupportedOperationException("Unsupported array columnType found:" + columnType);
    }
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
    switch (type.getBaseType()) {
      case FILE:
        return SQLDataType.BINARY;
      case UUID:
        return SQLDataType.UUID;
      case UUID_ARRAY:
        return SQLDataType.UUID.getArrayDataType();
      case STRING:
        return SQLDataType.VARCHAR(255);
      case STRING_ARRAY:
        return SQLDataType.VARCHAR(255).getArrayDataType();
      case INT:
        return SQLDataType.INTEGER;
      case INT_ARRAY:
        return SQLDataType.INTEGER.getArrayDataType();
      case BOOL:
        return SQLDataType.BOOLEAN;
      case BOOL_ARRAY:
        return SQLDataType.BOOLEAN.getArrayDataType();
      case DECIMAL:
        return SQLDataType.DOUBLE;
      case DECIMAL_ARRAY:
        return SQLDataType.DOUBLE.getArrayDataType();
      case TEXT:
        return SQLDataType.VARCHAR;
      case TEXT_ARRAY:
        return SQLDataType.VARCHAR.getArrayDataType();
      case DATE:
        return SQLDataType.DATE;
      case DATE_ARRAY:
        return SQLDataType.DATE.getArrayDataType();
      case DATETIME:
        return SQLDataType.TIMESTAMP;
      case DATETIME_ARRAY:
        return SQLDataType.TIMESTAMP.getArrayDataType();
      case JSONB:
        return SQLDataType.JSONB;
      case JSONB_ARRAY:
        return SQLDataType.JSONB.getArrayDataType();
      default:
        // should never happen
        throw new IllegalArgumentException("jooqTypeOf(type) : unsupported type '" + type + "'");
    }
  }

  public static Object getTypedValue(Object v, ColumnType columnType) {
    switch (columnType.getBaseType()) {
      case UUID:
        return TypeUtils.toUuid(v);
      case UUID_ARRAY:
        return TypeUtils.toUuidArray(v);
      case STRING:
        return TypeUtils.toString(v);
      case STRING_ARRAY:
        return TypeUtils.toStringArray(v);
      case BOOL:
        return TypeUtils.toBool(v);
      case BOOL_ARRAY:
        return TypeUtils.toBoolArray(v);
      case INT:
        return TypeUtils.toInt(v);
      case INT_ARRAY:
        return TypeUtils.toIntArray(v);
      case DECIMAL:
        return TypeUtils.toDecimal(v);
      case DECIMAL_ARRAY:
        return TypeUtils.toDecimalArray(v);
      case TEXT:
        return cast(TypeUtils.toText(v), SQLDataType.VARCHAR);
      case TEXT_ARRAY:
        return cast(TypeUtils.toTextArray(v), SQLDataType.VARCHAR.getArrayDataType());
      case DATE:
        return TypeUtils.toDate(v);
      case DATE_ARRAY:
        return TypeUtils.toDateArray(v);
      case DATETIME:
        return TypeUtils.toDateTime(v);
      case DATETIME_ARRAY:
        return TypeUtils.toDateTimeArray(v);
      case JSONB:
        return TypeUtils.toJsonb(v);
      case JSONB_ARRAY:
        return TypeUtils.toJsonbArray(v);
      default:
        throw new UnsupportedOperationException(
            "Unsupported columnType columnType found:" + columnType);
    }
  }
}
