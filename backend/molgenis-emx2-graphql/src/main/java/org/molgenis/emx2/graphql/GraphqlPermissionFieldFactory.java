package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.Permission.EditScope;
import org.molgenis.emx2.Permission.ViewScope;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Role;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.SqlRoleManager;

public class GraphqlPermissionFieldFactory {

  static final GraphQLEnumType viewScopeEnumType;

  static {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name("MolgenisViewScope");
    for (ViewScope vs : ViewScope.values()) {
      builder.value(vs.name(), vs);
    }
    viewScopeEnumType = builder.build();
  }

  static final GraphQLEnumType editScopeEnumType =
      GraphQLEnumType.newEnum()
          .name("MolgenisEditScope")
          .value("NONE", EditScope.NONE)
          .value("OWN", EditScope.OWN)
          .value("GROUP", EditScope.GROUP)
          .value("ALL", EditScope.ALL)
          .build();

  static final GraphQLObjectType effectivePermissionType =
      GraphQLObjectType.newObject()
          .name("MolgenisEffectivePermission")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("schema")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("table").type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(SELECT).type(viewScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(editScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(editScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(editScopeEnumType))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("share")
                  .type(Scalars.GraphQLBoolean))
          .build();

  private static final GraphQLObjectType rolePermissionsOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisRolePermissionsOutput")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("schema")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("table").type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(SELECT).type(viewScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(editScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(editScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(editScopeEnumType))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("share")
                  .type(Scalars.GraphQLBoolean))
          .build();

  private static final GraphQLObjectType roleOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisRoleOutput")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("role").type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("description")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("immutable")
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

