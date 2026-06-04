package org.molgenis.emx2.sql.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.UserPermissions;
import org.molgenis.emx2.sql.SqlDatabase;

public class CachedUserPermissions implements UserPermissions {

  private final SqlDatabase database;
  private final String schemaName;
  private Map<String, TablePermission> tablePermissions;

  public CachedUserPermissions(SchemaMetadata schema) {
    this.schemaName = schema.getName();
    this.database = (SqlDatabase) schema.getDatabase();
  }

  private Map<String, TablePermission> getValue() {
    // cached because per-table lookups in PermissionEvaluator are called very often during query
    // and schema building; is cleared along with rolesCache in reload()
    if (tablePermissions == null) {
      Map<String, TablePermission> byTable = new LinkedHashMap<>();
      for (TablePermission p :
          database.getRoleManager().getTablePermissionsForActiveUser(schemaName)) {
        byTable.putIfAbsent(p.table(), p);
      }
      tablePermissions = Collections.unmodifiableMap(byTable);
    }

    return tablePermissions;
  }

  @Override
  public Map<String, TablePermission> getByTable() {
    return getValue();
  }

  @Override
  public List<TablePermission> getAll() {
    return List.copyOf(getValue().values());
  }

  public void clearCache() {
    tablePermissions = null;
  }
}
