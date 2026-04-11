package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.*;

class Emx2YamlBundleTest {

  private static final Path PROFILES_DIR = findProfilesPath();

  private static Path findProfilesPath() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("profiles");
      if (candidate.toFile().isDirectory()) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Could not find profiles directory from: " + Path.of("").toAbsolutePath());
  }

  @Test
  void loadPetstoreSingleFileBundle() throws IOException {
    Path petstoreYaml = PROFILES_DIR.resolve("petstore.yaml");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(petstoreYaml);

    assertEquals("Pet store", bundle.getName());
    assertNotNull(bundle.getDescription());

    SchemaMetadata schema = bundle.getSchema();
    assertNotNull(schema.getTableMetadata("Pet"));
    assertNotNull(schema.getTableMetadata("Category"));
    assertNotNull(schema.getTableMetadata("Order"));
    assertNotNull(schema.getTableMetadata("User"));
    assertNotNull(schema.getTableMetadata("Types"));
    assertNotNull(schema.getTableMetadata("Options"));

    TableMetadata pet = schema.getTableMetadata("Pet");
    List<Column> petColumns = pet.getNonInheritedColumns();
    assertFalse(petColumns.isEmpty(), "Pet should have columns");

    Column nameCol =
        petColumns.stream()
            .filter(c -> c.getName().equals("name"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Column 'name' not found in Pet"));
    assertEquals(1, nameCol.getKey());
    assertEquals("true", nameCol.getRequired());

    Column categoryCol =
        petColumns.stream()
            .filter(c -> c.getName().equals("category"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Column 'category' not found in Pet"));
    assertEquals(ColumnType.RADIO, categoryCol.getColumnType());
    assertEquals("Category", categoryCol.getRefTableName());
  }

  @Test
  void loadPetstoreSectionAndHeadingColumns() throws IOException {
    Path petstoreYaml = PROFILES_DIR.resolve("petstore.yaml");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(petstoreYaml);

    SchemaMetadata schema = bundle.getSchema();
    TableMetadata pet = schema.getTableMetadata("Pet");
    assertNotNull(pet);

    Column statusCol = pet.getColumn("status");
    assertNotNull(statusCol, "Column 'status' from section 'details' should be present");

    Column tagsCol = pet.getColumn("tags");
    assertNotNull(tagsCol);
    assertEquals(ColumnType.ONTOLOGY_ARRAY, tagsCol.getColumnType());

    Column ordersCol = pet.getColumn("orders");
    assertNotNull(
        ordersCol,
        "Column 'orders' from heading 'Heading2' inside section 'details' should be present");
    assertEquals(ColumnType.REFBACK, ordersCol.getColumnType());
  }

  @Test
  void loadPetstoreUserSectionColumns() throws IOException {
    Path petstoreYaml = PROFILES_DIR.resolve("petstore.yaml");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(petstoreYaml);

    TableMetadata user = bundle.getSchema().getTableMetadata("User");
    assertNotNull(user);

    Column streetCol = user.getColumn("street");
    assertNotNull(streetCol, "Column 'street' from section 'address' should be present");
  }

  @Test
  void loadSharedDirectoryBundle() throws IOException {
    Path sharedDir = PROFILES_DIR.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    assertEquals("MOLGENIS shared catalogue bundle", bundle.getName());
    assertNotNull(bundle.getDescription());

    assertNotNull(bundle.getProfileRegistry(), "profileRegistry must not be null");
    assertNotNull(bundle.getTemplateRegistry(), "templateRegistry must not be null");

    assertTrue(
        bundle.getProfileRegistry().containsKey("datacatalogueflat"),
        "datacatalogueflat internal profile must be in profileRegistry");
    assertTrue(
        bundle.getProfileRegistry().containsKey("rwestaging"),
        "rwestaging internal profile must be in profileRegistry");
    assertTrue(
        bundle.getTemplateRegistry().containsKey("cohortsstaging"),
        "cohortsstaging profile must be in templateRegistry");
    assertTrue(
        bundle.getTemplateRegistry().containsKey("patient_registry"),
        "patient_registry profile must be in templateRegistry");
  }

  @Test
  void loadSharedBundleSubsetIncludes() throws IOException {
    Path sharedDir = PROFILES_DIR.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    ProfileEntry rweStaging = bundle.getProfileRegistry().get("rwestaging");
    assertNotNull(rweStaging);
    assertTrue(
        rweStaging.getIncludes().contains("datacatalogueflat"),
        "rwestaging should include datacatalogueflat");

    ProfileEntry datacatalogueflat = bundle.getProfileRegistry().get("datacatalogueflat");
    assertNotNull(datacatalogueflat);
  }

  @Test
  void loadSharedBundleProcessesTableSubtypes() throws IOException {
    Path sharedDir = PROFILES_DIR.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    SchemaMetadata schema = bundle.getSchema();
    assertNotNull(schema.getTableMetadata("Processes"), "Processes table must be loaded");
    assertNotNull(schema.getTableMetadata("Analyses"), "Analyses subtype must be loaded");
    assertNotNull(schema.getTableMetadata("Observations"), "Observations subtype must be loaded");
    assertNotNull(
        schema.getTableMetadata("NGS sequencing"), "NGS sequencing subtype must be loaded");

    TableMetadata analyses = schema.getTableMetadata("Analyses");
    assertNotNull(analyses.getExtendNames(), "Analyses must inherit from Processes");
    assertTrue(
        List.of(analyses.getExtendNames()).contains("Processes"),
        "Analyses must inherit Processes");
  }

  @Test
  void loadSharedBundleProcessesTableColumns() throws IOException {
    Path sharedDir = PROFILES_DIR.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    TableMetadata processes = bundle.getSchema().getTableMetadata("Processes");
    assertNotNull(processes);

    Column idCol = processes.getColumn("id");
    assertNotNull(idCol, "Column 'id' must be present in Processes");
    assertEquals(ColumnType.AUTO_ID, idCol.getColumnType());
    assertEquals(1, idCol.getKey());
  }

  @Test
  void loadSharedBundleColumnSubsets() throws IOException {
    Path sharedDir = PROFILES_DIR.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    TableMetadata processes = bundle.getSchema().getTableMetadata("Processes");
    assertNotNull(processes);

    Column idCol = processes.getColumn("id");
    assertNotNull(idCol);
    assertNotNull(idCol.getProfiles(), "id column should have subsets");
    assertTrue(
        List.of(idCol.getProfiles()).contains("patient_registry"),
        "id column should be in patient_registry subset");
  }

  @Test
  void validationErrorDuplicateColumnName() {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            columns:
              - name: foo
                type: string
              - section: Section1
                columns:
                  - name: foo
                    type: int
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(ex.getMessage().contains("foo"), "Error must name the duplicate column 'foo'");
  }

  @Test
  void validationErrorReservedNameColumns() {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            columns:
              - name: columns
                type: string
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().toLowerCase().contains("columns"),
        "Error must mention reserved name 'columns'");
  }

  @Test
  void validationErrorNestingDepthEnforced() throws IOException {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            columns:
              - section: Section1
                columns:
                  - heading: Heading1
                    columns:
                      - name: leaf
                        type: string
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    TableMetadata table = bundle.getSchema().getTableMetadata("MyTable");
    assertNotNull(table, "MyTable must be loaded");
    assertNotNull(table.getColumn("leaf"), "leaf column in heading must be loaded");
  }

  @Test
  void validationErrorSemanticsAllowedOnDataColumns() throws IOException {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            columns:
              - section: MySection
                columns:
                  - name: leaf
                    type: string
                    semantics: ['http://example.com/thing']
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    TableMetadata table = bundle.getSchema().getTableMetadata("MyTable");
    Column leaf = table.getColumn("leaf");
    assertNotNull(leaf);
    assertNotNull(leaf.getSemantics(), "semantics must be set on data column");
    assertTrue(
        List.of(leaf.getSemantics()).contains("http://example.com/thing"),
        "semantics value must match");
  }

  @Test
  void attributeHoistingSubsetsFromSection() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: my_subset
            description: section-level subset
            internal: true
          - name: other_subset
            description: column-level override subset
            internal: true
        tables:
          MyTable:
            columns:
              - section: MySection
                profiles: [my_subset]
                columns:
                  - name: col1
                    type: string
                  - name: col2
                    type: int
                    profiles: [other_subset]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    TableMetadata table = bundle.getSchema().getTableMetadata("MyTable");
    assertNotNull(table);

    Column col1 = table.getColumn("col1");
    assertNotNull(col1);
    assertNotNull(col1.getProfiles(), "col1 should inherit subset from section");
    assertTrue(
        List.of(col1.getProfiles()).contains("my_subset"),
        "col1 should inherit my_subset from MySection");

    Column col2 = table.getColumn("col2");
    assertNotNull(col2);
    assertFalse(
        List.of(col2.getProfiles()).contains("my_subset"),
        "col2 overrides subsets, should not have my_subset");
    assertTrue(
        List.of(col2.getProfiles()).contains("other_subset"),
        "col2 should have its own other_subset");
  }

  @Test
  void attributeHoistingSubtypeFromHeading() throws IOException {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            variants:
              - name: ChildA
                description: first child
            columns:
              - section: MySection
                columns:
                  - heading: MyHeading
                    variant: ChildA
                    columns:
                      - name: col1
                        type: string
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    SchemaMetadata schema = bundle.getSchema();
    assertNotNull(
        schema.getTableMetadata("ChildA"), "ChildA subtype must be materialized as a table");
    TableMetadata childA = schema.getTableMetadata("ChildA");
    assertNotNull(
        childA.getColumn("col1"),
        "col1 under MyHeading (subtype: ChildA) must be placed on ChildA");
  }

  @Test
  void subtypeInheritsParentTable() throws IOException {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            variants:
              - name: ChildA
                description: extends parent
              - name: ChildB
                extends: [ChildA]
                description: multi extend
            columns:
              - name: id
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    SchemaMetadata schema = bundle.getSchema();
    TableMetadata childA = schema.getTableMetadata("ChildA");
    assertNotNull(childA);
    assertTrue(List.of(childA.getExtendNames()).contains("MyTable"), "ChildA must inherit MyTable");

    TableMetadata childB = schema.getTableMetadata("ChildB");
    assertNotNull(childB);
    assertTrue(List.of(childB.getExtendNames()).contains("ChildA"), "ChildB must inherit ChildA");
  }

  @Test
  void singleFileBundleRejectsForbiddenKeys() {
    String yaml =
        """
        name: Test
        tables:
          T:
            columns:
              - name: id
                key: 1
        ontologies:
          - SomeOntology
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().toLowerCase().contains("ontologies")
            || ex.getMessage().toLowerCase().contains("single-file"),
        "Error must mention the forbidden key or single-file restriction");
  }

  @Test
  void validationErrorInvalidSubsetIdentifierFormat() {
    String yaml =
        """
        name: Test
        profiles:
          - name: BadName
            description: starts with uppercase
            internal: true
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("BadName"), "Error must name the offending identifier 'BadName'");
  }

  @Test
  void validationErrorInvalidTemplateIdentifierFormat() {
    String yaml =
        """
        name: Test
        profiles:
          - name: 123bad
            description: starts with digit
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("123bad"), "Error must name the offending identifier '123bad'");
  }

  @Test
  void internalFlagRoutesTemplateToSubsetRegistry() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: internal_one
            description: goes to subsetRegistry
            internal: true
          - name: user_facing
            description: goes to profileRegistry
            includes: [internal_one]
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(
        bundle.getProfileRegistry().get("internal_one"),
        "internal:true template must appear in subsetRegistry");
    assertNotNull(
        bundle.getTemplateRegistry().get("user_facing"),
        "non-internal template must appear in templateRegistry");
    assertNull(
        bundle.getProfileRegistry().get("user_facing"),
        "non-internal template must not appear in profileRegistry");
  }

  @Test
  void unifiedTemplatesAllowInternalAndUserFacingEntries() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: core
            description: core subset
            internal: true
          - name: extended
            description: user-facing template
            includes: [core]
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getProfileRegistry().get("core"));
    assertNotNull(bundle.getTemplateRegistry().get("extended"));
  }

  @Test
  void validationErrorIncludesUnresolved() {
    String yaml =
        """
        name: Test
        profiles:
          - name: my_subset
            includes: [nonexistent_subset]
            internal: true
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("nonexistent_subset"),
        "Error must name the unresolved include 'nonexistent_subset'");
    assertTrue(ex.getMessage().contains("my_subset"), "Error must name the parent 'my_subset'");
  }

  @Test
  void validIncludesResolution() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: base
            description: base subset
            internal: true
          - name: extended
            includes: [base]
            internal: true
          - name: full
            includes: [extended, base]
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getProfileRegistry().get("extended"));
    assertTrue(
        bundle.getProfileRegistry().get("extended").getIncludes().contains("base"),
        "extended must include base");
  }

  @Test
  void validationErrorIncludesCycle() {
    String yaml =
        """
        name: Test
        profiles:
          - name: alpha
            includes: [beta]
            internal: true
          - name: beta
            includes: [alpha]
            internal: true
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("alpha") || ex.getMessage().contains("beta"),
        "Error must name at least one node in the cycle");
  }

  @Test
  void validationErrorIncludesCycleThreeNodes() {
    String yaml =
        """
        name: Test
        profiles:
          - name: aaa
            includes: [bbb]
            internal: true
          - name: bbb
            includes: [ccc]
            internal: true
          - name: ccc
            includes: [aaa]
            internal: true
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().toLowerCase().contains("cycle")
            || ex.getMessage().toLowerCase().contains("circular"),
        "Error must mention cycle/circular");
  }

  @Test
  void validAcyclicIncludes() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: base
            description: base
            internal: true
          - name: mid
            includes: [base]
            internal: true
          - name: top
            includes: [mid, base]
            internal: true
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getProfileRegistry().get("top"));
  }

  @Test
  void validationErrorUnknownSubsetOnColumn() {
    String yaml =
        """
        name: Test
        profiles:
          - name: known_subset
            description: this one exists
            internal: true
        tables:
          MyTable:
            columns:
              - name: col1
                type: string
                profiles: [known_subset, ghost_subset]
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("ghost_subset"),
        "Error must name the unknown subset 'ghost_subset'");
  }

  @Test
  void validationErrorUnknownSubsetOnTable() {
    String yaml =
        """
        name: Test
        profiles:
          - name: real_subset
            description: this one exists
            internal: true
        tables:
          MyTable:
            profiles: [phantom_subset]
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("phantom_subset"),
        "Error must name the unknown subset 'phantom_subset'");
  }

  @Test
  void validKnownSubsetsOnColumnAndTable() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: feature_x
            description: a real subset
            internal: true
        tables:
          MyTable:
            profiles: [feature_x]
            columns:
              - name: col1
                type: string
                profiles: [feature_x]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getSchema().getTableMetadata("MyTable"));
    assertNotNull(bundle.getSchema().getTableMetadata("MyTable").getColumn("col1"));
  }

  @Test
  void validationErrorRefTableSubsetNotCompatible() {
    String yaml =
        """
        name: Test
        profiles:
          - name: subset_a
            description: subset A
            internal: true
          - name: subset_b
            description: subset B - not related to A
            internal: true
        tables:
          TableA:
            profiles: [subset_a]
            columns:
              - name: id
                key: 1
              - name: ref_col
                type: ref
                refTable: TableB
          TableB:
            profiles: [subset_b]
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("TableA") || ex.getMessage().contains("TableB"),
        "Error must name the tables involved in the incomplete reference");
  }

  @Test
  void validRefTableAlwaysOn() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: subset_a
            description: subset A
            internal: true
        tables:
          TableA:
            profiles: [subset_a]
            columns:
              - name: id
                key: 1
              - name: ref_col
                type: ref
                refTable: Lookup
          Lookup:
            columns:
              - name: id
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getSchema().getTableMetadata("TableA"));
    assertNotNull(bundle.getSchema().getTableMetadata("Lookup"));
  }

  @Test
  void validRefTableSubsetCompatibleViaIncludes() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: base
            description: base
            internal: true
          - name: extended
            includes: [base]
            internal: true
        tables:
          TableA:
            profiles: [extended]
            columns:
              - name: id
                key: 1
              - name: ref_col
                type: ref
                refTable: TableB
          TableB:
            profiles: [base]
            columns:
              - name: id
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getSchema().getTableMetadata("TableA"));
    assertNotNull(bundle.getSchema().getTableMetadata("TableB"));
  }

  @Test
  void serializerDedupsColumnProfilesMatchingTable() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: profile_a
            description: A
            internal: true
        tables:
          MyTable:
            profiles: [profile_a]
            columns:
              - name: col1
                type: string
                profiles: [profile_a]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    SchemaMetadata schema = bundle.getSchema();

    Emx2Yaml.toBundleSingleFile(
        schema,
        bundle.getName(),
        bundle.getDescription(),
        java.nio.file.Path.of("/tmp/test-dedup-table.yaml"),
        bundle.getBundle().profiles());
    String serialized =
        java.nio.file.Files.readString(java.nio.file.Path.of("/tmp/test-dedup-table.yaml"));
    assertFalse(
        serialized.contains("col1") && countProfilesOccurrences(serialized, "col1") > 0,
        "col1 profiles should be deduped when matching table profiles");

    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper(
            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> doc = mapper.readValue(serialized, java.util.Map.class);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> tables = (java.util.Map<String, Object>) doc.get("tables");
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> myTable = (java.util.Map<String, Object>) tables.get("MyTable");
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> columns =
        (java.util.List<java.util.Map<String, Object>>) myTable.get("columns");
    java.util.Map<String, Object> col1 =
        columns.stream().filter(c -> "col1".equals(c.get("name"))).findFirst().orElseThrow();
    assertNull(
        col1.get("profiles"), "col1 profiles should be omitted when matching table profiles");
  }

  private int countProfilesOccurrences(String yaml, String columnName) {
    int count = 0;
    int idx = yaml.indexOf("name: " + columnName);
    if (idx < 0) return 0;
    int end = yaml.indexOf("- name:", idx + 1);
    if (end < 0) end = yaml.length();
    String section = yaml.substring(idx, end);
    idx = 0;
    while ((idx = section.indexOf("profiles:", idx)) >= 0) {
      count++;
      idx++;
    }
    return count;
  }

  @Test
  void serializerDedupsColumnProfilesMatchingSection() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: pa
            description: A
            internal: true
          - name: pb
            description: B
            internal: true
        tables:
          MyTable:
            columns:
              - section: MySection
                profiles: [pa, pb]
                columns:
                  - name: col1
                    type: string
                    profiles: [pa, pb]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    SchemaMetadata schema = bundle.getSchema();

    java.nio.file.Path outFile = java.nio.file.Path.of("/tmp/test-dedup-section.yaml");
    Emx2Yaml.toBundleSingleFile(
        schema, bundle.getName(), bundle.getDescription(), outFile, bundle.getBundle().profiles());
    String serialized = java.nio.file.Files.readString(outFile);

    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper(
            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> doc = mapper.readValue(serialized, java.util.Map.class);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> tables = (java.util.Map<String, Object>) doc.get("tables");
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> myTable = (java.util.Map<String, Object>) tables.get("MyTable");
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> topCols =
        (java.util.List<java.util.Map<String, Object>>) myTable.get("columns");
    java.util.Map<String, Object> section =
        topCols.stream()
            .filter(c -> "MySection".equals(c.get("section")))
            .findFirst()
            .orElseThrow();
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> sectionCols =
        (java.util.List<java.util.Map<String, Object>>) section.get("columns");
    java.util.Map<String, Object> col1 =
        sectionCols.stream().filter(c -> "col1".equals(c.get("name"))).findFirst().orElseThrow();
    assertNull(
        col1.get("profiles"), "col1 profiles should be omitted when matching section profiles");
  }

  @Test
  void serializerKeepsColumnProfilesWhenDifferent() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: pa
            description: A
            internal: true
          - name: pb
            description: B
            internal: true
        tables:
          MyTable:
            columns:
              - section: MySection
                profiles: [pa]
                columns:
                  - name: col1
                    type: string
                    profiles: [pa, pb]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    SchemaMetadata schema = bundle.getSchema();

    java.nio.file.Path outFile = java.nio.file.Path.of("/tmp/test-dedup-keep.yaml");
    Emx2Yaml.toBundleSingleFile(
        schema, bundle.getName(), bundle.getDescription(), outFile, bundle.getBundle().profiles());
    String serialized = java.nio.file.Files.readString(outFile);

    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper(
            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> doc = mapper.readValue(serialized, java.util.Map.class);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> tables = (java.util.Map<String, Object>) doc.get("tables");
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> myTable = (java.util.Map<String, Object>) tables.get("MyTable");
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> topCols =
        (java.util.List<java.util.Map<String, Object>>) myTable.get("columns");
    java.util.Map<String, Object> section =
        topCols.stream()
            .filter(c -> "MySection".equals(c.get("section")))
            .findFirst()
            .orElseThrow();
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> sectionCols =
        (java.util.List<java.util.Map<String, Object>>) section.get("columns");
    java.util.Map<String, Object> col1 =
        sectionCols.stream().filter(c -> "col1".equals(c.get("name"))).findFirst().orElseThrow();
    assertNotNull(col1.get("profiles"), "col1 profiles should be kept when different from section");
    @SuppressWarnings("unchecked")
    java.util.List<String> col1Profiles = (java.util.List<String>) col1.get("profiles");
    assertTrue(
        col1Profiles.contains("pa") && col1Profiles.contains("pb"),
        "col1 should have both pa and pb profiles");
  }

  @Test
  void serializerLiftsCommonChildProfilesToSection() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: pa
            description: A
            internal: true
        tables:
          MyTable:
            columns:
              - section: MySection
                columns:
                  - name: col1
                    type: string
                    profiles: [pa]
                  - name: col2
                    type: int
                    profiles: [pa]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    SchemaMetadata schema = bundle.getSchema();

    java.nio.file.Path outFile = java.nio.file.Path.of("/tmp/test-dedup-lift.yaml");
    Emx2Yaml.toBundleSingleFile(
        schema, bundle.getName(), bundle.getDescription(), outFile, bundle.getBundle().profiles());
    String serialized = java.nio.file.Files.readString(outFile);

    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper(
            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> doc = mapper.readValue(serialized, java.util.Map.class);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> tables = (java.util.Map<String, Object>) doc.get("tables");
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> myTable = (java.util.Map<String, Object>) tables.get("MyTable");
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> topCols =
        (java.util.List<java.util.Map<String, Object>>) myTable.get("columns");
    java.util.Map<String, Object> section =
        topCols.stream()
            .filter(c -> "MySection".equals(c.get("section")))
            .findFirst()
            .orElseThrow();
    assertNotNull(section.get("profiles"), "section should have profiles lifted from children");
    @SuppressWarnings("unchecked")
    java.util.List<String> sectionProfiles = (java.util.List<String>) section.get("profiles");
    assertTrue(sectionProfiles.contains("pa"), "section should have pa lifted from children");
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> sectionCols =
        (java.util.List<java.util.Map<String, Object>>) section.get("columns");
    for (java.util.Map<String, Object> col : sectionCols) {
      assertNull(col.get("profiles"), "child profiles should be omitted after lifting to section");
    }
  }

  @Test
  void roundtripAfterDedupProducesIdenticalMetadata() throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: pa
            description: A
            internal: true
          - name: pb
            description: B
            internal: true
        tables:
          MyTable:
            profiles: [pa, pb]
            columns:
              - name: col1
                type: string
                profiles: [pa, pb]
              - section: MySection
                profiles: [pa, pb]
                columns:
                  - name: col2
                    type: int
                    profiles: [pa, pb]
                  - name: col3
                    type: string
                    profiles: [pa]
        """;
    Emx2Yaml.BundleResult original =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    SchemaMetadata originalSchema = original.getSchema();

    java.nio.file.Path outFile = java.nio.file.Path.of("/tmp/test-dedup-roundtrip.yaml");
    Emx2Yaml.toBundleSingleFile(
        originalSchema,
        original.getName(),
        original.getDescription(),
        outFile,
        original.getBundle().profiles());

    Emx2Yaml.BundleResult roundtrip = Emx2Yaml.fromBundle(outFile);
    SchemaMetadata roundtripSchema = roundtrip.getSchema();

    TableMetadata originalTable = originalSchema.getTableMetadata("MyTable");
    TableMetadata roundtripTable = roundtripSchema.getTableMetadata("MyTable");
    assertNotNull(roundtripTable, "MyTable must survive roundtrip");

    for (Column originalCol : originalTable.getNonInheritedColumns()) {
      if (originalCol.isSystemColumn()) continue;
      Column roundtripCol = roundtripTable.getColumn(originalCol.getName());
      assertNotNull(roundtripCol, "Column '" + originalCol.getName() + "' must survive roundtrip");
      String[] origProfiles = originalCol.getProfiles();
      String[] rtProfiles = roundtripCol.getProfiles();
      java.util.Set<String> origSet =
          origProfiles != null
              ? new java.util.HashSet<>(java.util.Arrays.asList(origProfiles))
              : java.util.Set.of();
      java.util.Set<String> rtSet =
          rtProfiles != null
              ? new java.util.HashSet<>(java.util.Arrays.asList(rtProfiles))
              : java.util.Set.of();
      assertEquals(
          origSet,
          rtSet,
          "Profiles for column '" + originalCol.getName() + "' must match after dedup roundtrip");
    }
  }

  @Test
  void sharedBundlePassesAllValidationRules() throws IOException {
    Path sharedDir = PROFILES_DIR.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);
    assertNotNull(bundle.getName());
    assertFalse(bundle.getProfileRegistry().isEmpty());
    assertFalse(bundle.getTemplateRegistry().isEmpty());
  }

  @Test
  void petstoreBundlePassesAllValidationRules() throws IOException {
    Path petstoreYaml = PROFILES_DIR.resolve("petstore.yaml");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(petstoreYaml);
    assertNotNull(bundle.getName());
  }

  @Test
  void importsAtBundleRootExpand(@TempDir Path tempDir) throws IOException {
    Path tablesDir = tempDir.resolve("tables");
    Files.createDirectories(tablesDir);

    Files.writeString(
        tablesDir.resolve("Alpha.yaml"),
        """
        table: Alpha
        columns:
          - name: id
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("molgenis.yaml"),
        """
        name: test_bundle
        imports:
        - tables/
        """);

    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir);
    assertNotNull(bundle.getSchema().getTableMetadata("Alpha"), "Alpha must be loaded via imports");
  }

  @Test
  void importsInsideTablesMap(@TempDir Path tempDir) throws IOException {
    Files.writeString(
        tempDir.resolve("extra.yaml"),
        """
        table: Imported
        columns:
          - name: id
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          Inline:
            columns:
              - name: id
                key: 1
          imports:
          - extra.yaml
        """);

    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml"));
    assertNotNull(
        bundle.getSchema().getTableMetadata("Imported"),
        "Imported table must be loaded from imports inside tables map");
    assertNotNull(
        bundle.getSchema().getTableMetadata("Inline"), "Inline table must still be present");
  }

  @Test
  void importsNestedRecursion(@TempDir Path tempDir) throws IOException {
    Files.writeString(
        tempDir.resolve("c.yaml"),
        """
        table: TableC
        columns:
          - name: id
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("b.yaml"),
        """
        table: TableB
        imports:
        - c.yaml
        columns:
          - name: id
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          imports:
          - b.yaml
        """);

    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml"));
    assertNotNull(bundle.getSchema().getTableMetadata("TableB"), "TableB must be loaded");
    assertNotNull(
        bundle.getSchema().getTableMetadata("TableC"), "TableC must be loaded via b.yaml");
  }

  @Test
  void importsCycleDetected(@TempDir Path tempDir) throws IOException {
    Path fileA = tempDir.resolve("a.yaml");
    Path fileB = tempDir.resolve("b.yaml");

    Files.writeString(
        fileA,
        """
        table: TableA
        imports:
        - b.yaml
        columns:
          - name: id
            key: 1
        """);

    Files.writeString(
        fileB,
        """
        table: TableB
        imports:
        - a.yaml
        columns:
          - name: id
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          imports:
          - a.yaml
        """);

    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml")));
    assertTrue(
        ex.getMessage().toLowerCase().contains("cycle")
            || ex.getMessage().toLowerCase().contains("import"),
        "Error must mention cycle or import: " + ex.getMessage());
  }

  @Test
  void importsKeyCollisionError(@TempDir Path tempDir) throws IOException {
    Files.writeString(
        tempDir.resolve("first.yaml"),
        """
        table: SameName
        columns:
          - name: id
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("second.yaml"),
        """
        table: SameName
        columns:
          - name: label
            type: string
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          imports:
          - first.yaml
          - second.yaml
        """);

    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml")));
    assertTrue(
        ex.getMessage().contains("SameName"),
        "Error must name the colliding table 'SameName': " + ex.getMessage());
  }

  @Test
  void importsFileNotFound(@TempDir Path tempDir) throws IOException {
    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          imports:
          - nonexistent.yaml
        """);

    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml")));
    assertTrue(
        ex.getMessage().contains("nonexistent.yaml") || ex.getMessage().contains("not found"),
        "Error must mention the missing path: " + ex.getMessage());
  }

  @Test
  void normalizeSubsetNameLowercase() {
    assertEquals("petstore", ProfileNameNormalizer.normalize("Petstore"));
  }

  @Test
  void normalizeSubsetNameReplacesSpaces() {
    assertEquals("data_catalogue", ProfileNameNormalizer.normalize("Data Catalogue"));
  }

  @Test
  void normalizeSubsetNameCollapsesUnderscores() {
    assertEquals("data_catalogue", ProfileNameNormalizer.normalize("Data  Catalogue"));
  }

  @Test
  void normalizeSubsetNameStripsPunctuation() {
    assertEquals("fair_genomes", ProfileNameNormalizer.normalize("FAIR-Genomes!"));
  }

  @Test
  void normalizeSubsetNameLeadingDigit() {
    assertEquals("s_3prime", ProfileNameNormalizer.normalize("3prime"));
  }

  @Test
  void variantColumnTypeMapping() throws IOException {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            variants:
              - name: ChildA
                description: child
            columns:
              - name: id
                key: 1
              - name: kind
                type: variant
              - name: kinds
                type: variant_array
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    TableMetadata table = bundle.getSchema().getTableMetadata("MyTable");
    assertNotNull(table);

    Column kind = table.getColumn("kind");
    assertNotNull(kind);
    assertEquals(
        ColumnType.VARIANT, kind.getColumnType(), "type: variant must map to ColumnType.VARIANT");

    Column kinds = table.getColumn("kinds");
    assertNotNull(kinds);
    assertEquals(
        ColumnType.VARIANT_ARRAY,
        kinds.getColumnType(),
        "type: variant_array must map to ColumnType.VARIANT_ARRAY");
  }

  @Test
  void importsRejectedInInputStream() {
    String yaml =
        """
        name: Test
        imports: [foo.yaml]
        tables:
          T:
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("imports:"),
        "Error must mention the imports: key: " + ex.getMessage());
  }

  @Test
  void importsRejectedInInputStreamNested() {
    String yaml =
        """
        name: Test
        tables:
          T:
            imports: [extra.yaml]
            columns:
              - name: id
                key: 1
        """;
    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                Emx2Yaml.fromBundle(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    assertTrue(
        ex.getMessage().contains("imports:"),
        "Error must mention the imports: key: " + ex.getMessage());
  }

  @Test
  void importsAbsolutePathRejected(@TempDir Path tempDir) throws IOException {
    Path tableFile = tempDir.resolve("table.yaml");
    Files.writeString(tableFile, "table: T\ncolumns:\n  - name: id\n    key: 1\n");
    Path molgenisYaml = tempDir.resolve("molgenis.yaml");
    Files.writeString(molgenisYaml, "name: Test\nimports: [/etc/passwd]\n");
    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundle(tempDir));
    assertTrue(
        ex.getMessage().contains("/etc/passwd")
            || ex.getMessage().toLowerCase().contains("absolute"),
        "Error must mention the absolute path: " + ex.getMessage());
  }

  @Test
  void importsParentTraversalRejected(@TempDir Path tempDir) throws IOException {
    Path molgenisYaml = tempDir.resolve("molgenis.yaml");
    Files.writeString(molgenisYaml, "name: Test\nimports: [../../etc/passwd]\n");
    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundle(tempDir));
    assertTrue(
        ex.getMessage().contains("../../etc/passwd")
            || ex.getMessage().toLowerCase().contains("escape"),
        "Error must mention the traversal path: " + ex.getMessage());
  }

  @Test
  void importsCycleIncludesInitiator(@TempDir Path tempDir) throws IOException {
    Path aYaml = tempDir.resolve("a.yaml");
    Path bYaml = tempDir.resolve("b.yaml");
    Files.writeString(aYaml, "table: A\nimports: [b.yaml]\n");
    Files.writeString(bYaml, "table: B\nimports: [a.yaml]\n");
    Path molgenisYaml = tempDir.resolve("molgenis.yaml");
    Files.writeString(molgenisYaml, "name: Test\nimports: [a.yaml]\n");
    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundle(tempDir));
    String msg = ex.getMessage();
    assertTrue(
        msg.contains("a.yaml") && msg.contains("b.yaml"),
        "Cycle error must mention all files in the cycle: " + msg);
    int aIdx = msg.indexOf("a.yaml");
    int bIdx = msg.indexOf("b.yaml");
    int aLastIdx = msg.lastIndexOf("a.yaml");
    assertTrue(
        aLastIdx > bIdx, "a.yaml should appear both before and after b.yaml in cycle: " + msg);
  }

  @Test
  void importsInsideColumnsList(@TempDir Path tempDir) throws IOException {
    Path colsDir = tempDir.resolve("columns");
    Files.createDirectories(colsDir);

    Files.writeString(
        colsDir.resolve("demographics.yaml"),
        """
        columns:
          - name: year of birth
            type: int
          - name: country of birth
            type: ontology
            refTable: Countries
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          Patient:
            columns:
              - name: id
                key: 1
              - import: columns/demographics.yaml
              - name: status
        """);

    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml"));
    TableMetadata patient = bundle.getSchema().getTableMetadata("Patient");
    assertNotNull(patient, "Patient table must be loaded");
    assertNotNull(patient.getColumn("id"), "id column must be present");
    assertNotNull(
        patient.getColumn("year of birth"),
        "imported column 'year of birth' must expand at position");
    assertNotNull(
        patient.getColumn("country of birth"),
        "imported column 'country of birth' must be present");
    assertNotNull(patient.getColumn("status"), "status column after import must be present");
  }

  @Test
  void importsAtMultiplePositionsInColumns(@TempDir Path tempDir) throws IOException {
    Path colsDir = tempDir.resolve("columns");
    Files.createDirectories(colsDir);

    Files.writeString(
        colsDir.resolve("part1.yaml"),
        """
        columns:
          - name: field_a
            type: string
        """);

    Files.writeString(
        colsDir.resolve("part2.yaml"),
        """
        columns:
          - name: field_b
            type: int
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          T:
            columns:
              - name: id
                key: 1
              - import: columns/part1.yaml
              - name: mid
              - import: columns/part2.yaml
              - name: last
        """);

    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml"));
    TableMetadata table = bundle.getSchema().getTableMetadata("T");
    assertNotNull(table.getColumn("field_a"), "field_a from first import must be present");
    assertNotNull(table.getColumn("field_b"), "field_b from second import must be present");
    assertNotNull(table.getColumn("mid"), "mid inline column must be present");
    assertNotNull(table.getColumn("last"), "last inline column must be present");
  }

  @Test
  void importSectionIntoColumnsList(@TempDir Path tempDir) throws IOException {
    Path colsDir = tempDir.resolve("columns");
    Files.createDirectories(colsDir);

    Files.writeString(
        colsDir.resolve("consent_section.yaml"),
        """
        section: Consent
        columns:
          - name: consent given
            type: bool
          - name: consent date
            type: date
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          Subject:
            columns:
              - name: id
                key: 1
              - import: columns/consent_section.yaml
        """);

    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml"));
    TableMetadata subject = bundle.getSchema().getTableMetadata("Subject");
    assertNotNull(subject, "Subject table must be loaded");
    assertNotNull(
        subject.getColumn("consent given"), "consent given from imported section must be present");
    assertNotNull(
        subject.getColumn("consent date"), "consent date from imported section must be present");
  }

  @Test
  void importColumnCollisionDetected(@TempDir Path tempDir) throws IOException {
    Path colsDir = tempDir.resolve("columns");
    Files.createDirectories(colsDir);

    Files.writeString(
        colsDir.resolve("shared.yaml"),
        """
        columns:
          - name: status
            type: string
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          T:
            columns:
              - name: id
                key: 1
              - import: columns/shared.yaml
              - name: status
                type: int
        """);

    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> Emx2Yaml.fromBundle(tempDir.resolve("bundle.yaml")));
    assertTrue(
        ex.getMessage().contains("status"),
        "Error must name the duplicate column 'status': " + ex.getMessage());
  }

  @Test
  void serializerDedupsIncludedProfileFromColumn(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: base
            description: base
            internal: true
          - name: extended
            includes: [base]
            internal: true
        tables:
          MyTable:
            columns:
              - name: col1
                type: string
                profiles: [base, extended]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Path outFile = tempDir.resolve("dedup-include.yaml");
    Emx2Yaml.toBundleSingleFile(
        bundle.getSchema(),
        bundle.getName(),
        bundle.getDescription(),
        outFile,
        bundle.getBundle().profiles());

    Emx2Yaml.BundleResult roundtrip = Emx2Yaml.fromBundle(outFile);
    Column col1 = roundtrip.getSchema().getTableMetadata("MyTable").getColumn("col1");
    assertNotNull(col1);
    List<String> profiles = col1.getProfiles() != null ? List.of(col1.getProfiles()) : List.of();
    assertTrue(profiles.contains("base"), "base profile must be kept");
    assertFalse(
        profiles.contains("extended"),
        "extended must be dropped because extended includes base, so extended tag is redundant");
  }

  @Test
  void serializerDedupsTransitivelyIncludedProfile(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: base
            description: base
            internal: true
          - name: mid
            includes: [base]
            internal: true
          - name: top
            includes: [mid]
            internal: true
        tables:
          MyTable:
            columns:
              - name: col1
                type: string
                profiles: [base, mid, top]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Path outFile = tempDir.resolve("dedup-transitive.yaml");
    Emx2Yaml.toBundleSingleFile(
        bundle.getSchema(),
        bundle.getName(),
        bundle.getDescription(),
        outFile,
        bundle.getBundle().profiles());

    Emx2Yaml.BundleResult roundtrip = Emx2Yaml.fromBundle(outFile);
    Column col1 = roundtrip.getSchema().getTableMetadata("MyTable").getColumn("col1");
    assertNotNull(col1);
    List<String> profiles = col1.getProfiles() != null ? List.of(col1.getProfiles()) : List.of();
    assertTrue(profiles.contains("base"), "base profile must be kept");
    assertFalse(profiles.contains("mid"), "mid must be dropped: mid transitively includes base");
    assertFalse(profiles.contains("top"), "top must be dropped: top transitively includes base");
  }

  @Test
  void serializerKeepsProfileNotInIncludeChain(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: alpha
            description: alpha
            internal: true
          - name: beta
            description: beta - unrelated to alpha
            internal: true
        tables:
          MyTable:
            columns:
              - name: col1
                type: string
                profiles: [alpha, beta]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Path outFile = tempDir.resolve("dedup-unrelated.yaml");
    Emx2Yaml.toBundleSingleFile(
        bundle.getSchema(),
        bundle.getName(),
        bundle.getDescription(),
        outFile,
        bundle.getBundle().profiles());

    Emx2Yaml.BundleResult roundtrip = Emx2Yaml.fromBundle(outFile);
    Column col1 = roundtrip.getSchema().getTableMetadata("MyTable").getColumn("col1");
    assertNotNull(col1);
    List<String> profiles = col1.getProfiles() != null ? List.of(col1.getProfiles()) : List.of();
    assertTrue(profiles.contains("alpha"), "alpha must be kept: unrelated to beta");
    assertTrue(profiles.contains("beta"), "beta must be kept: unrelated to alpha");
  }

  @Test
  void serializerDedupsIncludeProfileOnSection(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: base
            description: base
            internal: true
          - name: derived
            includes: [base]
            internal: true
        tables:
          MyTable:
            columns:
              - section: MySection
                profiles: [base, derived]
                columns:
                  - name: col1
                    type: string
                    profiles: [base, derived]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Path outFile = tempDir.resolve("dedup-section.yaml");
    Emx2Yaml.toBundleSingleFile(
        bundle.getSchema(),
        bundle.getName(),
        bundle.getDescription(),
        outFile,
        bundle.getBundle().profiles());

    Emx2Yaml.BundleResult roundtrip = Emx2Yaml.fromBundle(outFile);
    Column col1 = roundtrip.getSchema().getTableMetadata("MyTable").getColumn("col1");
    assertNotNull(col1);
    List<String> profiles = col1.getProfiles() != null ? List.of(col1.getProfiles()) : List.of();
    assertTrue(profiles.contains("base"), "base must be kept");
    assertFalse(profiles.contains("derived"), "derived must be dropped: derived includes base");
  }

  @Test
  void roundtripAfterIncludeDedupLossless(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
        name: Test
        profiles:
          - name: base
            description: base
            internal: true
          - name: extended
            includes: [base]
            internal: true
        tables:
          MyTable:
            columns:
              - name: col1
                type: string
                profiles: [base, extended]
              - name: col2
                type: int
                profiles: [base]
        """;
    Emx2Yaml.BundleResult original =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Path outFile = tempDir.resolve("dedup-roundtrip.yaml");
    Emx2Yaml.toBundleSingleFile(
        original.getSchema(),
        original.getName(),
        original.getDescription(),
        outFile,
        original.getBundle().profiles());

    Emx2Yaml.BundleResult roundtrip = Emx2Yaml.fromBundle(outFile);
    SchemaMetadata schema = roundtrip.getSchema();

    Column col1 = schema.getTableMetadata("MyTable").getColumn("col1");
    assertNotNull(col1);
    List<String> col1Profiles =
        col1.getProfiles() != null ? List.of(col1.getProfiles()) : List.of();
    assertTrue(col1Profiles.contains("base"), "col1 must have base after dedup+roundtrip");

    Column col2 = schema.getTableMetadata("MyTable").getColumn("col2");
    assertNotNull(col2);
    List<String> col2Profiles =
        col2.getProfiles() != null ? List.of(col2.getProfiles()) : List.of();
    assertTrue(col2Profiles.contains("base"), "col2 base must survive roundtrip");
  }
}
