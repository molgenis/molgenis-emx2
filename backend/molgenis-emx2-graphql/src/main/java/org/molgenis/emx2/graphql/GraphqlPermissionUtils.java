package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.TABLE;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.ColumnAccess;
import org.molgenis.emx2.ModifyLevel;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.RoleInfo;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SelectLevel;

public class GraphqlPermissionUtils {

  private GraphqlPermissionUtils() {}

  @SuppressWarnings("unchecked")
  static Permission mapToPermission(Map<String, Object> permMap) {
    Permission perm = new Permission();
    perm.setTable((String) permMap.get(TABLE));

    if (permMap.get(SELECT) != null) {
      perm.setSelect(parseSelectLevel(permMap.get(SELECT)));
    }
    if (permMap.get(INSERT) != null) {
      perm.setInsert(parseModifyLevel(permMap.get(INSERT)));
    }
    if (permMap.get(UPDATE) != null) {
      perm.setUpdate(parseModifyLevel(permMap.get(UPDATE)));
    }
    if (permMap.get(DELETE) != null) {
      perm.setDelete(parseModifyLevel(permMap.get(DELETE)));
    }

    Map<String, Object> columnsMap = (Map<String, Object>) permMap.get(COLUMN_ACCESS);
    if (columnsMap != null) {
      ColumnAccess columnAccess = new ColumnAccess();
      columnAccess.setEditable((List<String>) columnsMap.get(EDITABLE));
      columnAccess.setReadonly((List<String>) columnsMap.get(READONLY_FIELD));
      columnAccess.setHidden((List<String>) columnsMap.get(HIDDEN));
      perm.setColumnAccess(columnAccess);
    }

    return perm;
  }

  private static SelectLevel parseSelectLevel(Object value) {
    if (value instanceof String) {
      return SelectLevel.valueOf(((String) value).toUpperCase());
    } else if (value instanceof Boolean && Boolean.TRUE.equals(value)) {
      return SelectLevel.TABLE;
    }
    return null;
  }

  private static ModifyLevel parseModifyLevel(Object value) {
    if (value instanceof String) {
      return ModifyLevel.valueOf(((String) value).toUpperCase());
    } else if (value instanceof Boolean && Boolean.TRUE.equals(value)) {
      return ModifyLevel.TABLE;
    }
    return null;
  }

  static Map<String, Object> permissionToMap(Permission perm) {
    Map<String, Object> permMap = new LinkedHashMap<>();
    if (perm.getSchema() != null) {
      permMap.put(SCHEMA_NAME, perm.getSchema());
    }
    permMap.put(TABLE, perm.getTable());
    permMap.put(SELECT, perm.getSelect() != null ? perm.getSelect().toString() : null);
    permMap.put(INSERT, perm.getInsert() != null ? perm.getInsert().toString() : null);
    permMap.put(UPDATE, perm.getUpdate() != null ? perm.getUpdate().toString() : null);
    permMap.put(DELETE, perm.getDelete() != null ? perm.getDelete().toString() : null);

    if (perm.getColumnAccess() != null) {
      Map<String, Object> columnsMap = new LinkedHashMap<>();
      columnsMap.put(EDITABLE, perm.getColumnAccess().getEditable());
      columnsMap.put(READONLY_FIELD, perm.getColumnAccess().getReadonly());
      columnsMap.put(HIDDEN, perm.getColumnAccess().getHidden());
      permMap.put(COLUMN_ACCESS, columnsMap);
    }

    return permMap;
  }

  static Map<String, Object> roleInfoToMap(RoleInfo roleInfo) {
    Map<String, Object> roleMap = new LinkedHashMap<>();
    roleMap.put(NAME, roleInfo.getName());
    roleMap.put(DESCRIPTION, roleInfo.getDescription());
    roleMap.put(SYSTEM, roleInfo.isSystem());
    List<Map<String, Object>> permList = new ArrayList<>();
    for (Permission perm : roleInfo.getPermissions()) {
      permList.add(permissionToMap(perm));
    }
    roleMap.put(PERMISSIONS, permList);
    return roleMap;
  }

  @SuppressWarnings("unchecked")
  static void applyPermissions(
      Schema schema, String roleName, List<Map<String, Object>> permissions) {
    if (permissions != null) {
      for (Map<String, Object> permMap : permissions) {
        Permission perm = mapToPermission(permMap);
        if (perm.isRevocation()) {
          schema.revoke(roleName, perm);
        } else {
          schema.grant(roleName, perm);
        }
      }
    }
  }
}
