package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TablePermission.SelectScope;
import org.molgenis.emx2.TablePermission.UpdateScope;
import org.molgenis.emx2.sql.SqlRoleManager;

public class GraphqlPermissionFieldFactory {

  static final GraphQLEnumType updateScopeEnumType;

  static {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name("MolgenisUpdateScope");
    for (UpdateScope s : UpdateScope.values()) {
      builder.value(s.name(), s);
    }
    updateScopeEnumType = builder.build();
  }

  static final GraphQLEnumType selectScopeEnumType;

  static {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name("MolgenisSelectScope");
    for (SelectScope s : SelectScope.values()) {
      builder.value(s.name(), s);
    }
    selectScopeEnumType = builder.build();
  }

  static final GraphQLObjectType effectivePermissionType =
      GraphQLObjectType.newObject()
          .name("MolgenisEffectivePermission")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("schema")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("table").type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SELECT)
                  .type(GraphQLList.list(GraphQLNonNull.nonNull(selectScopeEnumType))))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(updateScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(updateScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(updateScopeEnumType))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeGroup")
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLObjectType rolePermissionsOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisRolePermissionsOutput")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("schema")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("table").type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SELECT)
                  .type(GraphQLList.list(GraphQLNonNull.nonNull(selectScopeEnumType))))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(updateScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(updateScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(updateScopeEnumType))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeGroup")
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLObjectType roleOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisRoleOutput")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("role").type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("systemRole")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(PERMISSIONS)
                  .type(GraphQLList.list(rolePermissionsOutputType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(MEMBERS)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .build();

  static final GraphQLInputObjectType inputPermissionFgType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisPermissionInputFg")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("schema")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("table")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SELECT)
                  .type(GraphQLList.list(GraphQLNonNull.nonNull(selectScopeEnumType))))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(INSERT).type(updateScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(UPDATE).type(updateScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(DELETE).type(updateScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("changeGroup")
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLInputObjectType inputRoleType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisRoleInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(PERMISSIONS)
                  .type(GraphQLList.list(inputPermissionFgType)))
          .build();

  static final GraphQLInputObjectType inputRoleMemberType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisRoleMemberInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("role")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("user")
                  .type(Scalars.GraphQLString))
          .build();

  public GraphqlPermissionFieldFactory() {}

  static List<Map<String, Object>> permissionsToMaps(SqlRoleManager rm, String role) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (TablePermission p : rm.getPermissions(role)) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("schema", p.schema());
      map.put("table", p.table());
      map.put(SELECT, p.select().stream().map(SelectScope::name).collect(Collectors.toList()));
      map.put(INSERT, p.insert());
      map.put(UPDATE, p.update());
      map.put(DELETE, p.delete());
      map.put("changeOwner", p.changeOwner());
      map.put("changeGroup", p.changeGroup());
      result.add(map);
    }
    return result;
  }

  static void applyRoles(Database db, SqlRoleManager rm, List<Map<String, Object>> roles) {
    if (roles == null) return;
    for (Map<String, Object> roleMap : roles) {
      String name = (String) roleMap.get(NAME);
      String description = (String) roleMap.get(DESCRIPTION);
      List<Map<String, Object>> perms = (List<Map<String, Object>>) roleMap.get(PERMISSIONS);
      if (!db.isAdmin()) {
        if (perms == null || perms.isEmpty()) {
          throw new MolgenisException("admin only");
        }
        applyPermissionsForRole(db, rm, name, perms);
      } else {
        rm.createOrUpdateRole(name, description);
        if (perms != null) {
          applyPermissionsForRole(db, rm, name, perms);
        }
      }
    }
  }

  private static void applyPermissionsForRole(
      Database db, SqlRoleManager rm, String role, List<Map<String, Object>> perms) {
    PermissionSet ps = new PermissionSet();
    for (Map<String, Object> pMap : perms) {
      String schemaName = (String) pMap.get("schema");
      requireManagerOrOwner(db, schemaName);
      ps.put(
          new TablePermission(
              schemaName,
              (String) pMap.get("table"),
              toSelectScopeSet(pMap.get(SELECT)),
              toUpdateScope(pMap.get(INSERT)),
              toUpdateScope(pMap.get(UPDATE)),
              toUpdateScope(pMap.get(DELETE)),
              Boolean.TRUE.equals(pMap.get("changeOwner")),
              Boolean.TRUE.equals(pMap.get("changeGroup"))));
    }
    rm.setPermissions(role, ps);
  }

  private static void requireManagerOrOwner(Database db, String schemaName) {
    if (db.isAdmin()) return;
    Schema schema = db.getSchema(schemaName);
    if (schema == null
        || (!schema.hasActiveUserRole(Privileges.MANAGER)
            && !schema.hasActiveUserRole(Privileges.OWNER))) {
      throw new MolgenisException(
          "Permission denied: setting permissions requires MANAGER or OWNER privilege on schema "
              + schemaName);
    }
  }

  static void applyMembers(SqlRoleManager rm, List<Map<String, Object>> members) {
    if (members == null) return;
    for (Map<String, Object> member : members) {
      rm.grantRoleToUser((String) member.get("role"), (String) member.get("user"));
    }
  }

  static Set<SelectScope> toSelectScopeSet(Object value) {
    if (value == null) return TablePermission.emptySelect();
    if (value instanceof List<?> list) {
      Set<SelectScope> result = EnumSet.noneOf(SelectScope.class);
      for (Object item : list) {
        if (item instanceof SelectScope s) {
          result.add(s);
        } else {
          try {
            result.add(SelectScope.fromString(item.toString()));
          } catch (Exception ignored) {
            // skip unknown values
          }
        }
      }
      return result;
    }
    if (value instanceof SelectScope s) return TablePermission.singletonSelect(s);
    try {
      return TablePermission.singletonSelect(SelectScope.fromString(value.toString()));
    } catch (Exception e) {
      return TablePermission.emptySelect();
    }
  }

  static UpdateScope toUpdateScope(Object value) {
    if (value == null) return UpdateScope.NONE;
    if (value instanceof UpdateScope s) return s;
    try {
      return UpdateScope.fromString(value.toString());
    } catch (Exception e) {
      return UpdateScope.NONE;
    }
  }
}
