package org.molgenis.utils;

import org.molgenis.MolgenisException;
import org.molgenis.metadata.Type;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypeUtils {

  protected TypeUtils() {
    // hide public constructor
  }

  public static UUID toUuid(Object v) {
    if (v == null) return null;
    if (v instanceof String) return UUID.fromString((String) v);
    return (UUID) v;
  }

  public static UUID[] toUuidArray(Object v) {
    if (v == null) return new UUID[0];
    if (v instanceof UUID[]) return (UUID[]) v;
    if (v instanceof Object[]) {
      return Stream.of((Object[]) v).map(TypeUtils::toUuid).toArray(UUID[]::new);
    }
    return new UUID[] {toUuid(v)};
  }

  public static String[] toStringArray(Object v) {
    if (v == null) return new String[0];
    if (v instanceof String) v = splitCsvString((String) v);
    if (v instanceof String[]) return (String[]) v;
    else if (v instanceof Object[])
      return Stream.of((Object[]) v).map(TypeUtils::toString).toArray(String[]::new);
    return new String[] {v.toString()};
  }

  public static String toString(Object v) {
    if (v == null) return null;
    if (v instanceof String) return (String) v;
    if (v instanceof Object[]) return joinCsvString((Object[]) v);
    if (v instanceof Collection) return joinCsvString(((Collection) v).toArray());
    return v.toString();
  }

  public static Integer toInt(Object v) {
    if (v instanceof String) return Integer.parseInt((String) v);
    return (Integer) v;
  }

  public static Integer[] toIntArray(Object v) {
    if (v == null) return new Integer[0];
    if (v instanceof Integer[]) return (Integer[]) v;
    if (v instanceof String) v = splitCsvString((String) v);
    if (v instanceof Object[])
      return Stream.of((Object[]) v).map(TypeUtils::toInt).toArray(Integer[]::new);
    return new Integer[] {toInt(v)};
  }

  public static Boolean toBool(Object v) {
    if (v instanceof String) {
      if ("true".equalsIgnoreCase((String) v)) return true;
      if ("false".equalsIgnoreCase((String) v)) return false;
    }
    return (Boolean) v;
  }

  public static Boolean[] toBoolArray(Object v) {
    if (v == null) return new Boolean[0];
    if (v instanceof Boolean[]) return (Boolean[]) v;
    if (v instanceof String) v = splitCsvString((String) v);
    if (v instanceof Object[])
      return Stream.of((Object[]) v).map(TypeUtils::toBool).toArray(Boolean[]::new);
    return new Boolean[] {toBool(v)};
  }

  public static Double toDecimal(Object v) {
    if (v instanceof String) return Double.parseDouble((String) v);
    return (Double) v;
  }

  public static Double[] toDecimalArray(Object v) {
    if (v == null) return new Double[0];
    if (v instanceof Double[]) return (Double[]) v;
    if (v instanceof String) v = splitCsvString((String) v);
    if (v instanceof Object[])
      return Stream.of((Object[]) v).map(TypeUtils::toDecimal).toArray(Double[]::new);
    return new Double[] {toDecimal(v)};
  }

  public static LocalDate toDate(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDate) return (LocalDate) v;
    if (v instanceof OffsetDateTime) return ((OffsetDateTime) v).toLocalDate();
    return LocalDate.parse(v.toString());
  }

  public static LocalDate[] toDateArrray(Object v) {

    if (v == null) return new LocalDate[0];
    if (v instanceof LocalDate[]) return (LocalDate[]) v;
    if (v instanceof String) v = splitCsvString((String) v);
    if (v instanceof Object[])
      return Stream.of((Object[]) v).map(TypeUtils::toDate).toArray(LocalDate[]::new);
    return new LocalDate[] {toDate(v)};
  }

  public static LocalDateTime toDateTime(Object v) {
    if (v == null) return null;
    if (v instanceof LocalDateTime) return (LocalDateTime) v;
    if (v instanceof OffsetDateTime) return ((OffsetDateTime) v).toLocalDateTime();
    if (v instanceof Timestamp) return ((Timestamp) v).toLocalDateTime();
    return LocalDateTime.parse(v.toString());
  }

  public static LocalDateTime[] toDateTimeArray(Object v) {
    if (v == null) return new LocalDateTime[0];
    if (v instanceof LocalDateTime[]) return (LocalDateTime[]) v;
    if (v instanceof String) v = splitCsvString((String) v);
    if (v instanceof Object[]) {
      return Stream.of((Object[]) v).map(TypeUtils::toDateTime).toArray(LocalDateTime[]::new);
    }
    return new LocalDateTime[] {toDateTime(v)};
  }

  public static String toText(Object v) {
    return toString(v);
  }

  public static String[] toTextArray(Object v) {
    return toStringArray(v);
  }

  public static Type typeOf(Class klazz) throws MolgenisException {
    for (Type t : Type.values()) {
      if (t.getType().equals(klazz)) return t;
    }
    throw new MolgenisException(
        "invalid_type",
        "Can not determine typeOf(Class)",
        "No MOLGENIS type is defined to match " + klazz.getCanonicalName());
  }

  public static Type getArrayType(Type type) {
    switch (type) {
      case UUID:
        return Type.UUID_ARRAY;
      case STRING:
        return Type.STRING_ARRAY;
      case BOOL:
        return Type.BOOL_ARRAY;
      case INT:
        return Type.INT_ARRAY;
      case DECIMAL:
        return Type.DECIMAL_ARRAY;
      case TEXT:
        return Type.TEXT_ARRAY;
      case DATE:
        return Type.DATE_ARRAY;
      case DATETIME:
        return Type.DATETIME_ARRAY;
      default:
        throw new UnsupportedOperationException("Unsupported REF_ARRAY type found:" + type);
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

  private static String[] splitCsvString(String value) {
    // thanks stackoverflow
    ArrayList<String> result = new ArrayList<>();
    boolean notInsideComma = true;
    int start = 0;
    for (int i = 0; i < value.length() - 1; i++) {
      if (value.charAt(i) == ',' && notInsideComma) {
        result.add(trimQuotes(value.substring(start, i)));
        start = i + 1;
      } else if (value.charAt(i) == '"') notInsideComma = !notInsideComma;
    }
    result.add(trimQuotes(value.substring(start)));
    return result.toArray(new String[result.size()]);
  }

  private static String trimQuotes(String value) {
    if (value == null || !value.contains("\"")) return value;
    value = value.trim();
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }
}
