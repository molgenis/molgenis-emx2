package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.yaml.snakeyaml.Yaml;

class Emx2YamlTest {

  @SuppressWarnings("unchecked")
  private static Set<String> stubKeys(String rootYaml, String tableName) {
    Map<String, Object> root = new Yaml().load(rootYaml);
    for (Object entry : (List<Object>) root.get("tables")) {
      if (entry instanceof Map<?, ?> table && tableName.equals(table.get("name"))) {
        return (Set<String>) table.keySet();
      }
    }
    throw new AssertionError("stub not found for table: " + tableName);
  }

  private Path minimalBundleDir() throws Exception {
    return Path.of(getClass().getResource("/yamlbundle/minimal/molgenis.yaml").toURI()).getParent();
  }

  private Path bundleDir(String name) throws Exception {
    return Path.of(getClass().getResource("/yamlbundle/" + name + "/molgenis.yaml").toURI())
        .getParent();
  }

  private static List<String> nonSystemNames(List<Column> columns) {
    return columns.stream()
        .filter(column -> !column.isSystemColumn())
        .map(Column::getName)
        .toList();
  }

  @Test
  void minimalBundleRoundTripsByteIdentical() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(minimalBundleDir());

    SchemaMetadata schema = parsed.schema();
    assertEquals(2, schema.getTables().size());
    assertEquals("1.0.0", parsed.version());
    assertEquals("main", schema.getSettings().get("menu"));
    TableMetadata pet = schema.getTableMetadata("Pet");
    assertNotNull(pet);
    assertEquals("Pets", pet.getLabels().get("en"));
    assertEquals("card", pet.getSettings().get("row_style"));
    assertEquals(1, pet.getColumn("name").getKey());
    assertEquals("true", pet.getColumn("active").getRequired());

