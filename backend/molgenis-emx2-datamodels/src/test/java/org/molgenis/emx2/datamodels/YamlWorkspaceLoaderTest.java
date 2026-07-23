package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Row.row;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

class YamlWorkspaceLoaderTest extends TestLoaders {

  private static final String CATALOGUE = "catalogue";
  private static final String RD3 = "rd3";
  private static final String CONTACTS = "Contacts";
  private static final String COLLECTIONS = "Collections";
  private static final String ORGANISATIONS = "Organisations";
  private static final String EMAIL = "email";
  private static final String COMPANION = "CatalogueOntologies";

  private static final String CATALOGUE_NO_DEMO = "wsCatalogueNoDemo";
  private static final String CATALOGUE_DEMO = "wsCatalogueDemo";
  private static final String RD3_SCHEMA = "wsRd3";
  private static final String ROOT_IMPORTS_TEMPLATE = "rootimports/demo";
  private static final String ROOT_IMPORTS_SCHEMA = "wsRootImports";
  private static final String REUSE_TEMPLATE = "reusecompanion/demo";
  private static final String REUSE_MAIN_A = "wsReuseCompanionA";
  private static final String REUSE_MAIN_B = "wsReuseCompanionB";
  private static final String REUSE_COMPANION = "YwlReuseOntologies";
  private static final String COUNTRIES = "Countries";
  private static final String NAME = "name";
  private static final String NETHERLANDS = "Netherlands";

  private static final YamlWorkspaceLoader loader = new YamlWorkspaceLoader();

  @BeforeAll
  void provisionWorkspaces() {
    for (String schemaName : List.of(CATALOGUE_NO_DEMO, CATALOGUE_DEMO, RD3_SCHEMA)) {
      database.dropSchemaIfExists(schemaName);
    }
    loader.create(database, CATALOGUE, CATALOGUE_NO_DEMO, false);
    loader.create(database, CATALOGUE, CATALOGUE_DEMO, true);
    loader.create(database, RD3, RD3_SCHEMA, true);
  }

  @Test
  void discoveryFindsBothBundlesAndCreatesBothSchemas() {
    assertEquals(List.of(CATALOGUE, RD3), loader.templates());
    assertNotNull(
        database.getSchema(CATALOGUE_NO_DEMO).getTable(COLLECTIONS),
        "catalogue schema must have Collections");
    assertNotNull(
        database.getSchema(RD3_SCHEMA).getTable("Subjects"), "rd3 schema must have Subjects");
    assertNotNull(database.getSchema(COMPANION), "companion ontology schema must be provisioned");
  }

  @Test
  void sharedAndRedefinedContactsDiverge() {
    Schema catalogueSchema = database.getSchema(CATALOGUE_NO_DEMO);
    Schema rd3Schema = database.getSchema(RD3_SCHEMA);

    assertNotNull(
        catalogueSchema.getTable(CONTACTS).getMetadata().getColumn(EMAIL),
        "catalogue Contacts (shared) must carry the email column");
    assertNull(
        rd3Schema.getTable(CONTACTS).getMetadata().getColumn(EMAIL),
        "rd3 Contacts (redefined) must not carry the shared email column");
    assertNotNull(
        rd3Schema.getTable(CONTACTS).getMetadata().getColumn("firstName"),
        "rd3 Contacts (redefined) must carry its own firstName column");
  }

  @Test
  void importedTableFileColumnsResolveInWorkspace() {
    assertNotNull(
        database
            .getSchema(CATALOGUE_NO_DEMO)
            .getTable(COLLECTIONS)
            .getMetadata()
            .getColumn("notes"),
        "Collections must carry the 'notes' column pulled in via the table file's own imports:");
  }

  @Test
  void rootLevelImportsResolveInWorkspace() {
    database.dropSchemaIfExists(ROOT_IMPORTS_SCHEMA);
    Schema schema = loader.create(database, ROOT_IMPORTS_TEMPLATE, ROOT_IMPORTS_SCHEMA, false);
    assertNotNull(
        schema.getTable("Widget").getMetadata().getColumn("reviewed"),
        "an inline table must resolve a column pulled in via the bundle's own root-level imports:");
  }

  @Test
  void availableTemplatesCarryYamlLabelAndDemoFlag() {
    List<YamlWorkspaceLoader.TemplateInfo> available = loader.availableTemplates();
    assertEquals(
        List.of(CATALOGUE, RD3),
        available.stream().map(YamlWorkspaceLoader.TemplateInfo::name).toList());
    YamlWorkspaceLoader.TemplateInfo catalogue = available.get(0);
    assertEquals(
        "catalogue yaml", catalogue.label(), "discovered template label carries the yaml suffix");
    assertTrue(catalogue.hasDemoData(), "the catalogue template carries demo: data");
  }

  @Test
  void dataLoadsAlwaysDemoLoadsOnlyOnRequest() {
    Schema noDemo = database.getSchema(CATALOGUE_NO_DEMO);
    assertEquals(
        2,
        noDemo.getTable(ORGANISATIONS).retrieveRows().size(),
        "data: reference rows must load even without demo data");
    assertTrue(
        noDemo.getTable(COLLECTIONS).retrieveRows().isEmpty(),
        "demo: rows must not load when demo data is not requested");

    Schema withDemo = database.getSchema(CATALOGUE_DEMO);
    assertEquals(
        2,
        withDemo.getTable(COLLECTIONS).retrieveRows().size(),
        "demo: rows must load when demo data is requested");
  }

  @Test
  void rootPermissionsAndSettingsAndCompanionPermissionsApply() {
    Schema catalogueSchema = database.getSchema(CATALOGUE_NO_DEMO);
    assertEquals(
        "[{\"label\":\"Home\",\"href\":\"/\"}]",
        catalogueSchema.getMetadata().getSetting("menu"),
        "bundle-root settings must be written to the created schema");
    assertEquals(
        Privileges.VIEWER.toString(),
        catalogueSchema.getRoleForUser(Constants.ANONYMOUS),
        "bundle-root permissions must add the role default to the main schema");
    assertEquals(
        Privileges.VIEWER.toString(),
        database.getSchema(COMPANION).getRoleForUser(Constants.ANONYMOUS),
        "companion permissions must add the role default to the provisioned companion schema");
  }

  @Test
  void companionDataIsEnsuredAdditivelyOnReuse() {
    database.dropSchemaIfExists(REUSE_MAIN_A);
    database.dropSchemaIfExists(REUSE_MAIN_B);
    database.dropSchemaIfExists(REUSE_COMPANION);

    loader.create(database, REUSE_TEMPLATE, REUSE_MAIN_A, false);
    Table countries = database.getSchema(REUSE_COMPANION).getTable(COUNTRIES);
    countries.delete(row(NAME, NETHERLANDS));
    assertTrue(
        countries.retrieveRows().stream()
            .noneMatch(term -> NETHERLANDS.equals(term.getString(NAME))),
        "precondition: the existing companion no longer contains the Netherlands term");

    loader.create(database, REUSE_TEMPLATE, REUSE_MAIN_B, false);

    List<Row> terms = database.getSchema(REUSE_COMPANION).getTable(COUNTRIES).retrieveRows();
    assertTrue(
        terms.stream().anyMatch(term -> NETHERLANDS.equals(term.getString(NAME))),
        "reprovisioning must ensure the companion data: term is present after reuse");
    assertTrue(
        terms.stream().anyMatch(term -> "Belgium".equals(term.getString(NAME))),
        "pre-existing companion rows must remain untouched after additive reuse");
  }
}
