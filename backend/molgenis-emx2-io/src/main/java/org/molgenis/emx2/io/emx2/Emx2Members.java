package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Row.row;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Member;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStore;

/** outputs into MOLGENIS_MEMBERS */
public class Emx2Roles {
  public static final String ROLES_TABLE = "molgenis_members";
  public static final String USER = "user";
  public static final String ROLE = "role";

  public static void outputRoles(TableStore store, Schema schema) {
    List<Row> members = new ArrayList<>();
    for (Member m : schema.getMembers()) {
      members.add(row(USER, m.getUser(), ROLE, m.getRole()));
    }
    if (members.size() > 0) {
      store.writeTable(ROLES_TABLE, members);
    }
  }

  public static void inputRoles(TableStore store, Schema schema) {
    if (store.containsTable(ROLES_TABLE)) {
      for (Row row : store.readTable(ROLES_TABLE)) {
        schema.addMember(row.getString(USER), row.getString(ROLE));
      }
    }
  }
}
