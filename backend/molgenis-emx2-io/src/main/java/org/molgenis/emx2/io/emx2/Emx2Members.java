package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Row.row;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Member;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStore;

/** outputs into MOLGENIS_MEMBERS */
public class Emx2Members {
  public static final String ROLES_TABLE = "molgenis_members";
  public static final String USER = "user";
  public static final String ROLE = "role";

  private Emx2Members() {
    // prevent
  }

  public static void outputRoles(TableStore store, Schema schema) {
    List<Row> members = new ArrayList<>();
    for (Member m : schema.getMembers()) {
      members.add(row(USER, m.getUser(), ROLE, m.getRole()));
    }
    if (!members.isEmpty()) {
      store.writeTable(ROLES_TABLE, List.of(USER, ROLE), members);
    }
  }

  public static int inputRoles(TableStore store, Schema schema) {
    int count = 0;
    if (store.containsTable(ROLES_TABLE)) {
      for (Row row : store.readTable(ROLES_TABLE, null)) {
        count++;
        schema.addMember(row.getString(USER), row.getString(ROLE));
      }
    }
    return count;
  }
}