    Map<String, String> firstExport = Emx2Yaml.toBundleFiles(parsed);
    Map<String, String> secondExport =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstExport));

    assertEquals(firstExport, secondExport);
  }

  @Test
  void unknownKeyError() {
    Map<String, String> files =
        Map.of(
            "molgenis.yaml", "tables:\n- tables/Bad.yaml\n",
            "tables/Bad.yaml",
                "name: Bad\ncolumns:\n- name: id\n  key: 1\n- name: broken\n  refTabel: Something\n");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(files));

    String message = exception.getMessage();
    assertTrue(message.contains("refTabel"), message);
    assertTrue(message.contains("tables/Bad.yaml"), message);
    assertTrue(message.contains("columns[1]"), message);
    assertTrue(message.contains("line"), message);
    assertTrue(message.contains("column"), message);
  }

  @Test
  void weavingPositions() throws Exception {
    SchemaMetadata schema = Emx2Yaml.fromBundle(bundleDir("woven")).schema();

    Column draftNote = schema.getTableMetadata("DraftReport").getColumn("draftNote");
    Column attachmentInfo = schema.getTableMetadata("Attachments").getColumn("attachmentInfo");
    Column reportId = schema.getTableMetadata("Report").getColumn("reportId");
    Column publishedDate = schema.getTableMetadata("Report").getColumn("publishedDate");

    // file order reportId, draftNote(subclass), attachmentInfo(module), publishedDate -> positions
    assertEquals(reportId.getPosition() + 1, draftNote.getPosition().intValue());
    assertEquals(draftNote.getPosition() + 1, attachmentInfo.getPosition().intValue());
    assertEquals(attachmentInfo.getPosition() + 1, publishedDate.getPosition().intValue());

    // subclass column sits between the two root columns in the merged form
    assertEquals(
        List.of("reportId", "draftNote", "publishedDate"),
        nonSystemNames(schema.getTableMetadata("DraftReport").getColumns()));

    // module column sits between the two root columns in the root's module-merged form
    assertEquals(
        List.of("reportId", "attachmentInfo", "publishedDate"),
        nonSystemNames(schema.getTableMetadata("Report").getColumnsIncludingModules()));
  }

  private static List<String> diamondColumns(String childExtends) {
    Map<String, String> files =
        Map.of(
            "molgenis.yaml",
            "formatVersion: 1\ntables:\n- tables/FilledOutlined.yaml\n- tables/Outlined.yaml\n"
                + "- tables/Filled.yaml\n- tables/Shape.yaml\n",
            "tables/Shape.yaml",
            "name: Shape\ncolumns:\n- name: shapeId\n  key: 1\n- name: shapeName\n",
            "tables/Filled.yaml",
            "name: Filled\nextends: [Shape]\ncolumns:\n- name: fillColor\n",
            "tables/Outlined.yaml",
            "name: Outlined\nextends: [Shape]\ncolumns:\n- name: strokeColor\n",
            "tables/FilledOutlined.yaml",
            "name: FilledOutlined\nextends: "
                + childExtends
                + "\ncolumns:\n- name: cornerRadius\n  type: int\n");
    return nonSystemNames(
        Emx2Yaml.fromBundleFiles(files).schema().getTableMetadata("FilledOutlined").getColumns());
  }

  @Test
  void diamondMergeOrder() throws Exception {
    List<String> parse =
        nonSystemNames(
            Emx2Yaml.fromBundle(bundleDir("diamond"))
                .schema()
                .getTableMetadata("FilledOutlined")
                .getColumns());

    // merge order ignores extends order: ancestors come before descendants (topological), ties
    // broken by the tables: declaration order (Shape, then Outlined before Filled, then the child)
    assertEquals(
        List.of("shapeId", "shapeName", "strokeColor", "fillColor", "cornerRadius"), parse);

    // two extends permutations of the child yield identical column order and metadata
    List<String> forward = diamondColumns("[Filled, Outlined]");
    List<String> reversed = diamondColumns("[Outlined, Filled]");
    assertEquals(parse, forward);
    assertEquals(forward, reversed);
  }

  @Test
  void multiParentOrder() throws Exception {
    List<String> inheritNames =
        Emx2Yaml.fromBundle(bundleDir("diamond"))
            .schema()
            .getTableMetadata("FilledOutlined")
            .getInheritNames();

    assertEquals(List.of("Filled", "Outlined"), inheritNames);
    assertEquals("Filled", inheritNames.get(0));
  }

  @Test
  void enumAndModuleAxes() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(bundleDir("axes"));
    TableMetadata subject = parsed.schema().getTableMetadata("Subject");

    assertEquals(ColumnType.ENUM, subject.getColumn("sex").getColumnType());
    assertEquals(List.of("male", "female", "unknown"), subject.getColumn("sex").getValues());
    assertEquals(ColumnType.MODULE_ARRAY, subject.getColumn("subgroups").getColumnType());
    assertEquals(
        List.of("CockayneSyndrome", "Trichothiodystrophy"),
        subject.getColumn("subgroups").getValues());
    assertEquals(ColumnType.MODULE, subject.getColumn("assay").getColumnType());
    assertTrue(
        subject.getColumn("assay").getValues() == null
            || subject.getColumn("assay").getValues().isEmpty());

    Map<String, String> firstExport = Emx2Yaml.toBundleFiles(parsed);
    SchemaMetadata reparsed = Emx2Yaml.fromBundleFiles(firstExport).schema();
    TableMetadata reparsedSubject = reparsed.getTableMetadata("Subject");
    assertEquals(
        List.of("male", "female", "unknown"), reparsedSubject.getColumn("sex").getValues());
    assertEquals(
        List.of("CockayneSyndrome", "Trichothiodystrophy"),
        reparsedSubject.getColumn("subgroups").getValues());
    assertTrue(
        reparsedSubject.getColumn("assay").getValues() == null
            || reparsedSubject.getColumn("assay").getValues().isEmpty());

    assertEquals(firstExport, Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstExport)));
  }

  @Test
  void headingSplice() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(bundleDir("reuse"));
    TableMetadata resources = parsed.schema().getTableMetadata("Resources");

    assertEquals(
        List.of("id", "familyName", "contactDetails", "email", "phone", "orcid", "sex"),
        nonSystemNames(resources.getColumns()));

    Column heading = resources.getColumn("contactDetails");
    assertTrue(heading.isHeading());
    assertEquals("hasContact", heading.getVisible());

    Column email = resources.getColumn("email");
    assertEquals(ColumnType.EMAIL, email.getColumnType());
    assertNull(
        email.getVisible(), "visible cascade is engine behavior, not materialized on columns");

    assertEquals(heading.getPosition() + 1, resources.getColumn("email").getPosition().intValue());
    assertEquals(
        resources.getColumn("orcid").getPosition() + 1,
        resources.getColumn("sex").getPosition().intValue());

    Map<String, String> firstExport = Emx2Yaml.toBundleFiles(parsed);
    Map<String, String> secondExport =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstExport));
    assertEquals(firstExport, secondExport);

    TableMetadata reparsed =
        Emx2Yaml.fromBundleFiles(firstExport).schema().getTableMetadata("Resources");
    assertEquals(
        List.of("id", "familyName", "contactDetails", "email", "phone", "orcid", "sex"),
        nonSystemNames(reparsed.getColumns()));
    assertTrue(reparsed.getColumn("contactDetails").isHeading());
    assertEquals("hasContact", reparsed.getColumn("contactDetails").getVisible());
  }

  @Test
  void dottedRefTable() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(bundleDir("refs"));
    TableMetadata cohorts = parsed.schema().getTableMetadata("Cohorts");

    // Schema.Table populates both refSchema and refTable
    Column cohortType = cohorts.getColumn("cohortType");
    assertEquals("CatalogueOntologies", cohortType.getRefSchemaName());
    assertEquals("Cohort types", cohortType.getRefTableName());

    // bare name resolves same-schema: refSchema left unset
    Column keywords = cohorts.getColumn("keywords");
    assertNull(keywords.getRefSchemaName());
    assertEquals("Keywords", keywords.getRefTableName());

    // export emits dotted for cross-schema, bare otherwise
    Map<String, String> export = Emx2Yaml.toBundleFiles(parsed);
    String cohortsYaml = export.get("tables/Cohorts.yaml");
    assertTrue(cohortsYaml.contains("CatalogueOntologies.Cohort types"), cohortsYaml);
    assertTrue(cohortsYaml.contains("refTable: Keywords"), cohortsYaml);

    // second cycle is byte-stable (demo fixture round-trips)
    assertEquals(export, Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(export)));

    // a YAML refSchema key errors as unknown
    Map<String, String> bad =
        Map.of(
            "molgenis.yaml",
            "tables:\n- tables/Bad.yaml\n",
            "tables/Bad.yaml",
            "name: Bad\ncolumns:\n- name: id\n  key: 1\n- name: ref\n  refSchema: Other\n"
                + "  refTable: Thing\n");
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(bad));
    assertTrue(exception.getMessage().contains("refSchema"), exception.getMessage());
  }

  @Test
  void i18nLocaleKeys() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(bundleDir("refs"));
    TableMetadata cohorts = parsed.schema().getTableMetadata("Cohorts");

    // bare label/description = default (en) locale; @nl variant roundtrips
    assertEquals("Cohorts", cohorts.getLabels().get("en"));
    assertEquals("Cohorten", cohorts.getLabels().get("nl"));
    assertEquals("Study cohorts", cohorts.getDescriptions().get("en"));
    assertEquals("Studie cohorten", cohorts.getDescriptions().get("nl"));

    // a SchemaMetadata carrying a nl label exports to label@nl (CSV label:nl equivalence)
    SchemaMetadata manual = new SchemaMetadata();
    TableMetadata widget = new TableMetadata("Widget");
    widget.setLabel("Widget");
    widget.setLabel("Dingetje", "nl");
    widget.add(new Column("id").setKey(1));
    manual.create(widget);
    Map<String, String> exported = Emx2Yaml.toBundleFiles(new Emx2YamlBundle(manual, 1, null));
    String widgetYaml = exported.get("tables/Widget.yaml");
    assertTrue(widgetYaml.contains("label@nl"), widgetYaml);
    SchemaMetadata reparsed = Emx2Yaml.fromBundleFiles(exported).schema();
    assertEquals("Widget", reparsed.getTableMetadata("Widget").getLabels().get("en"));
    assertEquals("Dingetje", reparsed.getTableMetadata("Widget").getLabels().get("nl"));

    // a map-valued label errors with path and position
    Map<String, String> bad =
        Map.of(
            "molgenis.yaml",
            "tables:\n- tables/Bad.yaml\n",
            "tables/Bad.yaml",
            "name: Bad\ncolumns:\n- name: id\n  key: 1\n- name: broken\n  label:\n"
                + "    nl: Kapot\n");
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(bad));
    assertTrue(exception.getMessage().contains("label"), exception.getMessage());
    assertTrue(exception.getMessage().contains("line"), exception.getMessage());
  }

  @Test
  void ontologyTablesExportAsStubs() {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata cohorts = new TableMetadata("Cohorts");
    cohorts.add(new Column("id").setKey(1));
    cohorts.add(new Column("keywords").setType(ColumnType.ONTOLOGY_ARRAY).setRefTable("Keywords"));
    schema.create(cohorts);
    TableMetadata keywords = new TableMetadata("Keywords");
    keywords.setTableType(TableType.ONTOLOGIES);
    keywords.setLabel("Keywords");
    keywords.setLabel("Trefwoorden", "nl");
    keywords.setDescription("Controlled keyword terms");
    keywords.setSemantics("sio:keyword");
    keywords.setProfiles("catalogue");
    schema.create(keywords);

    // the stub carries only metadata: name, tableType, label(+@locale), description, semantics,
    // profiles -- never a columns: block (its engine columns) and never term rows
    Map<String, String> bundle = Emx2Yaml.toBundleFiles(new Emx2YamlBundle(schema, 1, null));
    String rootYaml = bundle.get("molgenis.yaml");
    assertFalse(bundle.containsKey("tables/Keywords.yaml"), rootYaml);
    assertTrue(rootYaml.contains("name: Keywords"), rootYaml);
    assertTrue(rootYaml.contains("tableType: ontology"), rootYaml);
    assertTrue(rootYaml.contains("Controlled keyword terms"), rootYaml);
    assertTrue(rootYaml.contains("sio:keyword"), rootYaml);
    assertTrue(rootYaml.contains("catalogue"), rootYaml);
    assertTrue(rootYaml.contains("label@nl: Trefwoorden"), rootYaml);

    // nothing else: the emitted stub mapping carries only allowed metadata keys, no stray settings
    Set<String> stubKeys = stubKeys(rootYaml, "Keywords");
    Set<String> allowed =
        Set.of("name", "tableType", "label", "description", "semantics", "profiles");
    for (String key : stubKeys) {
      assertTrue(
          allowed.contains(key) || key.startsWith("label@") || key.startsWith("description@"),
          "unexpected stub key: " + key + " in " + stubKeys);
    }

    // the stub round-trips its metadata onto the (auto-created) ontology table, no duplicate error
    SchemaMetadata reparsed = Emx2Yaml.fromBundleFiles(bundle).schema();
    TableMetadata reKeywords = reparsed.getTableMetadata("Keywords");
    assertNotNull(reKeywords);
    assertEquals(TableType.ONTOLOGIES, reKeywords.getTableType());
    assertTrue(reKeywords.getColumns().isEmpty(), "stub must not carry term/engine columns");
    assertEquals("Controlled keyword terms", reKeywords.getDescriptions().get("en"));
    assertEquals("Trefwoorden", reKeywords.getLabels().get("nl"));
    assertArrayEquals(new String[] {"sio:keyword"}, reKeywords.getSemantics());
    assertArrayEquals(new String[] {"catalogue"}, reKeywords.getProfiles());
  }

  @Test
  void systemSettingsNotExported() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.setSetting("menu", "main");
    schema.setSetting("mg_model_version", "3.2.1");
    TableMetadata pet = new TableMetadata("Pet");
    pet.add(new Column("id").setKey(1));
    pet.setSetting("row_style", "card");
    pet.setSetting("mg_draft", "true");
    schema.create(pet);

    String bundle =
        String.join("\n", Emx2Yaml.toBundleFiles(new Emx2YamlBundle(schema, 1, null)).values());
    String single = Emx2Yaml.toSingleFile(new Emx2YamlBundle(schema, 1, null));

    for (String form : List.of(bundle, single)) {
      assertFalse(form.contains("mg_model_version"), form);
      assertFalse(form.contains("mg_draft"), form);
      assertTrue(form.contains("menu"), form);
      assertTrue(form.contains("row_style"), form);
    }
  }

  @Test
  void headingProfilesRoundtrip() {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata table = new TableMetadata("Survey");
    table.add(new Column("id").setKey(1));
    table.add(
        new Column("Section A")
            .setType(ColumnType.HEADING)
            .setProfiles("ProfileX")
            .setSemantics("sio:section"));
    schema.create(table);

    String yaml = Emx2Yaml.toSingleFile(new Emx2YamlBundle(schema, 1, null));
    SchemaMetadata reparsed = Emx2Yaml.fromBundleFiles(Map.of("molgenis.yaml", yaml)).schema();
    Column heading = reparsed.getTableMetadata("Survey").getColumn("Section A");

    assertTrue(heading.isHeading());
    assertArrayEquals(new String[] {"ProfileX"}, heading.getProfiles());
    assertArrayEquals(new String[] {"sio:section"}, heading.getSemantics());
  }

  @Test
  void subclassProfilesRoundtrip() {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata parent = new TableMetadata("Component");
    parent.add(new Column("id").setKey(1));
    schema.create(parent);
    TableMetadata child = new TableMetadata("Image");
    child.setInheritNames("Component");
    child.setProfiles("pages");
    child.setSemantics("sio:image");
    schema.create(child);

    String yaml = Emx2Yaml.toSingleFile(new Emx2YamlBundle(schema, 1, null));
    SchemaMetadata reparsed = Emx2Yaml.fromBundleFiles(Map.of("molgenis.yaml", yaml)).schema();
    TableMetadata reparsedChild = reparsed.getTableMetadata("Image");

    assertArrayEquals(new String[] {"pages"}, reparsedChild.getProfiles());
    assertArrayEquals(new String[] {"sio:image"}, reparsedChild.getSemantics());
  }

  @Test
  void surfaceV2Forms() throws Exception {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata base = new TableMetadata("Base");
    base.add(new Column("id").setKey(1));
    base.add(new Column("note"));
    schema.create(base);
    TableMetadata middle = new TableMetadata("Middle");
    middle.setInheritNames("Base");
    schema.create(middle);
    TableMetadata leaf = new TableMetadata("Leaf");
    leaf.setInheritNames("Middle");
    schema.create(leaf);

    Map<String, String> export = Emx2Yaml.toBundleFiles(new Emx2YamlBundle(schema, 1, null));
    String rootYaml = export.get("molgenis.yaml");
    String baseYaml = export.get("tables/Base.yaml");

    // bundle table entries are bare-string file refs, never a file: mapping key
    assertTrue(rootYaml.contains("- tables/Base.yaml"), rootYaml);
    assertFalse(rootYaml.contains("file:"), rootYaml);

    // defaults omitted: string type, unset required, block-implied tableType
    assertFalse(baseYaml.contains("type:"), baseYaml);
    assertFalse(baseYaml.contains("required:"), baseYaml);
    assertFalse(baseYaml.contains("tableType:"), baseYaml);

    // root-implied extends omitted (Middle -> Base), single non-root extends is scalar (Leaf ->
    // Middle), never a one-element list
    assertTrue(baseYaml.contains("extends: Middle"), baseYaml);
    assertFalse(baseYaml.contains("extends: [Middle]"), baseYaml);
    assertFalse(baseYaml.contains("extends: Base"), baseYaml);
    assertFalse(baseYaml.contains("extends: [Base]"), baseYaml);

    // multi-parent extends stays a list
    Map<String, String> diamond = Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundle(bundleDir("diamond")));
    String shapeYaml = diamond.get("tables/Shape.yaml");
    assertTrue(shapeYaml.contains("- Filled"), shapeYaml);
    assertTrue(shapeYaml.contains("- Outlined"), shapeYaml);

    // the old schemas: key no longer parses; additionalSchemas is the surface key
    Map<String, String> legacy = Map.of("molgenis.yaml", "tables: []\nschemas:\n  other: {}\n");
    assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(legacy));
  }

  private static String metadataCsv(SchemaMetadata schema) {
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    Emx2.outputMetadata(store, schema);
    return store.getCsvString("molgenis");
  }

  @Test
  void fullSurfaceCsvParity() throws Exception {
    SchemaMetadata yamlSchema = Emx2Yaml.fromBundle(bundleDir("fullsurface")).schema();

    Reader csvReader =
        new InputStreamReader(
            getClass().getResourceAsStream("/yamlbundle/fullsurface/molgenis.csv"),
            StandardCharsets.UTF_8);
    SchemaMetadata csvSchema = Emx2.fromRowList(CsvTableReader.read(csvReader));

    assertEquals(metadataCsv(csvSchema), metadataCsv(yamlSchema));
  }

  @Test
  void exportFidelity() throws Exception {
    Emx2YamlBundle model = Emx2Yaml.fromBundle(bundleDir("fullsurface"));
    Emx2YamlBundle reparsed = Emx2Yaml.fromBundleFiles(Emx2Yaml.toBundleFiles(model));

    // engine-level column attributes survive parse -> export -> parse
    TableMetadata resources = reparsed.schema().getTableMetadata("Resources");
    assertEquals("${contactId}", resources.getColumn("contact").getRefLabel());
    assertTrue(resources.getColumn("contact").isReadonly());
    assertEquals("contact", resources.getColumn("altContact").getRefLink());
    assertEquals("id", resources.getColumn("displayName").getComputed());
    assertEquals("Display name", resources.getColumn("displayName").getFormLabel());
    assertEquals("unknown", resources.getColumn("sex").getDefaultValue());
    assertEquals("sex != null", resources.getColumn("consentGiven").getRequired());
    assertEquals("sex == 'female'", resources.getColumn("visibleNote").getVisible());
    assertEquals("value != null", resources.getColumn("visibleNote").getValidation());
    assertEquals(2, resources.getColumn("orgCode").getKey());
    assertEquals(2, resources.getColumn("regionCode").getKey());
    assertEquals("http://schema.org/url", resources.getColumn("homepage").getSemantics()[0]);
    assertEquals("datacatalogue", resources.getColumn("profiledField").getProfiles()[0]);

    // table-level attributes survive
    assertEquals("http://schema.org/Dataset", resources.getSemantics()[0]);
    assertEquals("datacatalogue", resources.getProfiles()[0]);

    // refBack survives on the twin table
    assertEquals(
        "contact",
        reparsed.schema().getTableMetadata("Contacts").getColumn("linkedResources").getRefBack());

    // document-layer attributes survive: namespaces + previousNames
    assertEquals("http://schema.org/", reparsed.namespaces().get("schema"));
    assertEquals(
        List.of("surname", "last_name"),
        reparsed.previousNames().get("Resources").get("familyName"));
  }

  @Test
  void fileConvergence() throws Exception {
    Map<String, String> firstCycle =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundle(bundleDir("fullsurface")));
    Map<String, String> secondCycle = Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstCycle));

    assertEquals(firstCycle, secondCycle);
  }

  @Test
  void singleFileEquivalence() throws Exception {
    SchemaMetadata inline = Emx2Yaml.fromBundle(bundleDir("singlefile/inline")).schema();
    // the table-level import resolves inside the inlined form
    assertEquals(
        List.of("id", "contactDetails", "email", "phone"),
        nonSystemNames(inline.getTableMetadata("People").getColumns()));

    Map<String, String> bundleForm =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundle(bundleDir("singlefile/bundle")));
    Map<String, String> inlineForm =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundle(bundleDir("singlefile/inline")));

    assertEquals(bundleForm, inlineForm);
  }

  @Test
  void formatVersionSkew() {
    Map<String, String> files =
        Map.of("molgenis.yaml", "formatVersion: 999\ntables:\n- tables/Missing.yaml\n");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(files));

    String message = exception.getMessage();
    assertTrue(message.contains("999"), message);
    assertTrue(message.toLowerCase().contains("formatversion"), message);
    assertTrue(message.contains("newer"), message);
  }
}
