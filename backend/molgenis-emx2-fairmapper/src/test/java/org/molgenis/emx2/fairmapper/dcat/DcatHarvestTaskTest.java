package org.molgenis.emx2.fairmapper.dcat;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFramer;

class DcatHarvestTaskTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String RESOURCE_BASE = "/org/molgenis/emx2/fairmapper/dcat/";

  private static JsonNode frame;
  private static JsonNode framedOutput;
  private static List<JsonNode> graphItems;
  private static DcatHarvestTask task;

  @BeforeAll
  static void loadFixtures() throws Exception {
    frame = MAPPER.readTree(loadResource("roundtrip-frame.jsonld"));
    String ttl = loadResource("roundtrip-semantic-export.ttl");
    Model model = DcatHarvestTask.parseTurtle(ttl);
    JsonLdFramer framer = new JsonLdFramer();
    framedOutput = framer.frame(model, frame);
    task = new DcatHarvestTask(null, "test");
    graphItems = task.extractGraphItems(framedOutput);
  }

  @Test
  void framingProducesGraphItems() {
    assertFalse(graphItems.isEmpty(), "Framing should produce at least one graph item");
  }

  @Test
  void graphItemsContainTypedItems() {
    long typedItems = graphItems.stream().filter(item -> item.has("@type")).count();
    assertTrue(typedItems > 0, "At least some graph items should have @type");
  }

  @Test
  void graphItemsHaveTitles() {
    long itemsWithTitle =
        graphItems.stream()
            .filter(
                item -> item.has("dcterms:title") && !item.get("dcterms:title").asText().isBlank())
            .count();
    assertTrue(itemsWithTitle > 0, "At least one graph item should have a title");
  }

  @Test
  void reverseContextMapsExpectedPredicates() {
    Map<String, String> reverseContext = task.buildReverseContext(frame.get("@context"));

    assertEquals("name", reverseContext.get("dcterms:title"));
    assertEquals("description", reverseContext.get("dcterms:description"));
    assertEquals("keywords", reverseContext.get("dcat:keyword"));
    assertNotNull(reverseContext.get("dcterms:identifier"));
  }

  @Test
  void reverseContextExcludesReferenceEntries() {
    Map<String, String> reverseContext = task.buildReverseContext(frame.get("@context"));

    assertFalse(reverseContext.containsKey("dcat:dataset"));
  }

  @Test
  void reverseContextExcludesNamespacePrefixes() {
    Map<String, String> reverseContext = task.buildReverseContext(frame.get("@context"));

    assertNull(reverseContext.get("dcat"));
    assertNull(reverseContext.get("dcterms"));
    assertNull(reverseContext.get("foaf"));
  }

  @Test
  void reverseContextReturnsEmptyMapForNullInput() {
    Map<String, String> reverseContext = task.buildReverseContext(null);
    assertTrue(reverseContext.isEmpty());
  }

  @Test
  void isReferenceEntryReturnsTrueForIdType() throws Exception {
    JsonNode referenceEntry = MAPPER.readTree("{\"@id\": \"dcat:dataset\", \"@type\": \"@id\"}");
    assertTrue(task.isReferenceEntry(referenceEntry));
  }

  @Test
  void isReferenceEntryReturnsFalseForLiteralEntry() throws Exception {
    JsonNode literalEntry = MAPPER.readTree("{\"@id\": \"dcterms:title\"}");
    assertFalse(task.isReferenceEntry(literalEntry));
  }

  @Test
  void extractGraphItemsReturnsItemsFromGraphArray() {
    assertTrue(graphItems.size() > 1, "Real DATA_CATALOGUE should produce multiple graph items");
  }

  @Test
  void extractGraphItemsReturnsAllItemsFromSyntheticGraph() throws Exception {
    JsonNode withGraph =
        MAPPER.readTree(
            "{\"@graph\": [{\"@type\": \"dcat:Catalog\"}, {\"@type\": \"dcat:Dataset\"}]}");
    List<JsonNode> items = task.extractGraphItems(withGraph);
    assertEquals(2, items.size());
  }

  @Test
  void extractGraphItemsReturnsEmptyForNoTypeAndNoGraph() throws Exception {
    JsonNode empty = MAPPER.readTree("{\"foo\": \"bar\"}");
    List<JsonNode> items = task.extractGraphItems(empty);
    assertTrue(items.isEmpty());
  }

  @Test
  void inferTypeFromDcatTypeCatalogReturnsCatalogue() {
    assertEquals("Catalogue", task.inferTypeFromDcatType("dcat:Catalog"));
  }

  @Test
  void inferTypeFromDcatTypeDatasetReturnsCohortStudy() {
    assertEquals("Cohort study", task.inferTypeFromDcatType("dcat:Dataset"));
  }

  @Test
  void inferTypeFromDcatTypeUnknownReturnsNull() {
    assertNull(task.inferTypeFromDcatType("dcat:DataService"));
    assertNull(task.inferTypeFromDcatType("unknown:Type"));
    assertNull(task.inferTypeFromDcatType(null));
  }

  @Test
  void nodeToRowExtractsTitleFromGraphItem() {
    Map<String, String> reverseContext = task.buildReverseContext(frame.get("@context"));
    JsonNode itemWithTitle = findItemWithField("dcterms:title");
    assertNotNull(itemWithTitle, "Should find at least one item with a title");

    String itemType = itemWithTitle.has("@type") ? itemWithTitle.get("@type").asText() : null;
    Row row = task.nodeToRow(itemWithTitle, itemType, reverseContext);

    assertNotNull(row.getString("name"), "Row should have name from dcterms:title");
    assertFalse(row.getString("name").isBlank());
  }

  @Test
  void nodeToRowSetsInferredTypeForCatalog() {
    Map<String, String> reverseContext = task.buildReverseContext(frame.get("@context"));
    JsonNode catalogItem = findItemWithType("dcat:Catalog");
    if (catalogItem == null) return;

    Row row = task.nodeToRow(catalogItem, "dcat:Catalog", reverseContext);

    String[] types = row.getStringArray("type");
    assertNotNull(types);
    assertEquals(1, types.length);
    assertEquals("Catalogue", types[0]);
  }

  @Test
  void nodeToRowExtractsIdFromIri() {
    Map<String, String> reverseContext = task.buildReverseContext(frame.get("@context"));
    JsonNode itemWithId = findItemWithField("@id");
    assertNotNull(itemWithId);

    String itemType = itemWithId.has("@type") ? itemWithId.get("@type").asText() : null;
    Row row = task.nodeToRow(itemWithId, itemType, reverseContext);

    assertNotNull(row.getString("id"), "Row should have an id extracted from the IRI");
    assertFalse(row.getString("id").isBlank());
  }

  @Test
  void nodeToRowExtractsKeywordsFromItem() {
    Map<String, String> reverseContext = task.buildReverseContext(frame.get("@context"));
    String keywordColumn = reverseContext.get("dcat:keyword");
    assertNotNull(keywordColumn, "Reverse context should map dcat:keyword");

    JsonNode itemWithKeywords = null;
    for (JsonNode item : graphItems) {
      JsonNode kw = item.get("dcat:keyword");
      if (kw != null && kw.isArray() && kw.size() > 0) {
        itemWithKeywords = item;
        break;
      }
    }
    if (itemWithKeywords == null) return;

    String itemType = itemWithKeywords.has("@type") ? itemWithKeywords.get("@type").asText() : null;
    Row row = task.nodeToRow(itemWithKeywords, itemType, reverseContext);

    String[] keywords = row.getStringArray(keywordColumn);
    assertNotNull(keywords);
    assertTrue(keywords.length > 0);
  }

  private static JsonNode findItemWithField(String fieldName) {
    for (JsonNode item : graphItems) {
      if (item.has(fieldName) && !item.get(fieldName).isNull()) {
        return item;
      }
    }
    return null;
  }

  private static JsonNode findItemWithType(String dcatType) {
    for (JsonNode item : graphItems) {
      if (item.has("@type") && dcatType.equals(item.get("@type").asText())) {
        return item;
      }
    }
    return null;
  }

  private static String loadResource(String name) throws Exception {
    String path = RESOURCE_BASE + name;
    try (InputStream is = DcatHarvestTaskTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Resource not found: " + path);
      }
      return new String(is.readAllBytes());
    }
  }
}
