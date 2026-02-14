package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.TABLE;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.RoleInfo;
import org.molgenis.emx2.Schema;

public class GraphqlPermissionUtils {

  private GraphqlPermissionUtils() {}

  @SuppressWarnings("unchecked")
  static Permission mapToPermission(Map<String, Object> permMap) {
    Permission perm = new Permission();
    perm.setTable((String) permMap.get(TABLE));
    perm.setRowLevel(Boolean.TRUE.equals(permMap.get(ROW_LEVEL)));
    perm.setSelect((Boolean) permMap.get(SELECT));
    perm.setInsert((Boolean) permMap.get(INSERT));
    perm.setUpdate((Boolean) permMap.get(UPDATE));
    perm.setDelete((Boolean) permMap.get(DELETE));
    perm.setEditColumns((List<String>) permMap.get(EDIT_COLUMNS));
    perm.setDenyColumns((List<String>) permMap.get(DENY_COLUMNS));
    return perm;
  }

  static Map<String, Object> permissionToMap(Permission perm) {
    Map<String, Object> permMap = new LinkedHashMap<>();
    if (perm.getSchema() != null) {
      permMap.put(SCHEMA_NAME, perm.getSchema());
    }
    permMap.put(TABLE, perm.getTable());
    permMap.put(ROW_LEVEL, perm.isRowLevel());
    permMap.put(SELECT, perm.getSelect());
    permMap.put(INSERT, perm.getInsert());
    permMap.put(UPDATE, perm.getUpdate());
    permMap.put(DELETE, perm.getDelete());
    permMap.put(EDIT_COLUMNS, perm.getEditColumns());
    permMap.put(DENY_COLUMNS, perm.getDenyColumns());
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
          schema.revokePermission(roleName, perm.getTable(), perm.isRowLevel());
        } else {
          schema.setPermission(roleName, perm);
        }
      }
    }
  }
}
