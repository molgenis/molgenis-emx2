package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;

class ModelApiTest extends ApiTestBase {

  private static final String VERSION_SCHEMA = "ModelApiTestVersion";
  private static final String ROUNDTRIP_SCHEMA = "ModelApiTestRoundtrip";
  private static final String VALIDATOR_SCHEMA = "ModelApiTestValidator";
  private static final String COMPANION_ROOT_SCHEMA = "ModelApiTestCompanionRoot";
  private static final String COMPANION_SCHEMA = "ModelApiTestCompanionOnt";
  private static final String CYCLE_SCHEMA = "ModelApiTestCycle";
  private static final String ATOMIC_SCHEMA = "ModelApiTestAtomic";
  private static final String ATOMIC_COMPANION = "ModelApiTestAtomicCompanion";
  private static final String TABLE_ADD_SCHEMA = "ModelApiTestTableAdd";
  private static final String REF_ORDER_SCHEMA = "ModelApiTestRefOrder";
  private static final String PREVIOUS_NAMES_SCHEMA = "ModelApiTestPreviousNames";
  private static final String BUNDLE_REF_SCHEMA = "ModelApiTestBundleRef";
  private static final String RENAME_FALLBACK_SCHEMA = "ModelApiTestRenameFallback";
  private static final String DROP_SCHEMA = "ModelApiTestDrop";
  private static final String PERMISSION_ROOT_SCHEMA = "ModelApiTestPermissionRoot";
  private static final String PERMISSION_COMPANION = "ModelApiTestPermissionOnt";
  private static final String DATA_DEMO_SCHEMA = "ModelApiTestDataDemo";
  private static final String ROOT_APPLY_SCHEMA = "ModelApiTestRootApply";
  private static final String MENU_SETTING = "menu";
  private static final String MENU_VALUE = "[{\"label\":\"Home\",\"href\":\"/\"}]";
  private static final String YAML_GET_SCHEMA = "ModelApiTestYamlGet";
  private static final String LEGACY_MODEL_SCHEMA = "ModelApiTestLegacyModel";

  @BeforeAll
  static void setup() {
    login("admin", "admin");
  }

  private static Schema createPersonSchema(String schemaName) {
    Schema schema = database.dropCreateSchema(schemaName);
    schema.create(table("Person", column("id").setKey(1), column("name", STRING)));
    return schema;
  }

  private static String modelPath(String schemaName) {
    return "/" + schemaName + "/api/yaml";
  }

  private static String getModel(String schemaName) {
    return given().sessionId(sessionId).when().get(modelPath(schemaName)).asString();
  }

  private static Response putModel(String schemaName, String body, String query) {
    return given().sessionId(sessionId).body(body).when().put(modelPath(schemaName) + query);
  }

  private static String withVersion(String schemaName, String version) {
    String model = getModel(schemaName).replaceFirst("version: .*\n", "");
    return model.replaceFirst("formatVersion: 1\n", "formatVersion: 1\nversion: " + version + "\n");
  }

  @Test
  void getAtYamlPathReturnsBundleFormat() {
    createPersonSchema(YAML_GET_SCHEMA);
    String yaml =
        given().sessionId(sessionId).when().get("/" + YAML_GET_SCHEMA + "/api/yaml").asString();
    assertTrue(
        yaml.contains("formatVersion"),
        "GET /<schema>/api/yaml must serve the bundle single-file format (the old yaml had none)");
  }

  @Test
  void getModelSetsYamlDownloadFilename() {
    createPersonSchema(YAML_GET_SCHEMA);
    String disposition =
        given()
            .sessionId(sessionId)
            .when()
            .get("/" + YAML_GET_SCHEMA + "/api/yaml")
            .getHeader("Content-Disposition");
    assertNotNull(disposition, "yaml download must carry a Content-Disposition filename");
    assertTrue(
        disposition.startsWith("attachment; filename=\"" + YAML_GET_SCHEMA),
        "download filename must be an attachment named after the schema");
    assertTrue(disposition.endsWith(".yaml\""), "download filename must carry the .yaml extension");
  }

  @Test
  void legacyModelPathIsGone() {
    createPersonSchema(LEGACY_MODEL_SCHEMA);
    given()
        .sessionId(sessionId)
        .when()
        .get("/" + LEGACY_MODEL_SCHEMA + "/api/model")
        .then()
        .statusCode(404);
  }

