package org.molgenis.emx2;

import static java.util.stream.Collectors.toCollection;
import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.utils.TypeUtils.convertToCamelCase;

import com.google.common.net.PercentEscaper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PrimaryKey {
  public static final String NAME_VALUE_SEPARATOR = "=";
  public static final String KEY_PARTS_SEPARATOR = "&";
  private static final PercentEscaper escaper = new PercentEscaper("-._~", false);
  private final SortedMap<String, String> keys;

  public PrimaryKey(SortedMap<String, String> keys) {
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one key.");
    } else if (keys.containsValue(null)) {
      throw new IllegalArgumentException("Values are not allowed to be null.");
    }
    this.keys = keys;
  }

  public PrimaryKey(Map<String, String> keys) {
    this(new TreeMap<>(keys));
  }

  public static PrimaryKey fromRow(TableMetadata table, Row row) {
    final SortedMap<String, String> keyParts = new TreeMap<>();
    for (final Column column : table.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (final Reference reference : column.getReferences()) {
          final String[] values = row.getStringArray(reference.getName());
          if (values != null && values.length > 1) {
            throw new MolgenisException(
                "Primary key column '"
                    + reference.getName()
                    + "' cannot be an array reference. Found "
                    + values.length
                    + " values.");
          }
          String value = values != null && values.length > 0 ? values[0] : null;
          if (value == null) {
            throw new MolgenisException(
                "Primary key column '" + reference.getName() + "' cannot be null.");
          }
          keyParts.put(reference.getName(), value);
        }
      } else {
        String value = row.getString(column.getName());
        if (value == null) {
          throw new MolgenisException(
              "Primary key column '" + column.getName() + "' cannot be null.");
        }
        keyParts.put(column.getName(), value);
      }
    }
    return new PrimaryKey(keyParts);
  }

  public static PrimaryKey fromRow(Table table, Row row) {
    return fromRow(table.getMetadata(), row);
  }

  public static PrimaryKey fromEncodedString(TableMetadata table, String encodedString) {
    String[] encodedPairs = encodedString.split(KEY_PARTS_SEPARATOR);
    if (encodedPairs.length == 0) {
      throw new IllegalArgumentException("There must be at least one key.");
    } else if (encodedPairs.length > 1
        && !Arrays.equals(Arrays.stream(encodedPairs).sorted().toArray(), encodedPairs)) {
      throw new IllegalArgumentException("The encoded String does not contain sorted values");
    } else {
      SortedMap<String, String> pairs = new TreeMap<>();
      for (String pair : encodedPairs) {
        String[] parts = pair.split(NAME_VALUE_SEPARATOR);
        if (parts.length != 2) {
          throw new IllegalArgumentException(
              "Can't decode the key, name value pair is incomplete.");
        }
        String identifier = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        String name =
            recursiveIdentifierToName(
                table,
                Arrays.stream(identifier.split("\\.")).collect(toCollection(ArrayList::new)));
        String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        pairs.put(name, value);
      }
      return new PrimaryKey(pairs);
    }
  }

  private static String recursiveIdentifierToName(TableMetadata table, List<String> remaining) {
    String currentIdentifier = remaining.remove(0);
    Column currentColumn = table.getColumnByIdentifier(currentIdentifier);
    if (currentColumn == null) {
      throw new IllegalArgumentException(
          "Could not find (inherited) column for identifier \""
              + currentIdentifier
              + "\" in table \""
              + table.getTableName()
              + "\"");
    }

    if (!remaining.isEmpty()) {
      return currentColumn.getName()
          + "."
          + recursiveIdentifierToName(currentColumn.getRefTable(), remaining);
    }
    return currentColumn.getName();
  }

  public static PrimaryKey fromEncodedString(Table table, String encodedValue) {
    return fromEncodedString(table.getMetadata(), encodedValue);
  }

  public String getEncodedString() {
    try {
      List<String> encodedPairs = new ArrayList<>();
      for (Map.Entry<String, String> pair : keys.entrySet()) {
        String name = escaper.escape(convertToCamelCase(pair.getKey()));
        String value = escaper.escape(pair.getValue());
        encodedPairs.add(name + NAME_VALUE_SEPARATOR + value);
      }
      return String.join(KEY_PARTS_SEPARATOR, encodedPairs);
    } catch (Exception e) {
      throw new MolgenisException("Error encoding" + e);
    }
  }

  public String getString() {
    List<String> pairs = new ArrayList<>();
    for (Map.Entry<String, String> pair : keys.entrySet()) {
      String name = convertToCamelCase(pair.getKey());
      String value = pair.getValue();
      pairs.add(name + NAME_VALUE_SEPARATOR + value);
    }
    return String.join(KEY_PARTS_SEPARATOR, pairs);
  }

  public Filter getFilter() {
    final List<Filter> filters =
        keys.entrySet().stream().map(param -> f(param.getKey(), EQUALS, param.getValue())).toList();
    return and(filters);
  }

  SortedMap<String, String> getKeys() {
    return keys;
  }
}
