package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.TABLE;
import static org.molgenis.emx2.Row.row;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2Roles {
  public static final String ROLES_TABLE = "molgenis_roles";
  public static final String ROLE = "role";
  public static final String SELECT = "select";
  public static final String INSERT = "insert";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";
  public static final String IS_ROW_LEVEL = "isRowLevel";

  public static final List<String> COLUMN_NAMES =
      List.of(ROLE, TABLE, SELECT, INSERT, UPDATE, DELETE, IS_ROW_LEVEL);

  private Emx2Roles() {
    // prevent
  }

  public static void outputRoles(TableStore store, Schema schema) {
    if (!PermissionEvaluator.canManage(schema)) {
      return;
    }

    List<Row> rows = new ArrayList<>();
    for (Role role : schema.getRoleInfos()) {
      if (role.isSystemRole()) {
        continue;
      }
      if (role.permissions().isEmpty()) {
        rows.add(row(ROLE, role.name()));
        continue;
      }
      for (TablePermission permission : role.permissions()) {
        rows.add(
            row(
                ROLE, role.name(),
                TABLE, permission.table(),
                SELECT, permission.select(),
                INSERT, permission.insert(),
                UPDATE, permission.update(),
                DELETE, permission.delete(),
                IS_ROW_LEVEL, permission.isRowLevel()));
      }
    }
    if (!rows.isEmpty()) {
      store.writeTable(ROLES_TABLE, COLUMN_NAMES, rows);
    }
  }

  public static int inputRoles(TableStore store, Schema schema) {
    if (!PermissionEvaluator.canManage(schema)) {
      throw new MolgenisException(
          "Unauthorized to import roles into schema '%s'".formatted(schema.getName()));
    }
    if (!store.containsTable(ROLES_TABLE)) {
      return 0;
    }

    int lineNumber = 1;
    int count = 0;
    for (Row row : store.readTable(ROLES_TABLE)) {
      lineNumber++;
      String roleName = row.getString(ROLE);
      if (roleName == null || roleName.isBlank()) {
        throw new MolgenisException(
            "Loading of role failed on line "
                + lineNumber
                + ": column '"
                + ROLE
                + "' is required in '"
                + ROLES_TABLE
                + "'");
      }
      if (schema.getRoleInfos().stream().noneMatch(existing -> existing.name().equals(roleName))) {
        schema.createRole(roleName);
        count++;
      }
      String tableName = row.getString(TABLE);
      if (tableName != null && !tableName.isBlank()) {
        schema.grant(roleName, toTablePermission(row, tableName));
      }
    }
    return count;
  }

  private static TablePermission toTablePermission(Row row, String tableName) {
    return new TablePermission(tableName)
        .select(row.getBoolean(SELECT))
        .insert(row.getBoolean(INSERT))
        .update(row.getBoolean(UPDATE))
        .delete(row.getBoolean(DELETE))
        .rowLevel(row.getBoolean(IS_ROW_LEVEL));
  }
}
