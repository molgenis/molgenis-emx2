package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
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
  void shouldInputMembers() {
    addTestUserToStore();

    schema.addMember("bofke", Privileges.VIEWER.toString());
    Emx2Members.inputMembers(store, schema);
    List<Member> members = schema.getMembers();
    assertEquals(
        List.of(
            new Member("bofke", Privileges.VIEWER.toString()),
            new Member("test-user", Privileges.VIEWER.toString())),
        members);
  }

  @Test
  void givenUnauthorizedUser_thenInputMembersThrows() {
    addTestUserToStore();

    schema.getDatabase().clearActiveUser();
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Members.inputMembers(store, schema));
    assertTrue(exception.getMessage().contains("Unauthorized to import members"));
  }

  @Test
  void shouldOutputMembers() {
    schema.addMember("bofke", Privileges.VIEWER.toString());
    Emx2Members.outputMembers(store, schema);
    List<Row> rows = IteratorUtils.toList(store.readTable(Emx2Members.MEMBERS_TABLE).iterator());
    assertEquals(1, rows.size());
    assertEquals(
        rows.get(0).getValueMap(),
        Map.of(Emx2Members.USER, "bofke", Emx2Members.ROLE, Privileges.VIEWER.toString()));
  }

  @Test
  void givenUnauthorizedUser_thenDoNotOutputMembers() {
    schema.getDatabase().clearActiveUser();
    Emx2Members.outputMembers(store, schema);
    assertFalse(store.containsTable(Emx2Members.MEMBERS_TABLE));
  }

  private void addTestUserToStore() {
    Row row = new Row();
    row.set(Emx2Members.USER, "test-user");
    row.set(Emx2Members.ROLE, Privileges.VIEWER.toString());
    store.writeTable(
        Emx2Members.MEMBERS_TABLE, List.of(Emx2Members.USER, Emx2Members.ROLE), List.of(row));
  }
}
