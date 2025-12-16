package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.RunMolgenisEmx2;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

@Tag("slow")
class ApiTestBase {

  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static final String PET_SHOP_VIEWER = "shopviewer";
  public static final String PET_SHOP_MANAGER = "shopmanager";

  public static final String PET_STORE_SCHEMA = "pet store";

  protected static final int PORT = 8081; // other than default so we can see effect

  protected static String sessionId; // to toss around a session for the tests
  protected static Database db;
  protected static Schema schema;

  @BeforeAll
  static void startServerAndCreateSchema() throws Exception {
    // FIXME: beforeAll fails under windows
    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();

    // start web service for testing, including env variables
    new EnvironmentVariables(MOLGENIS_METRICS_ENABLED, Boolean.TRUE.toString())
        .execute(
            () -> {
              RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});
            });

    // set default rest assured settings
    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";

    // create an admin session to work with
    String adminPass =
        (String) EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);

    sessionId =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\""
                    + db.getAdminUserName()
                    + "\\\",password:\\\""
                    + adminPass
                    + "\\\"){message}}\"}")
            .when()
            .post("api/graphql")
            .sessionId();

    // Always create test database from scratch to avoid instability due to side effects.
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
    schema = db.createSchema(PET_STORE_SCHEMA);
    PET_STORE.getImportTask(schema, true).run();

    // grant a user permission
    db.setUserPassword(PET_SHOP_OWNER, PET_SHOP_OWNER);
    db.setUserPassword(PET_SHOP_VIEWER, PET_SHOP_VIEWER);
    db.setUserPassword(PET_SHOP_MANAGER, PET_SHOP_MANAGER);

    schema.addMember(PET_SHOP_MANAGER, Privileges.MANAGER.toString());
    schema.addMember(PET_SHOP_VIEWER, Privileges.VIEWER.toString());
    schema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
    schema.addMember(ANONYMOUS, Privileges.VIEWER.toString());
    db.grantCreateSchema(PET_SHOP_OWNER);
  }

  @AfterAll
  static void dropSchema() {
    // Always clean up database to avoid instability due to side effects.
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
  }
}
