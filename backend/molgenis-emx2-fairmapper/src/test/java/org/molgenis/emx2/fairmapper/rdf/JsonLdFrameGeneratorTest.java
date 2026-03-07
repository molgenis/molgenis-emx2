package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class JsonLdFrameGeneratorTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonLdFrameGenerator generator;
  private SchemaMetadata schema;

  @BeforeEach
  void setUp() {
    generator = new JsonLdFrameGenerator();
    schema = new SchemaMetadata("TestCatalogue");
  }

  @Test
  void generatesContextWithNamespacePrefixes() {
    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");

    assertNotNull(context);
    assertEquals("http://www.w3.org/ns/dcat#", context.get("dcat").asText());
    assertEquals("http://purl.org/dc/terms/", context.get("dcterms").asText());
    assertEquals("http://xmlns.com/foaf/0.1/", context.get("foaf").asText());
    assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", context.get("rdf").asText());
  }

  @Test
  void generatesRootEmbedAlways() {
    JsonNode frame = generator.generate(schema);
    assertEquals("@always", frame.get("@embed").asText());
  }

  @Test
  void includesSchemaPrefix() {
    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");

    assertNotNull(context.get("TestCatalogue"));
    assertEquals("urn:molgenis:schema:TestCatalogue#", context.get("TestCatalogue").asText());
  }

  @Test
  void mapsTableSemanticsAsTypeInContext() {
    TableMetadata resources = schema.create(new TableMetadata("Resources"));
    resources.setSemantics("dcat:Catalog");

    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");
    JsonNode resourcesNode = context.get("Resources");

    assertNotNull(resourcesNode);
    assertEquals("TestCatalogue:Resources", resourcesNode.get("@id").asText());
    assertEquals("dcat:Catalog", resourcesNode.get("@type").asText());
  }

  @Test
  void mapsStringColumnSemanticsToPredicateInContext() {
    TableMetadata resources = schema.create(new TableMetadata("Resources"));
    resources.add(new Column("name").setType(ColumnType.STRING).setSemantics("dcterms:title"));

    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");
    JsonNode nameNode = context.get("name");

    assertNotNull(nameNode);
    assertEquals("dcterms:title", nameNode.get("@id").asText());
  }

  @Test
  void mapsRefColumnWithSemanticsToIdTypeAndEmbed() {
    TableMetadata resources = schema.create(new TableMetadata("Resources"));
    resources.add(
        new Column("datasets")
            .setType(ColumnType.REF_ARRAY)
            .setRefTable("Resources")
            .setSemantics("dcat:dataset"));

    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");
    JsonNode datasetsNode = context.get("datasets");

    assertNotNull(datasetsNode);
    assertEquals("dcat:dataset", datasetsNode.get("@id").asText());
    assertEquals("@id", datasetsNode.get("@type").asText());

    JsonNode embedNode = frame.get("dcat:dataset");
    assertNotNull(embedNode);
    assertEquals("@always", embedNode.get("@embed").asText());
  }

  @Test
  void mapsRefColumnWithoutSemanticsUsesSchemaPrefix() {
    TableMetadata resources = schema.create(new TableMetadata("Resources"));
    resources.add(new Column("related").setType(ColumnType.REF).setRefTable("Resources"));

    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");
    JsonNode relatedNode = context.get("related");

    assertNotNull(relatedNode);
    assertEquals("TestCatalogue:related", relatedNode.get("@id").asText());
    assertEquals("@id", relatedNode.get("@type").asText());

    JsonNode embedNode = frame.get("TestCatalogue:related");
    assertNotNull(embedNode);
    assertEquals("@always", embedNode.get("@embed").asText());
  }

  @Test
  void mapsOntologyColumnToIdTypeAndEmbed() {
    TableMetadata themes = schema.create(new TableMetadata("Themes"));
    TableMetadata resources = schema.create(new TableMetadata("Resources"));
    resources.add(
        new Column("theme")
            .setType(ColumnType.ONTOLOGY)
            .setRefTable("Themes")
            .setSemantics("dcat:theme"));

    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");
    JsonNode themeNode = context.get("theme");

    assertNotNull(themeNode);
    assertEquals("dcat:theme", themeNode.get("@id").asText());
    assertEquals("@id", themeNode.get("@type").asText());

    JsonNode embedNode = frame.get("dcat:theme");
    assertNotNull(embedNode);
    assertEquals("@always", embedNode.get("@embed").asText());
  }

  @Test
  void ignoresColumnWithNoSemantics() {
    TableMetadata resources = schema.create(new TableMetadata("Resources"));
    resources.add(new Column("notes").setType(ColumnType.TEXT));

    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");

    assertNull(context.get("notes"));
    assertNull(frame.get("notes"));
  }

  @Test
  void deduplicatesColumnsWithSameName() {
    TableMetadata tableA = schema.create(new TableMetadata("TableA"));
    tableA.add(new Column("title").setType(ColumnType.STRING).setSemantics("dcterms:title"));
    TableMetadata tableB = schema.create(new TableMetadata("TableB"));
    tableB.add(new Column("title").setType(ColumnType.STRING).setSemantics("dcterms:title"));

    JsonNode frame = generator.generate(schema);
    JsonNode context = frame.get("@context");
    JsonNode titleNode = context.get("title");

    assertNotNull(titleNode);
    assertEquals("dcterms:title", titleNode.get("@id").asText());
  }

  @Test
  void generateAsStringProducesValidJson() throws Exception {
    TableMetadata resources = schema.create(new TableMetadata("Resources"));
    resources.setSemantics("dcat:Catalog");
    resources.add(new Column("title").setType(ColumnType.STRING).setSemantics("dcterms:title"));

    String json = generator.generateAsString(schema);
    JsonNode parsed = MAPPER.readTree(json);

    assertNotNull(parsed);
    assertTrue(parsed.has("@context"));
    assertTrue(parsed.has("@embed"));
  }
}
