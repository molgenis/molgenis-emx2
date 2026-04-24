package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TablePermission.Scope;
import org.molgenis.emx2.sql.SqlRoleManager;

public class GraphqlPermissionFieldFactory {

  static final GraphQLEnumType scopeEnumType;

  static {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name("MolgenisEditScope");
    for (Scope s : Scope.values()) {
      builder.value(s.name(), s);
    }
    scopeEnumType = builder.build();
  }

  static final GraphQLEnumType selectScopeEnumType;

  static {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name("MolgenisSelectScope");
    for (TablePermission.ViewMode vm : TablePermission.ViewMode.values()) {
      builder.value(vm.name(), vm);
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
          .field(GraphQLFieldDefinition.newFieldDefinition().name(SELECT).type(scopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(scopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(scopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(scopeEnumType))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("share")
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
          .field(GraphQLFieldDefinition.newFieldDefinition().name(SELECT).type(scopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(scopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(scopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(scopeEnumType))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("share")
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
          .field(GraphQLInputObjectField.newInputObjectField().name(SELECT).type(scopeEnumType))
          .field(GraphQLInputObjectField.newInputObjectField().name(INSERT).type(scopeEnumType))
          .field(GraphQLInputObjectField.newInputObjectField().name(UPDATE).type(scopeEnumType))
          .field(GraphQLInputObjectField.newInputObjectField().name(DELETE).type(scopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("share")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("selectScope")
                  .type(selectScopeEnumType))
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
      map.put(SELECT, p.select());
      map.put(INSERT, p.insert());
      map.put(UPDATE, p.update());
      map.put(DELETE, p.delete());
      map.put("changeOwner", p.changeOwner());
      map.put("share", p.share());
      result.add(map);
    }
    return result;
  }

  static void applyRoles(SqlRoleManager rm, List<Map<String, Object>> roles) {
    if (roles == null) return;
    for (Map<String, Object> roleMap : roles) {
      String name = (String) roleMap.get(NAME);
      String description = (String) roleMap.get(DESCRIPTION);
      rm.createOrUpdateRole(name, description);
      List<Map<String, Object>> perms = (List<Map<String, Object>>) roleMap.get(PERMISSIONS);
      if (perms != null) {
        applyPermissionsForRole(rm, name, perms);
      }
    }
  }

  private static void applyPermissionsForRole(
      SqlRoleManager rm, String role, List<Map<String, Object>> perms) {
    PermissionSet ps = new PermissionSet();
    for (Map<String, Object> pMap : perms) {
      Object selectScopeRaw = pMap.get("selectScope");
      if (selectScopeRaw != null) {
        TablePermission.ViewMode.fromString(selectScopeRaw.toString());
      }
      ps.put(
          new TablePermission(
              (String) pMap.get("schema"),
              (String) pMap.get("table"),
              toScope(pMap.get(SELECT)),
              toScope(pMap.get(INSERT)),
              toScope(pMap.get(UPDATE)),
              toScope(pMap.get(DELETE)),
              Boolean.TRUE.equals(pMap.get("changeOwner")),
              Boolean.TRUE.equals(pMap.get("share"))));
    }
    rm.setPermissions(role, ps);
  }

  static void applyMembers(SqlRoleManager rm, List<Map<String, Object>> members) {
    if (members == null) return;
    for (Map<String, Object> member : members) {
      rm.grantRoleToUser((String) member.get("role"), (String) member.get("user"));
    }
  }

  static Scope toScope(Object value) {
    if (value == null) return Scope.NONE;
    if (value instanceof Scope s) return s;
    try {
      return Scope.fromString(value.toString());
    } catch (Exception e) {
      return Scope.NONE;
    }
  }
}
