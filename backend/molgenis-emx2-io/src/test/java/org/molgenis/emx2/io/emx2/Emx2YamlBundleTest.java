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

  private static final Path DATA_TEMPLATES = findDataTemplatesPath();

  private static Path findDataTemplatesPath() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("data/templates");
      if (candidate.toFile().isDirectory()) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Could not find data/templates directory from: " + Path.of("").toAbsolutePath());
  }

  @Test
  void loadPetstoreSingleFileBundle() throws IOException {
    Path petstoreYaml = DATA_TEMPLATES.resolve("petstore.yaml");
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
    Path petstoreYaml = DATA_TEMPLATES.resolve("petstore.yaml");
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
    Path petstoreYaml = DATA_TEMPLATES.resolve("petstore.yaml");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(petstoreYaml);

    TableMetadata user = bundle.getSchema().getTableMetadata("User");
    assertNotNull(user);

    Column streetCol = user.getColumn("street");
    assertNotNull(streetCol, "Column 'street' from section 'address' should be present");
  }

  @Test
  void loadSharedDirectoryBundle() throws IOException {
    Path sharedDir = DATA_TEMPLATES.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    assertEquals("MOLGENIS shared catalogue bundle", bundle.getName());
    assertNotNull(bundle.getDescription());

    assertNotNull(bundle.getSubsetRegistry(), "subsetRegistry must not be null");
    assertNotNull(bundle.getTemplateRegistry(), "templateRegistry must not be null");

    assertTrue(
        bundle.getSubsetRegistry().containsKey("catalogue_core"),
        "catalogue_core internal template must be in subsetRegistry");
    assertTrue(
        bundle.getSubsetRegistry().containsKey("patient_core"),
        "patient_core internal template must be in subsetRegistry");
    assertTrue(
        bundle.getTemplateRegistry().containsKey("data_catalogue"),
        "data_catalogue template must be in templateRegistry");
    assertTrue(
        bundle.getTemplateRegistry().containsKey("patient_registry"),
        "patient_registry template must be in templateRegistry");
  }

  @Test
  void loadSharedBundleSubsetIncludes() throws IOException {
    Path sharedDir = DATA_TEMPLATES.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    SubsetEntry cohortCore = bundle.getSubsetRegistry().get("cohort_core");
    assertNotNull(cohortCore);
    assertTrue(
        cohortCore.getIncludes().contains("catalogue_core"),
        "cohort_core should include catalogue_core");

    SubsetEntry rweCore = bundle.getSubsetRegistry().get("rwe_core");
    assertNotNull(rweCore);
    assertTrue(
        rweCore.getIncludes().contains("rwe_staging"), "rwe_core should include rwe_staging");
  }

  @Test
  void loadSharedBundleProcessesTableSubtypes() throws IOException {
    Path sharedDir = DATA_TEMPLATES.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    SchemaMetadata schema = bundle.getSchema();
    assertNotNull(schema.getTableMetadata("Processes"), "Processes table must be loaded");
    assertNotNull(schema.getTableMetadata("Analyses"), "Analyses subtype must be loaded");
    assertNotNull(schema.getTableMetadata("Observations"), "Observations subtype must be loaded");
    assertNotNull(
        schema.getTableMetadata("NGS sequencing"), "NGS sequencing subtype must be loaded");

    TableMetadata analyses = schema.getTableMetadata("Analyses");
    assertNotNull(analyses.getInheritNames(), "Analyses must inherit from Processes");
    assertTrue(
        List.of(analyses.getInheritNames()).contains("Processes"),
        "Analyses must inherit Processes");
  }

  @Test
  void loadSharedBundleProcessesTableColumns() throws IOException {
    Path sharedDir = DATA_TEMPLATES.resolve("shared");
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
    Path sharedDir = DATA_TEMPLATES.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);

    TableMetadata processes = bundle.getSchema().getTableMetadata("Processes");
    assertNotNull(processes);

    Column idCol = processes.getColumn("id");
    assertNotNull(idCol);
    assertNotNull(idCol.getSubsets(), "id column should have subsets");
    assertTrue(
        List.of(idCol.getSubsets()).contains("patient_core"),
        "id column should be in patient_core subset");
  }

  @Test
  void validationErrorDuplicateColumnName() {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            columns:
              foo:
                type: string
            sections:
              Section1:
                columns:
                  foo:
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
              columns:
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
            sections:
              Section1:
                headings:
                  Heading1:
                    columns:
                      leaf:
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
            sections:
              MySection:
                columns:
                  leaf:
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
        templates:
          my_subset:
            description: section-level subset
            internal: true
          other_subset:
            description: column-level override subset
            internal: true
        tables:
          MyTable:
            sections:
              MySection:
                templates: [my_subset]
                columns:
                  col1:
                    type: string
                  col2:
                    type: int
                    templates: [other_subset]
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    TableMetadata table = bundle.getSchema().getTableMetadata("MyTable");
    assertNotNull(table);

    Column col1 = table.getColumn("col1");
    assertNotNull(col1);
    assertNotNull(col1.getSubsets(), "col1 should inherit subset from section");
    assertTrue(
        List.of(col1.getSubsets()).contains("my_subset"),
        "col1 should inherit my_subset from MySection");

    Column col2 = table.getColumn("col2");
    assertNotNull(col2);
    assertFalse(
        List.of(col2.getSubsets()).contains("my_subset"),
        "col2 overrides subsets, should not have my_subset");
    assertTrue(
        List.of(col2.getSubsets()).contains("other_subset"),
        "col2 should have its own other_subset");
  }

  @Test
  void attributeHoistingSubtypeFromHeading() throws IOException {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            subtypes:
              ChildA:
                description: first child
            sections:
              MySection:
                headings:
                  MyHeading:
                    subtype: ChildA
                    columns:
                      col1:
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
            subtypes:
              ChildA:
                description: inherits parent
              ChildB:
                inherits: [ChildA]
                description: multi inherit
            columns:
              id:
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    SchemaMetadata schema = bundle.getSchema();
    TableMetadata childA = schema.getTableMetadata("ChildA");
    assertNotNull(childA);
    assertTrue(
        List.of(childA.getInheritNames()).contains("MyTable"), "ChildA must inherit MyTable");

    TableMetadata childB = schema.getTableMetadata("ChildB");
    assertNotNull(childB);
    assertTrue(List.of(childB.getInheritNames()).contains("ChildA"), "ChildB must inherit ChildA");
  }

  @Test
  void singleFileBundleRejectsForbiddenKeys() {
    String yaml =
        """
        name: Test
        tables:
          T:
            columns:
              id:
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
        templates:
          BadName:
            description: starts with uppercase
            internal: true
        tables:
          T:
            columns:
              id:
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
        templates:
          123bad:
            description: starts with digit
        tables:
          T:
            columns:
              id:
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
        templates:
          internal_one:
            description: goes to subsetRegistry
            internal: true
          user_facing:
            description: goes to templateRegistry
            includes: [internal_one]
        tables:
          T:
            columns:
              id:
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(
        bundle.getSubsetRegistry().get("internal_one"),
        "internal:true template must appear in subsetRegistry");
    assertNotNull(
        bundle.getTemplateRegistry().get("user_facing"),
        "non-internal template must appear in templateRegistry");
    assertNull(
        bundle.getSubsetRegistry().get("user_facing"),
        "non-internal template must not appear in subsetRegistry");
  }

  @Test
  void unifiedTemplatesAllowInternalAndUserFacingEntries() throws IOException {
    String yaml =
        """
        name: Test
        templates:
          core:
            description: core subset
            internal: true
          extended:
            description: user-facing template
            includes: [core]
        tables:
          T:
            columns:
              id:
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getSubsetRegistry().get("core"));
    assertNotNull(bundle.getTemplateRegistry().get("extended"));
  }

  @Test
  void validationErrorIncludesUnresolved() {
    String yaml =
        """
        name: Test
        templates:
          my_subset:
            includes: [nonexistent_subset]
            internal: true
        tables:
          T:
            columns:
              id:
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
        templates:
          base:
            description: base subset
            internal: true
          extended:
            includes: [base]
            internal: true
          full:
            includes: [extended, base]
        tables:
          T:
            columns:
              id:
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getSubsetRegistry().get("extended"));
    assertTrue(
        bundle.getSubsetRegistry().get("extended").getIncludes().contains("base"),
        "extended must include base");
  }

  @Test
  void validationErrorIncludesCycle() {
    String yaml =
        """
        name: Test
        templates:
          alpha:
            includes: [beta]
            internal: true
          beta:
            includes: [alpha]
            internal: true
        tables:
          T:
            columns:
              id:
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
        templates:
          aaa:
            includes: [bbb]
            internal: true
          bbb:
            includes: [ccc]
            internal: true
          ccc:
            includes: [aaa]
            internal: true
        tables:
          T:
            columns:
              id:
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
        templates:
          base:
            description: base
            internal: true
          mid:
            includes: [base]
            internal: true
          top:
            includes: [mid, base]
            internal: true
        tables:
          T:
            columns:
              id:
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getSubsetRegistry().get("top"));
  }

  @Test
  void validationErrorUnknownSubsetOnColumn() {
    String yaml =
        """
        name: Test
        templates:
          known_subset:
            description: this one exists
            internal: true
        tables:
          MyTable:
            columns:
              col1:
                type: string
                templates: [known_subset, ghost_subset]
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
        templates:
          real_subset:
            description: this one exists
            internal: true
        tables:
          MyTable:
            templates: [phantom_subset]
            columns:
              id:
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
        templates:
          feature_x:
            description: a real subset
            internal: true
        tables:
          MyTable:
            templates: [feature_x]
            columns:
              col1:
                type: string
                templates: [feature_x]
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
        templates:
          subset_a:
            description: subset A
            internal: true
          subset_b:
            description: subset B - not related to A
            internal: true
        tables:
          TableA:
            templates: [subset_a]
            columns:
              id:
                key: 1
              ref_col:
                type: ref
                refTable: TableB
          TableB:
            templates: [subset_b]
            columns:
              id:
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
        templates:
          subset_a:
            description: subset A
            internal: true
        tables:
          TableA:
            templates: [subset_a]
            columns:
              id:
                key: 1
              ref_col:
                type: ref
                refTable: Lookup
          Lookup:
            columns:
              id:
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
        templates:
          base:
            description: base
            internal: true
          extended:
            includes: [base]
            internal: true
        tables:
          TableA:
            templates: [extended]
            columns:
              id:
                key: 1
              ref_col:
                type: ref
                refTable: TableB
          TableB:
            templates: [base]
            columns:
              id:
                key: 1
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(bundle.getSchema().getTableMetadata("TableA"));
    assertNotNull(bundle.getSchema().getTableMetadata("TableB"));
  }

  @Test
  void sharedBundlePassesAllValidationRules() throws IOException {
    Path sharedDir = DATA_TEMPLATES.resolve("shared");
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(sharedDir);
    assertNotNull(bundle.getName());
    assertFalse(bundle.getSubsetRegistry().isEmpty());
    assertFalse(bundle.getTemplateRegistry().isEmpty());
  }

  @Test
  void petstoreBundlePassesAllValidationRules() throws IOException {
    Path petstoreYaml = DATA_TEMPLATES.resolve("petstore.yaml");
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
          id:
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
          id:
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("bundle.yaml"),
        """
        name: test_bundle
        tables:
          Inline:
            columns:
              id:
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
          id:
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("b.yaml"),
        """
        table: TableB
        imports:
        - c.yaml
        columns:
          id:
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
          id:
            key: 1
        """);

    Files.writeString(
        fileB,
        """
        table: TableB
        imports:
        - a.yaml
        columns:
          id:
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
          id:
            key: 1
        """);

    Files.writeString(
        tempDir.resolve("second.yaml"),
        """
        table: SameName
        columns:
          label:
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
    assertEquals("petstore", TemplateNameNormalizer.normalize("Petstore"));
  }

  @Test
  void normalizeSubsetNameReplacesSpaces() {
    assertEquals("data_catalogue", TemplateNameNormalizer.normalize("Data Catalogue"));
  }

  @Test
  void normalizeSubsetNameCollapsesUnderscores() {
    assertEquals("data_catalogue", TemplateNameNormalizer.normalize("Data  Catalogue"));
  }

  @Test
  void normalizeSubsetNameStripsPunctuation() {
    assertEquals("fair_genomes", TemplateNameNormalizer.normalize("FAIR-Genomes!"));
  }

  @Test
  void normalizeSubsetNameLeadingDigit() {
    assertEquals("s_3prime", TemplateNameNormalizer.normalize("3prime"));
  }

  @Test
  void subtypeColumnTypeMapping() throws IOException {
    String yaml =
        """
        name: Test
        tables:
          MyTable:
            subtypes:
              ChildA:
                description: child
            columns:
              id:
                key: 1
              kind:
                type: subtype
              kinds:
                type: subtype_array
        """;
    Emx2Yaml.BundleResult bundle =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    TableMetadata table = bundle.getSchema().getTableMetadata("MyTable");
    assertNotNull(table);

    Column kind = table.getColumn("kind");
    assertNotNull(kind);
    assertEquals(
        ColumnType.EXTENSION,
        kind.getColumnType(),
        "type: subtype must map to ColumnType.EXTENSION");

    Column kinds = table.getColumn("kinds");
    assertNotNull(kinds);
    assertEquals(
        ColumnType.EXTENSION_ARRAY,
        kinds.getColumnType(),
        "type: subtype_array must map to ColumnType.EXTENSION_ARRAY");
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
              id:
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
              id:
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
    Files.writeString(tableFile, "table: T\ncolumns:\n  id:\n    key: 1\n");
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
}
