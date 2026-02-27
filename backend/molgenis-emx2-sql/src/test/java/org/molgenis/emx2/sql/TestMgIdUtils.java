package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

class TestMgIdUtils {

  @Test
  void encodePrefixUsesIdentifier() {
    TableMetadata table = table("MyTable");
    assertEquals("MyTable/", MgIdUtils.encodePrefix(table));
  }

  @Test
  void encodeKeyNameConvertsToCamelCase() {
    assertEquals("my_column", MgIdUtils.encodeKeyName("my_column"));
    assertEquals("id", MgIdUtils.encodeKeyName("id"));
    assertEquals("myColumn", MgIdUtils.encodeKeyName("My Column"));
  }

  @Test
  void encodeProducesCorrectFormat() {
    Map<String, String> keys = new LinkedHashMap<>();
    keys.put("id", "42");
    assertEquals("Person/id=42", MgIdUtils.encode("Person", keys));
  }

  @Test
  void encodeCompositeKeyProducesCorrectFormat() {
    Map<String, String> keys = new LinkedHashMap<>();
    keys.put("orderId", "99");
    keys.put("lineNum", "3");
    assertEquals("Order/orderId=99&lineNum=3", MgIdUtils.encode("Order", keys));
  }

  @Test
  void decodeParsesSingleKey() {
    MgIdUtils.DecodedMgId result = MgIdUtils.decode("Person/id=42");
    assertEquals("Person", result.tableIdentifier());
    assertEquals(Map.of("id", "42"), result.keyValues());
  }

  @Test
  void decodeIsSymmetricWithEncode() {
    Map<String, String> original = new LinkedHashMap<>();
    original.put("orderId", "99");
    original.put("lineNum", "3");
    String encoded = MgIdUtils.encode("Order", original);
    MgIdUtils.DecodedMgId decoded = MgIdUtils.decode(encoded);
    assertEquals("Order", decoded.tableIdentifier());
    assertEquals(original, decoded.keyValues());
  }

  @Test
  void decodeThrowsOnMissingTablePrefix() {
    assertThrows(MolgenisException.class, () -> MgIdUtils.decode("id=42"));
  }

  @Test
  void decodeThrowsOnMalformedPair() {
    assertThrows(MolgenisException.class, () -> MgIdUtils.decode("Person/id"));
  }

  @Test
  void decodeHandlesUrlEncodedValues() {
    MgIdUtils.DecodedMgId result = MgIdUtils.decode("Person/name=John%20Doe");
    assertEquals("John Doe", result.keyValues().get("name"));
  }

  @Test
  void encodeUrlEncodesSpecialCharacters() {
    Map<String, String> keys = new LinkedHashMap<>();
    keys.put("name", "John Doe");
    String encoded = MgIdUtils.encode("Person", keys);
    MgIdUtils.DecodedMgId decoded = MgIdUtils.decode(encoded);
    assertEquals("John Doe", decoded.keyValues().get("name"));
  }

  @Test
  void toColumnValuesMapsKeysToColumnNames() {
    TableMetadata table = table("Person", new Column("id").setPkey());
    MgIdUtils.DecodedMgId decoded = MgIdUtils.decode("Person/id=42");
    Map<String, String> result = MgIdUtils.toColumnValues(table, decoded);
    assertEquals(Map.of("id", "42"), result);
  }

  @Test
  void toColumnValuesMapsCamelCaseKeysToColumnNames() {
    TableMetadata table =
        table("Order", new Column("order_id").setPkey(), new Column("line_num").setPkey());
    MgIdUtils.DecodedMgId decoded = MgIdUtils.decode("Order/order_id=99&line_num=3");
    Map<String, String> result = MgIdUtils.toColumnValues(table, decoded);
    assertEquals(2, result.size());
    assertEquals("99", result.get("order_id"));
    assertEquals("3", result.get("line_num"));
  }

  @Test
  void toColumnValuesThrowsOnTableMismatch() {
    TableMetadata table = table("Person", new Column("id").setPkey());
    MgIdUtils.DecodedMgId decoded = MgIdUtils.decode("Order/id=42");
    assertThrows(MolgenisException.class, () -> MgIdUtils.toColumnValues(table, decoded));
  }

  @Test
  void toColumnValuesThrowsOnUnknownColumn() {
    TableMetadata table = table("Person", new Column("id").setPkey());
    MgIdUtils.DecodedMgId decoded = MgIdUtils.decode("Person/unknown=42");
    assertThrows(MolgenisException.class, () -> MgIdUtils.toColumnValues(table, decoded));
  }
}