  private static final GraphQLObjectType permissionQueryOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisPermissionQuery")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(ROLES)
                  .type(GraphQLList.list(roleOutputType)))
          .build();

  private static final GraphQLInputObjectType inputCustomRoleType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisCustomRoleInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .build();

  private static final GraphQLInputObjectType inputPermissionFgType =
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
          .field(GraphQLInputObjectField.newInputObjectField().name(SELECT).type(viewScopeEnumType))
          .field(GraphQLInputObjectField.newInputObjectField().name(INSERT).type(editScopeEnumType))
          .field(GraphQLInputObjectField.newInputObjectField().name(UPDATE).type(editScopeEnumType))
          .field(GraphQLInputObjectField.newInputObjectField().name(DELETE).type(editScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("share")
                  .type(Scalars.GraphQLBoolean))
          .build();

  private static final GraphQLInputObjectType inputRolePermissionsType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisRolePermissionsInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("role")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(PERMISSIONS)
                  .type(GraphQLList.list(inputPermissionFgType)))
          .build();

  private static final GraphQLInputObjectType inputRoleMemberType =
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

  private static final GraphQLInputObjectType inputTableRlsType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisTableRlsInput")
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
                  .name("rowLevelSecurity")
                  .type(Scalars.GraphQLBoolean))
          .build();

  public GraphqlPermissionFieldFactory() {}

  public GraphQLFieldDefinition permissionQueryField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("permission")
        .type(permissionQueryOutputType)
        .dataFetcher(
            env -> {
              if (!database.isAdmin()) {
                return Map.of(ROLES, List.of());
              }
              SqlRoleManager rm = (SqlRoleManager) database.getRoleManager();
              List<Role> roles = rm.listRoles();
              List<Map<String, Object>> roleList = new ArrayList<>();
              for (Role meta : roles) {
                Map<String, Object> roleMap = new LinkedHashMap<>();
                roleMap.put("role", meta.getRoleName());
                roleMap.put("description", meta.getDescription());
                roleMap.put("immutable", meta.isImmutable());
                roleMap.put(PERMISSIONS, permissionsToMaps(rm, meta.getRoleName()));
                roleMap.put(MEMBERS, rm.getMembersForRole(meta.getRoleName()));
                roleList.add(roleMap);
              }
              return Map.of(ROLES, roleList);
            })
        .build();
  }

  public GraphQLFieldDefinition changePermissionsMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("changePermissions")
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument().name(ROLES).type(GraphQLList.list(inputCustomRoleType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(PERMISSIONS)
                .type(GraphQLList.list(inputRolePermissionsType)))
        .argument(
            GraphQLArgument.newArgument().name(MEMBERS).type(GraphQLList.list(inputRoleMemberType)))
        .argument(
            GraphQLArgument.newArgument().name("tables").type(GraphQLList.list(inputTableRlsType)))
        .dataFetcher(
            env -> {
              if (!database.isAdmin()) {
                return new GraphqlApiMutationResult(FAILED, "admin only");
              }
              try {
                database.tx(
                    db -> {
                      SqlRoleManager rm = ((org.molgenis.emx2.sql.SqlDatabase) db).getRoleManager();
                      applyRoles(rm, env.getArgument(ROLES));
                      applyTables(db, env.getArgument("tables"));
                      applyPermissions(rm, env.getArgument(PERMISSIONS));
                      applyMembers(rm, env.getArgument(MEMBERS));
                    });
                return new GraphqlApiMutationResult(SUCCESS, "Permissions updated");
              } catch (MolgenisException ex) {
                return new GraphqlApiMutationResult(FAILED, ex.getMessage());
              }
            })
        .build();
  }

  public GraphQLFieldDefinition dropPermissionsMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("dropPermissions")
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument().name(ROLES).type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(
            GraphQLArgument.newArgument().name(MEMBERS).type(GraphQLList.list(inputRoleMemberType)))
        .dataFetcher(
            env -> {
              if (!database.isAdmin()) {
                return new GraphqlApiMutationResult(FAILED, "admin only");
              }
              try {
                database.tx(
                    db -> {
                      SqlRoleManager rm = ((org.molgenis.emx2.sql.SqlDatabase) db).getRoleManager();
                      List<String> rolesToDrop = env.getArgument(ROLES);
                      if (rolesToDrop != null) {
                        for (String role : rolesToDrop) {
                          rm.deleteRole(role);
                        }
                      }
                      List<Map<String, Object>> membersToDrop = env.getArgument(MEMBERS);
                      if (membersToDrop != null) {
                        for (Map<String, Object> member : membersToDrop) {
                          rm.revokeRoleFromUser(
                              (String) member.get("role"), (String) member.get("user"));
                        }
                      }
                    });
                return new GraphqlApiMutationResult(SUCCESS, "Permissions updated");
              } catch (MolgenisException ex) {
                return new GraphqlApiMutationResult(FAILED, ex.getMessage());
              }
            })
        .build();
  }

  private static List<Map<String, Object>> permissionsToMaps(SqlRoleManager rm, String role) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Permission p : rm.getPermissions(role)) {
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

  private static void applyRoles(SqlRoleManager rm, List<Map<String, Object>> roles) {
    if (roles == null) return;
    for (Map<String, Object> roleMap : roles) {
      String name = (String) roleMap.get(NAME);
      String description = (String) roleMap.get(DESCRIPTION);
      rm.createOrUpdateRole(name, description);
    }
  }

  private static void applyPermissions(
      SqlRoleManager rm, List<Map<String, Object>> permissionsList) {
    if (permissionsList == null) return;
    for (Map<String, Object> entry : permissionsList) {
      String role = (String) entry.get("role");
      List<Map<String, Object>> perms = (List<Map<String, Object>>) entry.get(PERMISSIONS);
      if (perms == null) continue;
      PermissionSet ps = new PermissionSet();
      for (Map<String, Object> pMap : perms) {
        ps.put(
            new Permission(
                (String) pMap.get("schema"),
                (String) pMap.get("table"),
                toViewScope(pMap.get(SELECT)),
                toEditScope(pMap.get(INSERT)),
                toEditScope(pMap.get(UPDATE)),
                toEditScope(pMap.get(DELETE)),
                Boolean.TRUE.equals(pMap.get("changeOwner")),
                Boolean.TRUE.equals(pMap.get("share"))));
      }
      rm.setPermissions(role, ps);
    }
  }

  private static void applyMembers(SqlRoleManager rm, List<Map<String, Object>> members) {
    if (members == null) return;
    for (Map<String, Object> member : members) {
      rm.grantRoleToUser((String) member.get("role"), (String) member.get("user"));
    }
  }

  private static void applyTables(Database db, List<Map<String, Object>> tables) {
    if (tables == null) return;
    for (Map<String, Object> tableEntry : tables) {
      String schemaName = (String) tableEntry.get("schema");
      String tableName = (String) tableEntry.get("table");
      boolean rls = Boolean.TRUE.equals(tableEntry.get("rowLevelSecurity"));
      TableMetadata tableMetadata = db.getSchema(schemaName).getTable(tableName).getMetadata();
      tableMetadata.setRowLevelSecurity(rls);
    }
  }

  private static ViewScope toViewScope(Object value) {
    if (value == null) return ViewScope.NONE;
    if (value instanceof ViewScope vs) return vs;
    try {
      return ViewScope.valueOf(value.toString());
    } catch (IllegalArgumentException e) {
      return ViewScope.NONE;
    }
  }

  private static EditScope toEditScope(Object value) {
    if (value == null) return EditScope.NONE;
    if (value instanceof EditScope es) return es;
    return switch (value.toString()) {
      case "OWN" -> EditScope.OWN;
      case "GROUP" -> EditScope.GROUP;
      case "ALL" -> EditScope.ALL;
      default -> EditScope.NONE;
    };
  }
}
