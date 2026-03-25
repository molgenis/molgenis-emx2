package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.ReverseAnnotationMapper.ColumnMapping;

class ReverseAnnotationMapperTest {

  private static final String DCTERMS_TITLE = "http://purl.org/dc/terms/title";
  private static final String DCAT_LANDING_PAGE = "http://www.w3.org/ns/dcat#landingPage";
  private static final String FOAF_HOMEPAGE = "http://xmlns.com/foaf/0.1/homepage";
  private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

  @Test
  void singleSemanticMapsToOneEntry() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata table =
        schema.create(new TableMetadata("Resources").setTableType(TableType.DATA));
    table.add(new Column("title").setSemantics(DCTERMS_TITLE));

    Map<IRI, List<ColumnMapping>> map = ReverseAnnotationMapper.buildPredicateMap(schema);

    assertEquals(1, map.size());
    IRI predicate = Values.iri(DCTERMS_TITLE);
    assertTrue(map.containsKey(predicate));
    assertEquals(1, map.get(predicate).size());
    assertEquals("title", map.get(predicate).get(0).column().getName());
  }

  @Test
  void multipleSemanticsMapsToMultipleEntries() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata table =
        schema.create(new TableMetadata("Resources").setTableType(TableType.DATA));
    table.add(new Column("landingPage").setSemantics(DCAT_LANDING_PAGE, FOAF_HOMEPAGE));

    Map<IRI, List<ColumnMapping>> map = ReverseAnnotationMapper.buildPredicateMap(schema);

    assertEquals(2, map.size());
    IRI dcatPredicate = Values.iri(DCAT_LANDING_PAGE);
    IRI foafPredicate = Values.iri(FOAF_HOMEPAGE);
    assertTrue(map.containsKey(dcatPredicate));
    assertTrue(map.containsKey(foafPredicate));
    assertEquals("landingPage", map.get(dcatPredicate).get(0).column().getName());
    assertEquals("landingPage", map.get(foafPredicate).get(0).column().getName());
  }

  @Test
  void multipleColumnsWithSameSemantic() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata tableA =
        schema.create(new TableMetadata("ResourcesA").setTableType(TableType.DATA));
    tableA.add(new Column("title").setSemantics(DCTERMS_TITLE));
    TableMetadata tableB =
        schema.create(new TableMetadata("ResourcesB").setTableType(TableType.DATA));
    tableB.add(new Column("name").setSemantics(DCTERMS_TITLE));

    Map<IRI, List<ColumnMapping>> map = ReverseAnnotationMapper.buildPredicateMap(schema);

    IRI predicate = Values.iri(DCTERMS_TITLE);
    assertEquals(1, map.size());
    assertEquals(2, map.get(predicate).size());
  }

  @Test
  void computedColumnsExcluded() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata table =
        schema.create(new TableMetadata("Resources").setTableType(TableType.DATA));
    table.add(new Column("computed").setSemantics(DCTERMS_TITLE).setComputed("row.id"));

    Map<IRI, List<ColumnMapping>> map = ReverseAnnotationMapper.buildPredicateMap(schema);

    assertTrue(map.isEmpty());
  }

  @Test
  void rdfTypeExcluded() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata table =
        schema.create(new TableMetadata("Resources").setTableType(TableType.DATA));
    table.add(new Column("type").setSemantics(RDF_TYPE));

    Map<IRI, List<ColumnMapping>> map = ReverseAnnotationMapper.buildPredicateMap(schema);

    assertFalse(map.containsKey(Values.iri(RDF_TYPE)));
    assertTrue(map.isEmpty());
  }

  @Test
  void ontologyTablesExcluded() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata ontologyTable =
        schema.create(new TableMetadata("Diseases").setTableType(TableType.ONTOLOGIES));
    ontologyTable.add(new Column("label").setSemantics(DCTERMS_TITLE));

    Map<IRI, List<ColumnMapping>> map = ReverseAnnotationMapper.buildPredicateMap(schema);

    assertTrue(map.isEmpty());
  }

  @Test
  void nullSemanticsSkipped() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata table =
        schema.create(new TableMetadata("Resources").setTableType(TableType.DATA));
    table.add(new Column("noSemantics"));

    Map<IRI, List<ColumnMapping>> map = ReverseAnnotationMapper.buildPredicateMap(schema);

    assertTrue(map.isEmpty());
  }
}
