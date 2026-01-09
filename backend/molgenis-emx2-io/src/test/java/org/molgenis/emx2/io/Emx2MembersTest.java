package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class Emx2MembersTest {

  private Schema schema;
  private TableStoreForCsvInMemory store;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(Emx2MembersTest.class.getSimpleName());
    store = new TableStoreForCsvInMemory();
  }

  @Test
  void shouldInputRoles() {
    addTestUserToStore();

    schema.addMember("bofke", Privileges.VIEWER.toString());
    Emx2Members.inputRoles(store, schema);
    List<Member> members = schema.getMembers();
    assertEquals(
        List.of(
            new Member("bofke", Privileges.VIEWER.toString()),
            new Member("test-user", Privileges.VIEWER.toString())),
        members);
  }

  @Test
  void givenUnauthorizedUser_thenDoNotInputRoles() {
    addTestUserToStore();

    schema.getDatabase().clearActiveUser();
    int nrInput = Emx2Members.inputRoles(store, schema);
    assertEquals(0, nrInput);
  }

  @Test
  void shouldOutputRoles() {
    schema.addMember("bofke", Privileges.VIEWER.toString());
    Emx2Members.outputRoles(store, schema);
    List<Row> rows = IteratorUtils.toList(store.readTable(Emx2Members.ROLES_TABLE).iterator());
    assertEquals(1, rows.size());
    assertEquals(
        rows.get(0).getValueMap(),
        Map.of(Emx2Members.USER, "bofke", Emx2Members.ROLE, Privileges.VIEWER.toString()));
  }

  @Test
  void givenUnauthorizedUser_thenDoNotOutputRoles() {
    schema.getDatabase().clearActiveUser();
    Emx2Members.outputRoles(store, schema);
    assertFalse(store.containsTable(Emx2Members.ROLES_TABLE));
  }

  private void addTestUserToStore() {
    Row row = new Row();
    row.set(Emx2Members.USER, "test-user");
    row.set(Emx2Members.ROLE, Privileges.VIEWER.toString());
    store.writeTable(
        Emx2Members.ROLES_TABLE, List.of(Emx2Members.USER, Emx2Members.ROLE), List.of(row));
  }
}
