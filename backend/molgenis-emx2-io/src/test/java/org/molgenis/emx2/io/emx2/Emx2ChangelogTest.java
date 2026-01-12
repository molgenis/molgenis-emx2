package org.molgenis.emx2.io.emx2;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.IS_CHANGELOG_ENABLED;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Change;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlSchema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class Emx2ChangelogTest {

  private static final List<Privileges> AUTHORIZED_PRIVILEGES =
      List.of(Privileges.OWNER, Privileges.MANAGER);

  private SqlSchema schema;
  private TableStoreForCsvInMemory store;

  @BeforeEach
  void setUp() {
    SqlDatabase sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    sqlDatabase.dropCreateSchema(Emx2ChangelogTest.class.getSimpleName());

    Map<String, String> settings = Map.of(IS_CHANGELOG_ENABLED, "true");
    schema = sqlDatabase.getSchema(Emx2ChangelogTest.class.getSimpleName());
    schema.getMetadata().setSettings(settings);

    // Generate changelog
    schema.create(table("test", column("A").setPkey(), column("B")));
    schema.getTable("test").insert(List.of(row("A", "a1", "B", "B")));

    store = new TableStoreForCsvInMemory();
  }

  @Test
  void givenAuthorizedUser_thenOutputChangelog() {
    for (Privileges privilege : AUTHORIZED_PRIVILEGES) {
      schema.getDatabase().becomeAdmin();
      schema.removeMember("test-user");
      schema.addMember("test-user", privilege.toString());
      schema.getDatabase().setActiveUser("test-user");

      Emx2Changelog.outputChangelog(store, schema);
      assertChangelogIsOutput();
    }
  }

  @Test
  void givenUnauthorizedUser_thenDontOutputChangelog() {
    for (Privileges privilege : unauthorizedPrivileges()) {
      schema.getDatabase().becomeAdmin();
      schema.removeMember("test-user");
      schema.addMember("test-user", privilege.toString());
      schema.getDatabase().setActiveUser("test-user");

      Emx2Changelog.outputChangelog(store, schema);
      assertFalse(store.containsTable(Constants.CHANGELOG_TABLE));
    }
  }

  private List<Privileges> unauthorizedPrivileges() {
    return Arrays.stream(Privileges.values()).filter(not(AUTHORIZED_PRIVILEGES::contains)).toList();
  }

  private void assertChangelogIsOutput() {
    Change change =
        schema.getChanges(schema.getChangesCount()).stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("No changes found"));

    Map<String, Object> expected = new HashMap<>();
    expected.put(Constants.CHANGELOG_NEW, change.newRowData());
    expected.put(Constants.CHANGELOG_OLD, change.oldRowData());
    expected.put(Constants.CHANGELOG_STAMP, String.valueOf(change.stamp()));
    expected.put(Constants.CHANGELOG_TABLENAME, change.tableName());
    expected.put(Constants.CHANGELOG_OPERATION, String.valueOf(change.operation()));
    expected.put(Constants.CHANGELOG_USERID, change.userId());

    Emx2Changelog.outputChangelog(store, schema);
    List<Row> actual = IteratorUtils.toList(store.readTable(Constants.CHANGELOG_TABLE).iterator());

    assertEquals(1, actual.size());
    assertEquals(expected, actual.getFirst().getValueMap());
  }
}