  @Test
  void versionLifecycle() {
    createPersonSchema(VERSION_SCHEMA);

    // initial export carries no version
    assertFalse(getModel(VERSION_SCHEMA).contains("version:"));

    // apply stamps the version on the schema, readable via GET
    putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "1.0.0"), "").then().statusCode(200);
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 1.0.0"));

    // dry-run of a newer version does NOT stamp it
    putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "2.0.0"), "?dryRun=true")
        .then()
        .statusCode(200);
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 1.0.0"));

    // applying an older version is refused, leaving the stored version untouched (no force escape)
    Response downgrade = putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "0.9.0"), "");
    downgrade.then().statusCode(400);
    assertTrue(downgrade.body().asString().contains("version"));
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 1.0.0"));

    // a non-numeric version fails validation and never stamps
    putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "1.0"), "").then().statusCode(400);
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 1.0.0"));
  }

  @Test
  void getDryRunApplyRoundTrip() {
    createPersonSchema(ROUNDTRIP_SCHEMA);

    // the live model exports without the email column
    assertFalse(getModel(ROUNDTRIP_SCHEMA).contains("email"));

    String desired =
        """
        formatVersion: 1
        version: 2.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: email
        """;

    // dry-run reports the added column but changes nothing
    Response dryRun = putModel(ROUNDTRIP_SCHEMA, desired, "?dryRun=true");
    dryRun.then().statusCode(200);
    assertTrue(dryRun.body().asString().contains("email"));
    assertFalse(getModel(ROUNDTRIP_SCHEMA).contains("email"));

    // apply persists the change and stamps the new version
    putModel(ROUNDTRIP_SCHEMA, desired, "").then().statusCode(200);
    String applied = getModel(ROUNDTRIP_SCHEMA);
    assertTrue(applied.contains("email"));
    assertTrue(applied.contains("version: 2.0.0"));
  }

  @Test
  void putValidatorErrorCarriesDocumentPathAndPosition() {
    createPersonSchema(VALIDATOR_SCHEMA);

    String invalid =
        """
        formatVersion: 1
        tables:
        - name: Person
          columns:
          - name: id
            bogusKey: nope
        """;

    Response response = putModel(VALIDATOR_SCHEMA, invalid, "");
    response.then().statusCode(400);
    String body = response.body().asString();
    assertTrue(body.contains("molgenis.yaml"));
    assertTrue(body.contains("line"));
    assertTrue(body.contains("column"));
    assertEquals(400, response.getStatusCode());
  }

  private static String companionBundle(String companionVersion) {
    return """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: cohort
            type: ref
            refTable: %s.Cohorts
        additionalSchemas:
          %s:
            version: %s
            permissions:
              Viewer: anonymous
            tables:
            - name: Cohorts
              columns:
              - name: id
                key: 1
              - name: types
                type: ontology_array
                refTable: CohortTypes
        """
        .formatted(COMPANION_SCHEMA, COMPANION_SCHEMA, companionVersion);
  }

  @Test
  void companionLifecycle() {
    createPersonSchema(COMPANION_ROOT_SCHEMA);
    database.dropSchemaIfExists(COMPANION_SCHEMA);
    assertFalse(database.hasSchema(COMPANION_SCHEMA));

    // first apply provisions the companion with its model and role-default permissions
    putModel(COMPANION_ROOT_SCHEMA, companionBundle("1.0.0"), "").then().statusCode(200);
    database.clearCache();
    assertTrue(database.hasSchema(COMPANION_SCHEMA));
    assertEquals(
        Privileges.VIEWER.toString(),
        database.getSchema(COMPANION_SCHEMA).getRoleForUser(Constants.ANONYMOUS));

    // dotted refs resolve companion-before-instance: the root ref to the companion table applied
    String rootModel = getModel(COMPANION_ROOT_SCHEMA);
    assertTrue(rootModel.contains("refTable: " + COMPANION_SCHEMA + ".Cohorts"));

    // second apply of the same bundle leaves the existing companion byte-identical
    String companionBefore = getModel(COMPANION_SCHEMA);
    assertTrue(companionBefore.contains("Cohorts"));
    putModel(COMPANION_ROOT_SCHEMA, companionBundle("1.0.0"), "").then().statusCode(200);
    String companionAfter = getModel(COMPANION_SCHEMA);
    assertEquals(companionBefore, companionAfter);

    // dryRun warns when the existing companion is older than the referenced version
    Response dryRun = putModel(COMPANION_ROOT_SCHEMA, companionBundle("2.0.0"), "?dryRun=true");
    dryRun.then().statusCode(200);
    String dryRunBody = dryRun.body().asString();
    assertTrue(dryRunBody.contains(COMPANION_SCHEMA));
    assertTrue(dryRunBody.contains("older"));

    // a companion cycle fails validation and surfaces through the API
    createPersonSchema(CYCLE_SCHEMA);
    String cycleBundle =
        """
        formatVersion: 1
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
        additionalSchemas:
          SomeCompanion:
            bundle: molgenis.yaml
        """;
    Response cycle = putModel(CYCLE_SCHEMA, cycleBundle, "");
    cycle.then().statusCode(400);
    assertTrue(cycle.body().asString().contains("cycle"));
  }

  @Test
  void atomicApply() {
    createPersonSchema(ATOMIC_SCHEMA);
    database.dropSchemaIfExists(ATOMIC_COMPANION);
    assertFalse(database.hasSchema(ATOMIC_COMPANION));

    // the root change is valid, but the companion references a table that does not exist,
    // so the whole-bundle apply must fail and leave everything untouched
    String bundle =
        """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: extra
        additionalSchemas:
          %s:
            version: 1.0.0
            tables:
            - name: Widget
              columns:
              - name: id
                key: 1
              - name: broken
                type: ref
                refTable: NoSuchTable
        """
            .formatted(ATOMIC_COMPANION);

    Response response = putModel(ATOMIC_SCHEMA, bundle, "");
    response.then().statusCode(400);
    // the error names the schema whose apply failed
    assertTrue(response.body().asString().contains(ATOMIC_COMPANION));

    database.clearCache();
    // nothing changed: the companion was not created and the root column was rolled back
    assertFalse(database.hasSchema(ATOMIC_COMPANION));
    assertFalse(getModel(ATOMIC_SCHEMA).contains("extra"));
  }

  @Test
  void tableAddsAppliedParentFirst() {
    createPersonSchema(TABLE_ADD_SCHEMA);

    // a single bundle introduces a new parent table AND a new subclass extending it
    String bundle =
        """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
        - name: Animal
          subclasses:
          - name: Dog
          columns:
          - name: species
            key: 1
          - name: breed
            subclass: Dog
        """;

    // dry-run: both the new parent and the new subclass are planned as table adds
    Response dryRun = putModel(TABLE_ADD_SCHEMA, bundle, "?dryRun=true");
    dryRun.then().statusCode(200);
    String plan = dryRun.body().asString();
    assertTrue(plan.contains("tableAdds"));
    assertTrue(plan.contains("Animal"));
    assertTrue(plan.contains("Dog"));

    // apply creates the parent before the subclass and the subclass inherits the parent's key
    putModel(TABLE_ADD_SCHEMA, bundle, "").then().statusCode(200);
    database.clearCache();
    Schema applied = database.getSchema(TABLE_ADD_SCHEMA);
    assertEquals(List.of("Animal"), applied.getTable("Dog").getMetadata().getInheritNames());
    // the inherited key column is present once, only the local column is non-inherited
    assertNotNull(applied.getTable("Dog").getMetadata().getColumn("species"));
    assertEquals(
        List.of("breed"),
        applied.getTable("Dog").getMetadata().getNonInheritedColumns().stream()
            .filter(column -> !column.isSystemColumn())
            .map(org.molgenis.emx2.Column::getName)
            .toList());
  }

  @Test
  void tableAddsWithForwardRefApplyInDependencyOrder() {
    database.dropCreateSchema(REF_ORDER_SCHEMA);

    // a table that refs another same-schema table declared AFTER it (Endpoint.publisher -> Agents)
    String bundle =
        """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Endpoint
          columns:
          - name: id
            key: 1
          - name: publisher
            type: ref_array
            refTable: Agents
        - name: Agents
          columns:
          - name: id
            key: 1
        """;

    putModel(REF_ORDER_SCHEMA, bundle, "").then().statusCode(200);
    database.clearCache();
    Schema applied = database.getSchema(REF_ORDER_SCHEMA);
    assertNotNull(applied.getTable("Endpoint"));
    assertNotNull(applied.getTable("Agents"));
    assertEquals(
        "Agents",
        applied.getTable("Endpoint").getMetadata().getColumn("publisher").getRefTableName());
  }

  @Test
  void previousNamesPersistAcrossGet() {
    createPersonSchema(PREVIOUS_NAMES_SCHEMA);

    // a PUT carrying a rename chain on the name column persists it server-side
    String withChain =
        """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
            previousNames:
            - formerName
        """;
    putModel(PREVIOUS_NAMES_SCHEMA, withChain, "").then().statusCode(200);

    // GET re-emits the persisted chain from the live schema alone
    String afterFirst = getModel(PREVIOUS_NAMES_SCHEMA);
    assertTrue(afterFirst.contains("previousNames"));
    assertTrue(afterFirst.contains("formerName"));

    // a second PUT WITHOUT chains: the persisted chain is the diff fallback and is retained
    String withoutChain =
        """
        formatVersion: 1
        version: 1.1.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
        """;
    putModel(PREVIOUS_NAMES_SCHEMA, withoutChain, "").then().statusCode(200);
    assertTrue(getModel(PREVIOUS_NAMES_SCHEMA).contains("formerName"));

    // precedence: an incoming chain WINS and replaces the persisted one wholesale per column
    String withNewChain =
        """
        formatVersion: 1
        version: 1.2.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
            previousNames:
            - renamedFrom
        """;
    putModel(PREVIOUS_NAMES_SCHEMA, withNewChain, "").then().statusCode(200);
    String afterThird = getModel(PREVIOUS_NAMES_SCHEMA);
    assertTrue(afterThird.contains("renamedFrom"));
    assertFalse(afterThird.contains("formerName"));

    // dryRun never persists: the stored chain is left untouched
    String dryChain =
        """
        formatVersion: 1
        version: 1.3.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
            previousNames:
            - neverStored
        """;
    putModel(PREVIOUS_NAMES_SCHEMA, dryChain, "?dryRun=true").then().statusCode(200);
    String afterDryRun = getModel(PREVIOUS_NAMES_SCHEMA);
    assertFalse(afterDryRun.contains("neverStored"));
    assertTrue(afterDryRun.contains("renamedFrom"));
  }

  @Test
  void wireBundleRefCompanionRejected() {
    createPersonSchema(BUNDLE_REF_SCHEMA);

    // a non-cyclic bundle: path reference cannot be resolved on the single-file wire;
    // the apply layer rejects it with a 400 whose message names the companion schema
    String bundle =
        """
        formatVersion: 1
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
        additionalSchemas:
          ExternalCohorts:
            bundle: external/cohorts.yaml
        """;
    Response response = putModel(BUNDLE_REF_SCHEMA, bundle, "");
    response.then().statusCode(400);
    assertTrue(response.body().asString().contains("ExternalCohorts"));
  }

  @Test
  void additiveApplyKeepsAbsentColumnAndDropMarkerRemoves() {
    createPersonSchema(DROP_SCHEMA);

    // seed a distinguishable column so we can watch it survive and later be dropped
    String withNickname =
        """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: nickname
        """;
    putModel(DROP_SCHEMA, withNickname, "").then().statusCode(200);
    assertTrue(getModel(DROP_SCHEMA).contains("nickname"));

    // additive: a PUT that OMITS nickname must leave it untouched (absence never deletes)
    String withoutNickname =
        """
        formatVersion: 1
        version: 1.1.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
        """;
    putModel(DROP_SCHEMA, withoutNickname, "").then().statusCode(200);
    assertTrue(getModel(DROP_SCHEMA).contains("nickname"));

    // an explicit drop marker is the only way to remove it
    String dropNickname =
        """
        formatVersion: 1
        version: 1.2.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: nickname
            drop: true
        """;
    Response dryRun = putModel(DROP_SCHEMA, dropNickname, "?dryRun=true");
    dryRun.then().statusCode(200);
    assertTrue(dryRun.body().asString().contains("columnDrops"));
    putModel(DROP_SCHEMA, dropNickname, "").then().statusCode(200);
    assertFalse(getModel(DROP_SCHEMA).contains("nickname"));
  }

  @Test
  void companionPermissionRoleMustBeExactRoleName() {
    createPersonSchema(PERMISSION_ROOT_SCHEMA);
    database.dropSchemaIfExists(PERMISSION_COMPANION);

    // a lowercase 'view' is no longer mapped: only exact role names (Viewer/...) are accepted
    String bundle =
        """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
        additionalSchemas:
          %s:
            version: 1.0.0
            permissions:
              view: anonymous
            tables:
            - name: Widget
              columns:
              - name: id
                key: 1
        """
            .formatted(PERMISSION_COMPANION);

    Response response = putModel(PERMISSION_ROOT_SCHEMA, bundle, "");
    response.then().statusCode(400);
    String body = response.body().asString();
    assertTrue(body.contains("view"));
    assertTrue(body.contains(Privileges.VIEWER.toString()));

    database.clearCache();
    assertFalse(database.hasSchema(PERMISSION_COMPANION));
  }

  @Test
  void rootPermissionsAndSettingsApplyAdditively() {
    Schema schema = createPersonSchema(ROOT_APPLY_SCHEMA);
    // an unrelated setting already on the schema must survive an apply that sets only 'menu'
    schema.getMetadata().setSetting("existingKey", "existingValue");

    String bundle =
        """
        formatVersion: 1
        version: 1.0.0
        settings:
          menu: '%s'
        permissions:
          Viewer: anonymous
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
        """
            .formatted(MENU_VALUE);

    putModel(ROOT_APPLY_SCHEMA, bundle, "").then().statusCode(200);

    database.clearCache();
    Schema applied = database.getSchema(ROOT_APPLY_SCHEMA);
    assertEquals(
        MENU_VALUE,
        applied.getMetadata().getSetting(MENU_SETTING),
        "bundle-root settings must be written to the schema");
    assertEquals(
        "existingValue",
        applied.getMetadata().getSetting("existingKey"),
        "an unrelated existing setting must survive the additive apply");
    assertEquals(
        Privileges.VIEWER.toString(),
        applied.getRoleForUser(Constants.ANONYMOUS),
        "bundle-root permissions must add the role default to the main schema");
  }

  @Test
  void persistedChainDrivesRename() {
    createPersonSchema(RENAME_FALLBACK_SCHEMA);

    // rename name -> label WITH chain; persists {Person:{label:[name]}}, live now has label
    String toLabel =
        """
        formatVersion: 1
        version: 1.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: label
            previousNames:
            - name
        """;
    putModel(RENAME_FALLBACK_SCHEMA, toLabel, "").then().statusCode(200);

    // desync: drop label with a marker and add name back WITHOUT a chain; the chain is retained
    String backToName =
        """
        formatVersion: 1
        version: 1.1.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: label
            drop: true
        """;
    putModel(RENAME_FALLBACK_SCHEMA, backToName, "").then().statusCode(200);

    // live now has name and the persisted setting still carries label:[name];
    // an incoming bundle renaming to label but OMITTING the chain must still plan a RENAME
    String labelNoChain =
        """
        formatVersion: 1
        version: 1.2.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: label
        """;
    Response dryRun = putModel(RENAME_FALLBACK_SCHEMA, labelNoChain, "?dryRun=true");
    dryRun.then().statusCode(200);
    String plan = dryRun.body().asString();
    // the merged persisted chain reached ModelDiff: a rename, not a drop + add
    assertTrue(plan.contains("columnRenames"));
    assertTrue(plan.contains("fromColumn"));
  }

  @Test
  void dataAndDemoKeysAreIgnoredWithWarning() {
    createPersonSchema(DATA_DEMO_SCHEMA);

    // a bundle carrying data:/demo: keys is accepted, but those keys are not the model API's job:
    // they are loaded by templates/loaders, so the API ignores them and warns
    String bundle =
        """
        formatVersion: 1
        version: 2.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: email
        data:
        - catalogue/data
        demo:
        - catalogue/demo
        """;

    Response dryRun = putModel(DATA_DEMO_SCHEMA, bundle, "?dryRun=true");
    dryRun.then().statusCode(200);
    String plan = dryRun.body().asString();
    assertTrue(
        plan.contains("ignoring 'data' key"), "data: key must surface an ignored-key warning");
    assertTrue(
        plan.contains("ignoring 'demo' key"), "demo: key must surface an ignored-key warning");

    // apply still succeeds: the model change lands, the ignored keys never block it
    putModel(DATA_DEMO_SCHEMA, bundle, "").then().statusCode(200);
    assertTrue(getModel(DATA_DEMO_SCHEMA).contains("email"));
  }
}
