package org.molgenis.emx2.fairmapper.dcat;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.molgenis.emx2.Row;

class DcatHarvestTaskUnitTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private DcatHarvestTask task;

  @BeforeEach
  void setUp() {
    task = new DcatHarvestTask(null, "test-label", "@prefix dcat: <http://www.w3.org/ns/dcat#>.");
  }

  // --- buildReverseContext ---

  @Test
  void buildReverseContextReturnsEmptyMapForNullContext() {
    Map<String, String> result = task.buildReverseContext(null);
    assertTrue(result.isEmpty());
  }

  @Test
  void buildReverseContextReturnsEmptyMapForTextNode() {
    Map<String, String> result = task.buildReverseContext(MAPPER.getNodeFactory().textNode("x"));
    assertTrue(result.isEmpty());
  }

  @Test
  void buildReverseContextMapsPredicateIriToColumnName() {
    ObjectNode context = MAPPER.createObjectNode();
    ObjectNode entry = MAPPER.createObjectNode();
    entry.put("@id", "dcterms:title");
    context.set("title", entry);

    Map<String, String> result = task.buildReverseContext(context);

    assertEquals("title", result.get("dcterms:title"));
  }

  @Test
  void buildReverseContextExcludesReferenceEntries() {
    ObjectNode context = MAPPER.createObjectNode();
    ObjectNode refEntry = MAPPER.createObjectNode();
    refEntry.put("@id", "dcat:dataset");
    refEntry.put("@type", "@id");
    context.set("datasets", refEntry);

    Map<String, String> result = task.buildReverseContext(context);

    assertFalse(result.containsKey("dcat:dataset"));
  }

  @Test
  void buildReverseContextWithAllowedColumnsFiltersOthers() {
    ObjectNode context = MAPPER.createObjectNode();
    ObjectNode titleEntry = MAPPER.createObjectNode();
    titleEntry.put("@id", "dcterms:title");
    context.set("title", titleEntry);

    ObjectNode descEntry = MAPPER.createObjectNode();
    descEntry.put("@id", "dcterms:description");
    context.set("description", descEntry);

    Map<String, String> result = task.buildReverseContext(context, Set.of("title"));

    assertEquals("title", result.get("dcterms:title"));
    assertFalse(result.containsKey("dcterms:description"));
  }

  @Test
  void buildReverseContextWithNullAllowedColumnsIncludesAll() {
    ObjectNode context = MAPPER.createObjectNode();
    ObjectNode titleEntry = MAPPER.createObjectNode();
    titleEntry.put("@id", "dcterms:title");
    context.set("title", titleEntry);

    Map<String, String> result = task.buildReverseContext(context, null);

    assertEquals("title", result.get("dcterms:title"));
  }

  // --- isReferenceEntry ---

  @Test
  void isReferenceEntryReturnsTrueWhenTypeIsAtId() {
    ObjectNode entry = MAPPER.createObjectNode();
    entry.put("@id", "dcat:dataset");
    entry.put("@type", "@id");

    assertTrue(task.isReferenceEntry(entry));
  }

  @Test
  void isReferenceEntryReturnsFalseWhenTypeIsMissing() {
    ObjectNode entry = MAPPER.createObjectNode();
    entry.put("@id", "dcterms:title");

    assertFalse(task.isReferenceEntry(entry));
  }

  @Test
  void isReferenceEntryReturnsFalseWhenTypeIsOtherValue() {
    ObjectNode entry = MAPPER.createObjectNode();
    entry.put("@id", "dcterms:title");
    entry.put("@type", "xsd:string");

    assertFalse(task.isReferenceEntry(entry));
  }

  // --- extractGraphItems ---

  @Test
  void extractGraphItemsReturnsItemsFromGraphArray() {
    ObjectNode framedJson = MAPPER.createObjectNode();
    ArrayNode graph = MAPPER.createArrayNode();
    graph.add(MAPPER.createObjectNode().put("@type", "dcat:Catalog"));
    graph.add(MAPPER.createObjectNode().put("@type", "dcat:Dataset"));
    framedJson.set("@graph", graph);

    assertEquals(2, task.extractGraphItems(framedJson).size());
  }

  @Test
  void extractGraphItemsReturnsSingleItemWhenNoGraph() {
    ObjectNode framedJson = MAPPER.createObjectNode();
    framedJson.put("@type", "dcat:Catalog");

    List<?> items = task.extractGraphItems(framedJson);

    assertEquals(1, items.size());
    assertEquals(framedJson, items.get(0));
  }

  @Test
  void extractGraphItemsReturnsEmptyWhenNeitherGraphNorType() {
    ObjectNode framedJson = MAPPER.createObjectNode();
    framedJson.put("someKey", "someValue");

    List<?> items = task.extractGraphItems(framedJson);

    assertTrue(items.isEmpty());
  }

  // --- extractType ---

  @Test
  void extractTypeReturnsNullWhenTypeFieldMissing() {
    ObjectNode item = MAPPER.createObjectNode();
    assertNull(task.extractType(item));
  }

  @Test
  void extractTypeReturnsTextWhenTypeIsTextual() {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("@type", "dcat:Catalog");

    assertEquals("dcat:Catalog", task.extractType(item));
  }

  @Test
  void extractTypeReturnsFirstElementWhenTypeIsArray() {
    ObjectNode item = MAPPER.createObjectNode();
    ArrayNode typeArray = MAPPER.createArrayNode();
    typeArray.add("dcat:Dataset");
    typeArray.add("dcat:Resource");
    item.set("@type", typeArray);

    assertEquals("dcat:Dataset", task.extractType(item));
  }

  @Test
  void extractTypeReturnsNullForEmptyArray() {
    ObjectNode item = MAPPER.createObjectNode();
    item.set("@type", MAPPER.createArrayNode());

    assertNull(task.extractType(item));
  }

  // --- isDcatResource ---

  @ParameterizedTest
  @CsvSource({
    "dcat:Catalog,true",
    "dcat:Dataset,true",
    "dcat:DataService,true",
    "foaf:Agent,false"
  })
  void isDcatResourceReturnsExpected(String itemType, boolean expected) {
    assertEquals(expected, task.isDcatResource(itemType));
  }

  @Test
  void isDcatResourceReturnsFalseForNull() {
    assertFalse(task.isDcatResource(null));
  }

  // --- inferTypeFromDcatType ---

  @ParameterizedTest
  @CsvSource({"dcat:Catalog,Catalogue", "dcat:Dataset,Cohort study", "dcat:DataService,"})
  void inferTypeFromDcatTypeReturnsExpected(String dcatType, String expected) {
    String result = task.inferTypeFromDcatType(dcatType);
    if (expected == null || expected.isBlank()) {
      assertNull(result);
    } else {
      assertEquals(expected, result);
    }
  }

  // --- extractIdFromIri ---

  @Test
  void extractIdFromIriReturnsNullForNull() {
    assertNull(task.extractIdFromIri(null, "dcat:Catalog"));
  }

  @Test
  void extractIdFromIriReturnsNullForBlank() {
    assertNull(task.extractIdFromIri("   ", "dcat:Catalog"));
  }

  @Test
  void extractIdFromIriExtractsValueFromKeyEqualsFormat() {
    String result = task.extractIdFromIri("https://example.org/catalog/id=abc123", "dcat:Catalog");
    assertEquals("abc123", result);
  }

  @Test
  void extractIdFromIriPrefixesCatalogSegment() {
    String result = task.extractIdFromIri("https://example.org/catalog/my-catalog", "dcat:Catalog");
    assertEquals("catalog-my-catalog", result);
  }

  @Test
  void extractIdFromIriPrefixesDatasetSegment() {
    String result =
        task.extractIdFromIri("https://example.org/datasets/my-dataset", "dcat:Dataset");
    assertEquals("dataset-my-dataset", result);
  }

  @Test
  void extractIdFromIriNoPrefixForOtherType() {
    String result = task.extractIdFromIri("https://example.org/services/svc1", "dcat:DataService");
    assertEquals("svc1", result);
  }

  // --- nodeToRow ---

  @Test
  void nodeToRowSetsFieldFromReverseContext() {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("@id", "https://example.org/catalog/id=cat1");
    item.put("@type", "dcat:Catalog");
    item.put("dcterms:title", "My Catalog");

    Map<String, String> reverseContext = Map.of("dcterms:title", "title");

    Row row = task.nodeToRow(item, "dcat:Catalog", reverseContext);

    assertEquals("My Catalog", row.getString("title"));
  }

  @Test
  void nodeToRowInfersTypeFromDcatType() {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("@id", "https://example.org/catalog/id=cat1");
    item.put("@type", "dcat:Catalog");

    Row row = task.nodeToRow(item, "dcat:Catalog", Map.of());

    String[] types = row.getStringArray("type");
    assertNotNull(types);
    assertEquals("Catalogue", types[0]);
  }

  @Test
  void nodeToRowExtractsIdFromIriWhenNoIdField() {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("@id", "https://example.org/catalog/id=cat42");
    item.put("@type", "dcat:Catalog");

    Row row = task.nodeToRow(item, "dcat:Catalog", Map.of());

    assertEquals("cat42", row.getString("id"));
  }

  @Test
  void nodeToRowSetsValueFromAtValueObject() {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("@id", "https://example.org/ds/id=ds1");
    item.put("@type", "dcat:Dataset");

    ObjectNode titleNode = MAPPER.createObjectNode();
    titleNode.put("@value", "My Dataset");
    item.set("dcterms:title", titleNode);

    Map<String, String> reverseContext = Map.of("dcterms:title", "title");

    Row row = task.nodeToRow(item, "dcat:Dataset", reverseContext);

    assertEquals("My Dataset", row.getString("title"));
  }

  @Test
  void nodeToRowSetsValueFromAtIdObject() {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("@id", "https://example.org/ds/id=ds1");
    item.put("@type", "dcat:Dataset");

    ObjectNode refNode = MAPPER.createObjectNode();
    refNode.put("@id", "https://example.org/publisher/org1");
    item.set("dcterms:publisher", refNode);

    Map<String, String> reverseContext = Map.of("dcterms:publisher", "publisher");

    Row row = task.nodeToRow(item, "dcat:Dataset", reverseContext);

    assertEquals("https://example.org/publisher/org1", row.getString("publisher"));
  }

  @Test
  void nodeToRowSetsArrayField() {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("@id", "https://example.org/ds/id=ds1");
    item.put("@type", "dcat:Dataset");

    ArrayNode themes = MAPPER.createArrayNode();
    themes.add("theme1");
    themes.add("theme2");
    item.set("dcat:theme", themes);

    Map<String, String> reverseContext = Map.of("dcat:theme", "theme");

    Row row = task.nodeToRow(item, "dcat:Dataset", reverseContext);

    String[] themeValues = row.getStringArray("theme");
    assertNotNull(themeValues);
    assertEquals(2, themeValues.length);
  }

  // --- setRowField ---

  @Test
  void setRowFieldSetsTextualValue() {
    Row row = new Row();
    task.setRowField(row, "title", MAPPER.getNodeFactory().textNode("Hello"));
    assertEquals("Hello", row.getString("title"));
  }

  @Test
  void setRowFieldSetsAtValueObject() {
    Row row = new Row();
    ObjectNode node = MAPPER.createObjectNode();
    node.put("@value", "World");
    task.setRowField(row, "title", node);
    assertEquals("World", row.getString("title"));
  }

  @Test
  void setRowFieldSetsAtIdObject() {
    Row row = new Row();
    ObjectNode node = MAPPER.createObjectNode();
    node.put("@id", "https://example.org/thing");
    task.setRowField(row, "link", node);
    assertEquals("https://example.org/thing", row.getString("link"));
  }

  @Test
  void setRowFieldSetsArrayOfMixedElements() {
    Row row = new Row();
    ArrayNode array = MAPPER.createArrayNode();
    array.add("textItem");
    ObjectNode valueItem = MAPPER.createObjectNode();
    valueItem.put("@value", "valueItem");
    array.add(valueItem);
    ObjectNode idItem = MAPPER.createObjectNode();
    idItem.put("@id", "https://example.org/idItem");
    array.add(idItem);

    task.setRowField(row, "tags", array);

    String[] tags = row.getStringArray("tags");
    assertNotNull(tags);
    assertEquals(3, tags.length);
  }

  @Test
  void setRowFieldIgnoresObjectWithNeitherValueNorId() {
    Row row = new Row();
    ObjectNode node = MAPPER.createObjectNode();
    node.put("unknown", "x");
    task.setRowField(row, "title", node);
    assertFalse(row.getColumnNames().contains("title"));
  }

  @Test
  void setRowFieldIgnoresEmptyArray() {
    Row row = new Row();
    task.setRowField(row, "tags", MAPPER.createArrayNode());
    assertFalse(row.getColumnNames().contains("tags"));
  }

  // --- findTableNameForType ---

  @Test
  void findTableNameForTypeReturnsMatchingKey() {
    ObjectNode frame = MAPPER.createObjectNode();
    ObjectNode context = MAPPER.createObjectNode();
    ObjectNode catalogEntry = MAPPER.createObjectNode();
    catalogEntry.put("@id", "MySchema:Resources");
    catalogEntry.put("@type", "dcat:Catalog");
    context.set("Resources", catalogEntry);
    frame.set("@context", context);

    String result = task.findTableNameForType(frame, "dcat:Catalog");
    assertEquals("Resources", result);
  }

  @Test
  void findTableNameForTypeReturnsNullWhenNoMatch() {
    ObjectNode frame = MAPPER.createObjectNode();
    ObjectNode context = MAPPER.createObjectNode();
    ObjectNode entry = MAPPER.createObjectNode();
    entry.put("@id", "MySchema:Resources");
    entry.put("@type", "dcat:Dataset");
    context.set("Resources", entry);
    frame.set("@context", context);

    assertNull(task.findTableNameForType(frame, "dcat:Catalog"));
  }

  @Test
  void findTableNameForTypeReturnsNullWhenContextMissing() {
    ObjectNode frame = MAPPER.createObjectNode();
    assertNull(task.findTableNameForType(frame, "dcat:Catalog"));
  }
}
