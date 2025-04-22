package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.IriGenerator.escaper;
import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;

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
  private final Map<String, String> keys;

  static PrimaryKey fromRow(TableMetadata table, Row row) {
    final Map<String, String> keyParts = new LinkedHashMap<>();
    for (final Column column : table.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (final Reference reference : column.getReferences()) {
          final String[] values = row.getStringArray(reference.getName());
          for (final String value : values) {
            keyParts.put(reference.getIdentifier(), value);
          }
        }
      } else {
        keyParts.put(column.getIdentifier(), row.get(column).toString());
      }
    }
    return new PrimaryKey(keyParts);
  }

  static PrimaryKey fromRow(Table table, Row row) {
    return fromRow(table.getMetadata(), row);
  }


  // use map instead of list<NameValuePair> to prevent duplicate entries
  // some foreign key have overlapping relationships which resulted in a bug

  static PrimaryKey makePrimaryKeyFromEncodedKey(String encodedValue) {
    String[] encodedPairs = encodedValue.split(KEY_PARTS_SEPARATOR);
    if (encodedPairs.length == 0) {
      throw new IllegalArgumentException("There must be at least one key.");
    } else {
      Map<String, String> pairs = new LinkedHashMap<>();
      for (var pair : encodedPairs) {
        var parts = pair.split(NAME_VALUE_SEPARATOR);
        if (parts.length != 2) {
          throw new IllegalArgumentException(
              "Can't decode the key, name value pair is incomplete.");
        }
        var name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        var value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        pairs.put(name, value);
      }
      return new PrimaryKey(pairs);
    }
  }

  PrimaryKey(Map<String, String> keys) {
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one key.");
    }
    this.keys = keys;
  }

  String getEncodedValue() {
    try {
      List<String> encodedPairs = new ArrayList<>();
      // Sort the list to have a stable order
      var sortedMap = new TreeMap<>(this.keys);
      for (var pair : sortedMap.entrySet()) {
        var name = escaper.escape(pair.getKey());
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

  Map<String, String> getKeys() {
    return keys;
  }
}
