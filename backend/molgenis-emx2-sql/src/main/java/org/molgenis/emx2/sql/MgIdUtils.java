package org.molgenis.emx2.sql;

import static org.molgenis.emx2.utils.TypeUtils.convertToCamelCase;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jooq.Field;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

class MgIdUtils {

  record DecodedMgId(String tableIdentifier, Map<String, String> keyValues) {}

  static String encode(String tableIdentifier, Map<String, String> keyValues) {
    StringBuilder sb = new StringBuilder(tableIdentifier).append("/");
    String separator = "";
    for (Map.Entry<String, String> entry : keyValues.entrySet()) {
      sb.append(separator)
          .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
          .append("=")
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
      separator = "&";
    }
    return sb.toString();
  }

  static DecodedMgId decode(String mgIdValue) {
    int slashIndex = mgIdValue.indexOf("/");
    if (slashIndex < 0) {
      throw new MolgenisException("mg_id missing table prefix: '" + mgIdValue + "'");
    }
    String tableIdentifier = mgIdValue.substring(0, slashIndex);
    String keyPart = mgIdValue.substring(slashIndex + 1);
    String[] pairs = keyPart.split("&");
    Map<String, String> keyValues = new LinkedHashMap<>();
    for (String pair : pairs) {
      String[] parts = pair.split("=", 2);
      if (parts.length != 2) {
        throw new MolgenisException("mg_id filter: malformed pair '" + pair + "'");
      }
      keyValues.put(
          URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
          URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
    }
    return new DecodedMgId(tableIdentifier, keyValues);
  }

  static Map<String, String> toColumnValues(TableMetadata table, DecodedMgId decodedMgId) {
    if (!table.getIdentifier().equals(decodedMgId.tableIdentifier())) {
      throw new MolgenisException(
          "mg_id table mismatch: expected '"
              + table.getIdentifier()
              + "' but got '"
              + decodedMgId.tableIdentifier()
              + "'");
    }
    Map<String, String> pkIdentifierToName = new LinkedHashMap<>();
    for (Field<?> pkField : table.getPrimaryKeyFields()) {
      pkIdentifierToName.put(convertToCamelCase(pkField.getName()), pkField.getName());
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : decodedMgId.keyValues().entrySet()) {
      String columnName = pkIdentifierToName.get(entry.getKey());
      if (columnName == null) {
        throw new MolgenisException(
            "mg_id filter: unknown primary key column '" + entry.getKey() + "'");
      }
      result.put(columnName, entry.getValue());
    }
    return result;
  }

  static String encodeKeyName(String columnName) {
    return convertToCamelCase(columnName);
  }

  static String encodePrefix(TableMetadata table) {
    return table.getIdentifier() + "/";
  }
}
