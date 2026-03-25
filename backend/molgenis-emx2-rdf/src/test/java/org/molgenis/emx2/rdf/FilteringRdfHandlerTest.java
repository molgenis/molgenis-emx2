package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.ReverseAnnotationMapper.ColumnMapping;

class FilteringRdfHandlerTest {

  private static final IRI DCTERMS_TITLE = Values.iri("http://purl.org/dc/terms/title");
  private static final IRI DCAT_KEYWORD = Values.iri("http://www.w3.org/ns/dcat#keyword");
  private static final IRI FOAF_NAME = Values.iri("http://xmlns.com/foaf/0.1/name");
  private static final IRI DS1 = Values.iri("https://example.org/ds1");
  private static final IRI DS2 = Values.iri("https://example.org/ds2");
  private static final IRI DCAT_DATASET = Values.iri("http://www.w3.org/ns/dcat#Dataset");

  private Map<IRI, List<ColumnMapping>> predicateMap;
  private ColumnMapping titleMapping;
  private ColumnMapping keywordsMapping;

  @BeforeEach
  void setUp() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata resourcesTable =
        schema.create(new TableMetadata("Resources").setTableType(TableType.DATA));
    Column titleCol = new Column(resourcesTable, "title");
    Column keywordsCol = new Column(resourcesTable, "keywords");

    titleMapping = new ColumnMapping(resourcesTable, titleCol);
    keywordsMapping = new ColumnMapping(resourcesTable, keywordsCol);

    predicateMap = new HashMap<>();
    predicateMap.put(DCTERMS_TITLE, List.of(titleMapping));
    predicateMap.put(DCAT_KEYWORD, List.of(keywordsMapping));
  }

  @Test
  void capturesRdfTypeTriples() throws IOException {
    FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
    String turtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        <https://example.org/ds1> a dcat:Dataset .
        """;
    RdfParser.parseString(handler, turtle);

    Map<Resource, Set<IRI>> typeMap = handler.getTypeMap();
    assertTrue(typeMap.containsKey(DS1));
    assertTrue(typeMap.get(DS1).contains(DCAT_DATASET));
  }

  @Test
  void capturesMatchedTriples() throws IOException {
    FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
    String turtle =
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <https://example.org/ds1> dcterms:title "My Dataset" .
        """;
    RdfParser.parseString(handler, turtle);

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = handler.getMatchedData();
    assertTrue(matchedData.containsKey(DS1));
    assertTrue(matchedData.get(DS1).containsKey(titleMapping));
    assertEquals(1, matchedData.get(DS1).get(titleMapping).size());
    assertEquals("My Dataset", matchedData.get(DS1).get(titleMapping).get(0).stringValue());
  }

  @Test
  void accumulatesMultipleValuesForSamePredicateAndSubject() throws IOException {
    FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
    String turtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        <https://example.org/ds1> dcat:keyword "health", "biobank" .
        """;
    RdfParser.parseString(handler, turtle);

    List<Value> keywords = handler.getMatchedData().get(DS1).get(keywordsMapping);
    assertNotNull(keywords);
    assertEquals(2, keywords.size());
    assertTrue(keywords.stream().anyMatch(v -> v.stringValue().equals("health")));
    assertTrue(keywords.stream().anyMatch(v -> v.stringValue().equals("biobank")));
  }

  @Test
  void discardsUnmatchedTriples() throws IOException {
    FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
    String turtle =
        """
        @prefix foaf: <http://xmlns.com/foaf/0.1/> .
        <https://example.org/ds1> foaf:homepage <https://example.org> .
        """;
    RdfParser.parseString(handler, turtle);

    assertEquals(1, handler.getUnmatchedCount());
    assertTrue(handler.getMatchedData().isEmpty());
  }

  @Test
  void countsUnmatchedTriples() throws IOException {
    FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
    String turtle =
        """
        @prefix foaf: <http://xmlns.com/foaf/0.1/> .
        <https://example.org/ds1> foaf:homepage <https://example.org> .
        <https://example.org/ds2> foaf:name "Alice" .
        """;
    RdfParser.parseString(handler, turtle);

    assertEquals(2, handler.getUnmatchedCount());
  }

  @Test
  void handlesMultipleSubjects() throws IOException {
    FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
    String turtle =
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <https://example.org/ds1> dcterms:title "Dataset One" .
        <https://example.org/ds2> dcterms:title "Dataset Two" .
        """;
    RdfParser.parseString(handler, turtle);

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = handler.getMatchedData();
    assertTrue(matchedData.containsKey(DS1));
    assertTrue(matchedData.containsKey(DS2));
    assertEquals("Dataset One", matchedData.get(DS1).get(titleMapping).get(0).stringValue());
    assertEquals("Dataset Two", matchedData.get(DS2).get(titleMapping).get(0).stringValue());
  }

  @Test
  void rdfTypeNotCountedAsUnmatched() throws IOException {
    FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
    String turtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        <https://example.org/ds1> a dcat:Dataset .
        """;
    RdfParser.parseString(handler, turtle);

    assertEquals(0, handler.getUnmatchedCount());
  }
}
