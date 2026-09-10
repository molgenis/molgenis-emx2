package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Row.row;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;

/** outputs into MOLGENIS_MEMBERS */
public class Emx2Members {
  public static final String MEMBERS_TABLE = "molgenis_members";
  public static final String USER = "user";
  public static final String ROLE = "role";

  private Emx2Members() {
    // prevent
  }

  public static void outputMembers(TableStore store, Schema schema) {
    if (!PermissionEvaluator.canManage(schema)) {
      return;
    }

    List<Row> members = new ArrayList<>();
    for (Member m : schema.getMembers()) {
      members.add(row(USER, m.getUser(), ROLE, m.getRole()));
    }
    if (!members.isEmpty()) {
      store.writeTable(MEMBERS_TABLE, List.of(USER, ROLE), members);
    }
  }

  public static int inputMembers(TableStore store, Schema schema) {
    if (!PermissionEvaluator.canManage(schema)) {
      throw new MolgenisException(
          "Unauthorized to import members into schema '%s'".formatted(schema.getName()));
    }

    int count = 0;
    if (store.containsTable(MEMBERS_TABLE)) {
      for (Row row : store.readTable(MEMBERS_TABLE)) {
        count++;
        schema.addMember(row.getString(USER), row.getString(ROLE));
      }
    }
    return count;
  }
}
