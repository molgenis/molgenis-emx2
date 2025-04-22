package org.molgenis.emx2.rdf;

import static java.util.stream.Collectors.toCollection;
import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.IriGenerator.escaper;
import static org.molgenis.emx2.utils.TypeUtils.convertToCamelCase;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Filter;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

/**
 * Does not support "{@code _ARRAY}" {@link org.molgenis.emx2.ColumnType}'s
 *
 * @see <a
 *     href=https://github.com/molgenis/molgenis-emx2/issues/4944>https://github.com/molgenis/molgenis-emx2/issues/4944</a>
 */
class PrimaryKey {
  public static final String NAME_VALUE_SEPARATOR = "=";
  public static final String KEY_PARTS_SEPARATOR = "&";
  // "column name", "column value" (non-escaped due to getFilter() functionality)
  private final SortedMap<String, String> keys;

  /**
   * A table should always have a key=1 column (and it must have a value), so validating on empty
   * values should not be needed.
   */
  static PrimaryKey fromRow(TableMetadata table, Row row) {
    final SortedMap<String, String> keyParts = new TreeMap<>();
    for (final Column column : table.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (final Reference reference : column.getReferences()) {
          final String[] values = row.getStringArray(reference.getName());
          for (final String value : values) {
            keyParts.put(reference.getName(), value);
          }
        }
      } else {
        keyParts.put(column.getName(), row.getString(column.getName()));
      }
    }
    return new PrimaryKey(keyParts);
  }

  static PrimaryKey fromRow(Table table, Row row) {
    return fromRow(table.getMetadata(), row);
  }

  /**
   * Uses map instead of list<NameValuePair> to prevent duplicate entries as some foreign key have
   * overlapping relationships which resulted in a bug.
   *
   * @throws IllegalArgumentException if encodedString contains 0 pairs, encodedString is unsorted
   */
  static PrimaryKey fromEncodedString(TableMetadata table, String encodedString) {
    String[] encodedPairs = encodedString.split(KEY_PARTS_SEPARATOR);
    if (encodedPairs.length == 0) {
      throw new IllegalArgumentException("There must be at least one key.");
    } else if (encodedPairs.length > 1
        && !Arrays.equals(Arrays.stream(encodedPairs).sorted().toArray(), encodedPairs)) {
      throw new IllegalArgumentException("The encoded String does not contain sorted values");
    } else {
      SortedMap<String, String> pairs = new TreeMap<>();
      for (var pair : encodedPairs) {
        var parts = pair.split(NAME_VALUE_SEPARATOR);
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
      throw new MolgenisException(
          "Could not find column for identifier \""
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

  static PrimaryKey fromEncodedString(Table table, String encodedValue) {
    return fromEncodedString(table.getMetadata(), encodedValue);
  }

  PrimaryKey(SortedMap<String, String> keys) {
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one key.");
    }
    this.keys = keys;
  }

  PrimaryKey(Map<String, String> keys) {
    this(new TreeMap<>(keys));
  }

  String getEncodedString() {
    try {
      List<String> encodedPairs = new ArrayList<>();
      for (var pair : keys.entrySet()) {
        var name = escaper.escape(convertToCamelCase(pair.getKey()));
        var value = escaper.escape(pair.getValue());
        encodedPairs.add(name + NAME_VALUE_SEPARATOR + value);
      }
      return String.join(KEY_PARTS_SEPARATOR, encodedPairs);
    } catch (Exception e) {
      throw new MolgenisException("Error encoding" + e);
    }
  }

  Filter getFilter() {
    final List<Filter> filters =
        keys.entrySet().stream().map(param -> f(param.getKey(), EQUALS, param.getValue())).toList();
    return and(filters);
  }

  SortedMap<String, String> getKeys() {
    return keys;
  }
}
