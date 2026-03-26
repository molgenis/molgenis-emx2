package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.ReverseAnnotationMapper.ColumnMapping;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RowBuilderTest {
  private static Database db;
  private static Schema schema;
  private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

  private static final IRI DCAT_DATASET = Values.iri("http://www.w3.org/ns/dcat#Dataset");
  private static final IRI DCAT_CATALOG = Values.iri("http://www.w3.org/ns/dcat#Catalog");
  private static final IRI FOAF_AGENT = Values.iri("http://xmlns.com/foaf/0.1/Agent");
  private static final IRI DCTERMS_TITLE = Values.iri("http://purl.org/dc/terms/title");
  private static final IRI DCTERMS_DESC = Values.iri("http://purl.org/dc/terms/description");
  private static final IRI DCTERMS_ID = Values.iri("http://purl.org/dc/terms/identifier");
  private static final IRI DCAT_KEYWORD = Values.iri("http://www.w3.org/ns/dcat#keyword");
  private static final IRI DCAT_THEME = Values.iri("http://www.w3.org/ns/dcat#theme");
  private static final IRI DCTERMS_PUBLISHER = Values.iri("http://purl.org/dc/terms/publisher");
  private static final IRI FOAF_NAME = Values.iri("http://xmlns.com/foaf/0.1/name");

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema("RowBuilderTest");

    schema.create(
        new TableMetadata("Organisations")
            .setTableType(TableType.DATA)
            .add(new Column("pid").setPkey())
            .add(new Column("name").setSemantics("http://xmlns.com/foaf/0.1/name"))
            .add(new Column("identifier").setSemantics("http://purl.org/dc/terms/identifier")));

    schema.create(
        new TableMetadata("Resources")
            .setTableType(TableType.DATA)
            .add(new Column("pid").setPkey())
            .add(new Column("title").setSemantics("http://purl.org/dc/terms/title"))
            .add(
                new Column("description")
                    .setType(ColumnType.TEXT)
                    .setSemantics("http://purl.org/dc/terms/description"))
            .add(
                new Column("keywords")
                    .setType(ColumnType.STRING_ARRAY)
                    .setSemantics("http://www.w3.org/ns/dcat#keyword"))
            .add(new Column("type").setType(ColumnType.ONTOLOGY).setRefTable("Resource types"))
            .add(
                new Column("themes")
                    .setType(ColumnType.ONTOLOGY_ARRAY)
                    .setRefTable("Data themes")
                    .setSemantics("http://www.w3.org/ns/dcat#theme"))
            .add(
                new Column("publisher")
                    .setType(ColumnType.REF)
                    .setRefTable("Organisations")
                    .setSemantics("http://purl.org/dc/terms/publisher"))
            .add(new Column("identifier").setSemantics("http://purl.org/dc/terms/identifier")));

    Table resourceTypes = schema.getTable("Resource types");
    resourceTypes.insert(
        new Row().setString("name", "Catalogue"), new Row().setString("name", "Cohort study"));

    Table dataThemes = schema.getTable("Data themes");
    dataThemes.insert(
        new Row()
            .setString("name", "Health")
            .setString(
                "ontologyTermURI",
                "http://publications.europa.eu/resource/authority/data-theme/HEAL"),
        new Row()
            .setString("name", "Society")
            .setStringArray(
                "alternativeIds",
                new String[] {"http://publications.europa.eu/resource/authority/data-theme/SOCI"}));
  }

  @Test
  void buildsRowForDatasetWithLiterals() {
    Resource subject = Values.iri("https://example.org/dataset/1");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    Column titleCol = resourcesMeta.getColumn("title");
    Column descCol = resourcesMeta.getColumn("description");
    Column idCol = resourcesMeta.getColumn("identifier");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
    Map<ColumnMapping, List<Value>> subjectData = new HashMap<>();
    subjectData.put(
        new ColumnMapping(resourcesMeta, titleCol), List.of(Values.literal("Test Dataset")));
    subjectData.put(
        new ColumnMapping(resourcesMeta, descCol), List.of(Values.literal("A description")));
    subjectData.put(new ColumnMapping(resourcesMeta, idCol), List.of(Values.literal("ds-1")));
    matchedData.put(subject, subjectData);

    Map<Resource, Set<IRI>> typeMap = Map.of(subject, Set.of(DCAT_DATASET));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    List<Row> rows = result.rowsByTable().get("Resources");
    assertNotNull(rows);
    assertEquals(1, rows.size());
    assertEquals("ds-1", rows.get(0).getString("pid"));
    assertEquals("Test Dataset", rows.get(0).getString("title"));
    assertEquals("A description", rows.get(0).getString("description"));
  }

  @Test
  void setsTypeValueFromDiscriminator() {
    Resource subject = Values.iri("https://example.org/dataset/typed");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    Column idCol = resourcesMeta.getColumn("identifier");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
    Map<ColumnMapping, List<Value>> subjectData = new HashMap<>();
    subjectData.put(new ColumnMapping(resourcesMeta, idCol), List.of(Values.literal("typed-1")));
    matchedData.put(subject, subjectData);

    Map<Resource, Set<IRI>> typeMap = Map.of(subject, Set.of(DCAT_DATASET));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    List<Row> rows = result.rowsByTable().get("Resources");
    assertNotNull(rows);
    assertEquals(1, rows.size());
    assertEquals("Cohort study", rows.get(0).getString("type"));
  }

  @Test
  void handlesStringArrayColumns() {
    Resource subject = Values.iri("https://example.org/dataset/keywords");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    Column keywordsCol = resourcesMeta.getColumn("keywords");
    Column idCol = resourcesMeta.getColumn("identifier");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
    Map<ColumnMapping, List<Value>> subjectData = new HashMap<>();
    subjectData.put(
        new ColumnMapping(resourcesMeta, keywordsCol),
        List.of(Values.literal("health"), Values.literal("biobank")));
    subjectData.put(new ColumnMapping(resourcesMeta, idCol), List.of(Values.literal("kw-1")));
    matchedData.put(subject, subjectData);

    Map<Resource, Set<IRI>> typeMap = Map.of(subject, Set.of(DCAT_DATASET));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    List<Row> rows = result.rowsByTable().get("Resources");
    assertNotNull(rows);
    assertEquals(1, rows.size());
    String[] keywords = rows.get(0).getStringArray("keywords");
    assertNotNull(keywords);
    assertEquals(2, keywords.length);
    assertTrue(Arrays.asList(keywords).contains("health"));
    assertTrue(Arrays.asList(keywords).contains("biobank"));
  }

  @Test
  void resolvesOntologyViaOntologyMapper() {
    Resource subject = Values.iri("https://example.org/dataset/themes");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    Column themesCol = resourcesMeta.getColumn("themes");
    Column idCol = resourcesMeta.getColumn("identifier");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
    Map<ColumnMapping, List<Value>> subjectData = new HashMap<>();
    subjectData.put(
        new ColumnMapping(resourcesMeta, themesCol),
        List.of(
            Values.iri("http://publications.europa.eu/resource/authority/data-theme/HEAL"),
            Values.iri("http://publications.europa.eu/resource/authority/data-theme/SOCI")));
    subjectData.put(new ColumnMapping(resourcesMeta, idCol), List.of(Values.literal("theme-1")));
    matchedData.put(subject, subjectData);

    Map<Resource, Set<IRI>> typeMap = Map.of(subject, Set.of(DCAT_DATASET));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    List<Row> rows = result.rowsByTable().get("Resources");
    assertNotNull(rows);
    assertEquals(1, rows.size());
    String[] themes = rows.get(0).getStringArray("themes");
    assertNotNull(themes);
    assertEquals(2, themes.length);
    List<String> themeList = Arrays.asList(themes);
    assertTrue(themeList.contains("Health"));
    assertTrue(themeList.contains("Society"));
  }

  @Test
  void resolvesRefToInlineResource() {
    Resource dataset = Values.iri("https://example.org/dataset/with-publisher");
    Resource publisher = Values.iri("https://example.org/org/acme");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    TableMetadata orgsMeta = schema.getTable("Organisations").getMetadata();
    Column publisherCol = resourcesMeta.getColumn("publisher");
    Column idCol = resourcesMeta.getColumn("identifier");
    Column orgIdCol = orgsMeta.getColumn("identifier");
    Column orgNameCol = orgsMeta.getColumn("name");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();

    Map<ColumnMapping, List<Value>> datasetData = new HashMap<>();
    datasetData.put(new ColumnMapping(resourcesMeta, publisherCol), List.of(publisher));
    datasetData.put(new ColumnMapping(resourcesMeta, idCol), List.of(Values.literal("pub-ds-1")));
    matchedData.put(dataset, datasetData);

    Map<ColumnMapping, List<Value>> orgData = new HashMap<>();
    orgData.put(new ColumnMapping(orgsMeta, orgNameCol), List.of(Values.literal("ACME Corp")));
    orgData.put(new ColumnMapping(orgsMeta, orgIdCol), List.of(Values.literal("acme")));
    matchedData.put(publisher, orgData);

    Map<Resource, Set<IRI>> typeMap = new HashMap<>();
    typeMap.put(dataset, Set.of(DCAT_DATASET));
    typeMap.put(publisher, Set.of(FOAF_AGENT));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    List<Row> datasetRows = result.rowsByTable().get("Resources");
    assertNotNull(datasetRows);
    assertEquals(1, datasetRows.size());
    assertEquals("acme", datasetRows.get(0).getString("publisher"));
  }

  @Test
  void generatesPidFromUriLocalName() {
    Resource subject = Values.iri("https://example.org/dataset/my-dataset");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    Column titleCol = resourcesMeta.getColumn("title");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
    Map<ColumnMapping, List<Value>> subjectData = new HashMap<>();
    subjectData.put(
        new ColumnMapping(resourcesMeta, titleCol), List.of(Values.literal("My Dataset")));
    matchedData.put(subject, subjectData);

    Map<Resource, Set<IRI>> typeMap = Map.of(subject, Set.of(DCAT_DATASET));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    List<Row> rows = result.rowsByTable().get("Resources");
    assertNotNull(rows);
    assertEquals(1, rows.size());
    assertEquals("my-dataset", rows.get(0).getString("pid"));
  }

  @Test
  void skipsSubjectWithNoTypeAssignment() {
    Resource subject = Values.iri("https://example.org/unknown/thing");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    Column titleCol = resourcesMeta.getColumn("title");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
    Map<ColumnMapping, List<Value>> subjectData = new HashMap<>();
    subjectData.put(new ColumnMapping(resourcesMeta, titleCol), List.of(Values.literal("Unknown")));
    matchedData.put(subject, subjectData);

    Map<Resource, Set<IRI>> typeMap =
        Map.of(subject, Set.of(Values.iri("http://example.org/Unknown")));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    assertNull(result.rowsByTable().get("Resources"));
    assertFalse(result.warnings().isEmpty());
    assertTrue(result.warnings().get(0).contains("no matching table"));
  }

  @Test
  void resolvesCompositePkeyViaReverseRef() {
    Database compositDb = TestDatabaseFactory.getTestDatabase();
    Schema compositSchema = compositDb.dropCreateSchema("RowBuilderTestComposite");

    compositSchema.create(
        new TableMetadata("Resources")
            .setTableType(TableType.DATA)
            .add(new Column("pid").setPkey())
            .add(new Column("title").setSemantics("http://purl.org/dc/terms/title"))
            .add(new Column("identifier").setSemantics("http://purl.org/dc/terms/identifier")),
        new TableMetadata("Organisations")
            .setTableType(TableType.DATA)
            .add(new Column("resource").setType(ColumnType.REF).setRefTable("Resources").setKey(1))
            .add(new Column("id").setKey(1))
            .add(new Column("name").setSemantics("http://xmlns.com/foaf/0.1/name")));

    Resource resSubject = Values.iri("https://example.org/resource/res1");
    Resource orgSubject = Values.iri("https://example.org/org/org1");

    TableMetadata resourcesMeta = compositSchema.getTable("Resources").getMetadata();
    TableMetadata orgsMeta = compositSchema.getTable("Organisations").getMetadata();

    Column resIdCol = resourcesMeta.getColumn("identifier");
    Column publisherCol =
        new Column("publisher")
            .setType(ColumnType.REF)
            .setRefTable("Organisations")
            .setSemantics("http://purl.org/dc/terms/publisher");
    Column orgNameCol = orgsMeta.getColumn("name");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();

    Map<ColumnMapping, List<Value>> resData = new HashMap<>();
    resData.put(new ColumnMapping(resourcesMeta, resIdCol), List.of(Values.literal("res1")));
    resData.put(new ColumnMapping(resourcesMeta, publisherCol), List.of(orgSubject));
    matchedData.put(resSubject, resData);

    Map<ColumnMapping, List<Value>> orgData = new HashMap<>();
    orgData.put(new ColumnMapping(orgsMeta, orgNameCol), List.of(Values.literal("UMCG")));
    matchedData.put(orgSubject, orgData);

    Map<Resource, Set<IRI>> typeMap = new HashMap<>();
    typeMap.put(resSubject, Set.of(DCAT_DATASET));
    typeMap.put(orgSubject, Set.of(FOAF_AGENT));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, compositSchema);

    List<Row> orgRows = result.rowsByTable().get("Organisations");
    assertNotNull(orgRows, "Expected Organisations rows");
    assertEquals(1, orgRows.size());
    assertEquals("res1", orgRows.get(0).getString("resource"));
    assertEquals("org1", orgRows.get(0).getString("id"));
    assertEquals("UMCG", orgRows.get(0).getString("name"));
  }

  @Test
  void duplicatesRowForMultipleParentRefs() {
    Database compositDb = TestDatabaseFactory.getTestDatabase();
    Schema compositSchema = compositDb.dropCreateSchema("RowBuilderTestDuplicate");

    compositSchema.create(
        new TableMetadata("Resources")
            .setTableType(TableType.DATA)
            .add(new Column("pid").setPkey())
            .add(new Column("title").setSemantics("http://purl.org/dc/terms/title"))
            .add(new Column("identifier").setSemantics("http://purl.org/dc/terms/identifier")),
        new TableMetadata("Organisations")
            .setTableType(TableType.DATA)
            .add(new Column("resource").setType(ColumnType.REF).setRefTable("Resources").setKey(1))
            .add(new Column("id").setKey(1))
            .add(new Column("name").setSemantics("http://xmlns.com/foaf/0.1/name")));

    Resource res1Subject = Values.iri("https://example.org/resource/res1");
    Resource res2Subject = Values.iri("https://example.org/resource/res2");
    Resource orgSubject = Values.iri("https://example.org/org/shared-org");

    TableMetadata resourcesMeta = compositSchema.getTable("Resources").getMetadata();
    TableMetadata orgsMeta = compositSchema.getTable("Organisations").getMetadata();

    Column resIdCol = resourcesMeta.getColumn("identifier");
    Column publisherCol =
        new Column("publisher")
            .setType(ColumnType.REF)
            .setRefTable("Organisations")
            .setSemantics("http://purl.org/dc/terms/publisher");
    Column orgNameCol = orgsMeta.getColumn("name");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();

    Map<ColumnMapping, List<Value>> res1Data = new HashMap<>();
    res1Data.put(new ColumnMapping(resourcesMeta, resIdCol), List.of(Values.literal("res1")));
    res1Data.put(new ColumnMapping(resourcesMeta, publisherCol), List.of(orgSubject));
    matchedData.put(res1Subject, res1Data);

    Map<ColumnMapping, List<Value>> res2Data = new HashMap<>();
    res2Data.put(new ColumnMapping(resourcesMeta, resIdCol), List.of(Values.literal("res2")));
    res2Data.put(new ColumnMapping(resourcesMeta, publisherCol), List.of(orgSubject));
    matchedData.put(res2Subject, res2Data);

    Map<ColumnMapping, List<Value>> orgData = new HashMap<>();
    orgData.put(new ColumnMapping(orgsMeta, orgNameCol), List.of(Values.literal("SharedOrg")));
    matchedData.put(orgSubject, orgData);

    Map<Resource, Set<IRI>> typeMap = new HashMap<>();
    typeMap.put(res1Subject, Set.of(DCAT_DATASET));
    typeMap.put(res2Subject, Set.of(DCAT_DATASET));
    typeMap.put(orgSubject, Set.of(FOAF_AGENT));

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, compositSchema);

    List<Row> orgRows = result.rowsByTable().get("Organisations");
    assertNotNull(orgRows, "Expected Organisations rows");
    assertEquals(2, orgRows.size(), "Expected one row per parent resource");
    List<String> resourceValues = orgRows.stream().map(r -> r.getString("resource")).toList();
    assertTrue(resourceValues.contains("res1"), "Expected row for res1");
    assertTrue(resourceValues.contains("res2"), "Expected row for res2");
  }

  @Test
  void skipsSubjectWithNoType() {
    Resource subject = Values.iri("https://example.org/dataset/notype");

    TableMetadata resourcesMeta = schema.getTable("Resources").getMetadata();
    Column titleCol = resourcesMeta.getColumn("title");

    Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
    Map<ColumnMapping, List<Value>> subjectData = new HashMap<>();
    subjectData.put(new ColumnMapping(resourcesMeta, titleCol), List.of(Values.literal("No type")));
    matchedData.put(subject, subjectData);

    Map<Resource, Set<IRI>> typeMap = Map.of();

    RowBuilder.RowBuildResult result = RowBuilder.buildRows(matchedData, typeMap, schema);

    assertNull(result.rowsByTable().get("Resources"));
    assertFalse(result.warnings().isEmpty());
    assertTrue(result.warnings().get(0).contains("no matching table"));
  }
}
