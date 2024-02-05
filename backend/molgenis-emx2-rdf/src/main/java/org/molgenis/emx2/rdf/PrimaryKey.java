package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.molgenis.emx2.Filter;
import org.molgenis.emx2.MolgenisException;

class PrimaryKey {

  public static final String NAME_VALUE_SEPARATOR = "=";
  public static final String KEY_PARTS_SEPARATOR = "&";
  private final List<NameValuePair> keys;

  static PrimaryKey makePrimaryKeyFromEncodedKey(String encodedValue) {
    String[] encodedPairs = encodedValue.split(KEY_PARTS_SEPARATOR);
    if (encodedPairs.length == 0) {
      throw new IllegalArgumentException("There must be at least one key.");
    } else {
      List<NameValuePair> pairs = new ArrayList<>();
      for (var pair : encodedPairs) {
        var parts = pair.split(NAME_VALUE_SEPARATOR);
        if (parts.length != 2) {
          throw new MolgenisException("Can't decode the key, name value pair is incomplete.");
        }
        var name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        var value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        pairs.add(new BasicNameValuePair(name, value));
      }
      return new PrimaryKey(pairs);
    }
  }

  PrimaryKey(List<NameValuePair> keys) {
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one key.");
    }
    this.keys = keys;
  }

  String getEncodedValue() {
    try {
      List<String> encodedPairs = new ArrayList<>();
      // Sort the list to have a stable order
      var sortedList = keys.stream().sorted(new NameValuePairComparator()).toList();
      for (var pair : sortedList) {
        var name = URLEncoder.encode(pair.getName(), StandardCharsets.UTF_8.toString());
        var value = URLEncoder.encode(pair.getValue(), StandardCharsets.UTF_8.toString());
        encodedPairs.add(name + NAME_VALUE_SEPARATOR + value);
      }
      return String.join(KEY_PARTS_SEPARATOR, encodedPairs);
    } catch (Exception e) {
      throw new MolgenisException("Error encoding" + e);
    }
  }

  Filter getFilter() {
    final List<Filter> filters =
        keys.stream().map(param -> f(param.getName(), EQUALS, param.getValue())).toList();
    return and(filters);
  }

  List<NameValuePair> getKeys() {
    return keys;
  }

  static class NameValuePairComparator implements Comparator<NameValuePair> {

    @Override
    public int compare(NameValuePair left, NameValuePair right) {
      return left.getName().compareTo(right.getName());
    }
  }
}
