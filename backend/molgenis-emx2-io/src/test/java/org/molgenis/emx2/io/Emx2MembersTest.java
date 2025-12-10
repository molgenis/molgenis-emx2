package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
    schema.addMember("bofke", "Viewer");
    Emx2Members.inputRoles(store, schema);
    List<Member> members = schema.getMembers();
    assertEquals(members, List.of(new Member("bofke", "Viewer")));
  }

  @Test
  void shouldOutputRoles() {
    schema.addMember("bofke", "Viewer");
    Emx2Members.outputRoles(store, schema);
    List<Row> rows = IteratorUtils.toList(store.readTable(Emx2Members.ROLES_TABLE).iterator());
    assertEquals(1, rows.size());
    assertEquals(
        rows.get(0).getValueMap(),
        Map.of(Emx2Members.USER, "bofke", Emx2Members.ROLE, Privileges.VIEWER.toString()));
  }

  @Nested
  class UnauthorizedTest {

    @Test
    void givenUnauthorizedUser_thenDoNotOutputRoles() {
      schema.getDatabase().clearActiveUser();
      assertThrows(UnauthorizedException.class, () -> Emx2Members.outputRoles(store, schema));
    }

    @Test
    void givenUnauthorizedUser_thenDoNotInputRoles() {
      schema.getDatabase().clearActiveUser();
      assertThrows(UnauthorizedException.class, () -> Emx2Members.inputRoles(store, schema));
    }
  }
}
