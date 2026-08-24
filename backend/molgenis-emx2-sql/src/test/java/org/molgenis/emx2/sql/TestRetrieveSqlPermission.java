package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestRetrieveSqlPermission {

  private static final String SCHEMA = TestRetrieveSqlPermission.class.getSimpleName();
  private static final String ARTICLES = "Articles";
  private static final String SQL = "SELECT \"title\" FROM \"" + SCHEMA + "\".\"" + ARTICLES + "\"";

  private static final String USER_VIEWER = "raw_sql_viewer";
  private static final String USER_AGGREGATOR = "raw_sql_aggregator";
  private static final String USER_COUNT = "raw_sql_count";

  private static Database database;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    for (String user : List.of(USER_VIEWER, USER_AGGREGATOR, USER_COUNT)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(table(ARTICLES).add(column("id").setPkey()).add(column("title")));
    schema.getTable(ARTICLES).insert(new Row("id", "a1", "title", "secret"));

    schema.addMember(USER_VIEWER, Privileges.VIEWER.toString());
    schema.addMember(USER_AGGREGATOR, Privileges.AGGREGATOR.toString());
    schema.addMember(USER_COUNT, Privileges.COUNT.toString());
  }

  @AfterEach
  void becomeAdminAgain() {
    database.becomeAdmin();
  }

  @Test
  void aggregatorCannotRunRawSql() {
    database.setActiveUser(USER_AGGREGATOR);
    Schema schema = database.getSchema(SCHEMA);
    assertThrows(MolgenisException.class, () -> schema.retrieveSql(SQL));
  }

  @Test
  void countCannotRunRawSql() {
    database.setActiveUser(USER_COUNT);
    Schema schema = database.getSchema(SCHEMA);
    assertThrows(MolgenisException.class, () -> schema.retrieveSql(SQL));
  }

  @Test
  void viewerCanRunRawSql() {
    database.setActiveUser(USER_VIEWER);
    List<Row> rows = database.getSchema(SCHEMA).retrieveSql(SQL);
    assertEquals(1, rows.size());
    assertEquals("secret", rows.get(0).getString("title"));
  }

  @Test
  void adminCanRunRawSql() {
    database.becomeAdmin();
    List<Row> rows = database.getSchema(SCHEMA).retrieveSql(SQL);
    assertEquals(1, rows.size());
  }
}
