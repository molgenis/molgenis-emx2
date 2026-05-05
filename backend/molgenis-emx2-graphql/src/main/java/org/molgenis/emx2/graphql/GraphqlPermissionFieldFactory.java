package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.TABLE;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SelectScope;

public class GraphqlPermissionFieldFactory {

  private static final Set<SelectScope> VIEW_MODE_SCOPES =
      EnumSet.of(SelectScope.EXISTS, SelectScope.COUNT, SelectScope.RANGE, SelectScope.AGGREGATE);

  static final GraphQLEnumType selectScopeEnumType;

  static {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name("MolgenisSelectScope");
    for (SelectScope scope : SelectScope.values()) {
      builder.value(scope.name(), scope);
    }
    selectScopeEnumType = builder.build();
  }

  static final GraphQLInputObjectType tablePermissionInputType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisTablePermissionInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TABLE)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(SELECT).type(selectScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(INSERT).type(selectScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(UPDATE).type(selectScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(DELETE).type(selectScopeEnumType))
          .build();

  static final GraphQLInputObjectType inputRoleType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisRoleInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(NAME)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TABLES)
                  .type(GraphQLList.list(tablePermissionInputType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("changeGroup")
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLObjectType tablePermissionOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisTablePermissionOutput")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(TABLE).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(SELECT).type(selectScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(selectScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(selectScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(selectScopeEnumType))
          .build();

  static final GraphQLObjectType roleOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisRoleOutput")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TABLES)
                  .type(GraphQLList.list(tablePermissionOutputType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeOwner")
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("changeGroup")
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLObjectType groupOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisGroupOutput")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(USERS)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .build();

  static SelectScope toSelectScope(Object graphqlValue) {
    if (graphqlValue == null) {
      return SelectScope.NONE;
    }
    if (graphqlValue instanceof SelectScope scope) {
      return scope;
    }
    try {
      return SelectScope.fromString(graphqlValue.toString());
    } catch (MolgenisException ignored) {
      return SelectScope.NONE;
    }
  }

  @SuppressWarnings("unchecked")
  static PermissionSet toPermissionSet(Map<String, Object> input) {
    PermissionSet ps = new PermissionSet();
    if (input == null) {
      return ps;
    }

    boolean changeOwner = Boolean.TRUE.equals(input.get("changeOwner"));
    boolean changeGroup = Boolean.TRUE.equals(input.get("changeGroup"));
    ps.setChangeOwner(changeOwner);
    ps.setChangeGroup(changeGroup);

    Object tablesValue = input.get(TABLES);
    if (!(tablesValue instanceof java.util.List<?> tableList)) {
      return ps;
    }

    for (Object entry : tableList) {
      if (!(entry instanceof Map<?, ?> rawMap)) {
        continue;
      }
      Map<String, Object> tableMap = (Map<String, Object>) rawMap;
      String tableName = (String) tableMap.get(TABLE);
      if (tableName == null) {
        continue;
      }

      SelectScope selectScope = toSelectScope(tableMap.get(SELECT));
      SelectScope insertScope = toWriteScope(tableMap.get(INSERT));
      SelectScope updateScope = toWriteScope(tableMap.get(UPDATE));
      SelectScope deleteScope = toWriteScope(tableMap.get(DELETE));

      PermissionSet.TablePermissions perms =
          new PermissionSet.TablePermissions()
              .setSelect(selectScope)
              .setInsert(insertScope)
              .setUpdate(updateScope)
              .setDelete(deleteScope);

      ps.putTable(tableName, perms);
    }

    return ps;
  }

  private static SelectScope toWriteScope(Object graphqlValue) {
    SelectScope scope = toSelectScope(graphqlValue);
    if (VIEW_MODE_SCOPES.contains(scope)) {
      throw new MolgenisException(
          "Scope "
              + scope.name()
              + " is not valid for insert/update/delete; allowed: NONE, OWN, GROUP, ALL");
    }
    return scope;
  }

  static Map<String, Object> permissionSetToMap(String roleName, PermissionSet ps) {
    Map<String, Object> roleMap = new LinkedHashMap<>();
    roleMap.put(NAME, roleName);
    roleMap.put("changeOwner", ps.isChangeOwner());
    roleMap.put("changeGroup", ps.isChangeGroup());
    List<Map<String, Object>> tableList =
        ps.getTables().entrySet().stream()
            .map(
                entry -> {
                  Map<String, Object> tableMap = new LinkedHashMap<>();
                  tableMap.put(TABLE, entry.getKey());
                  tableMap.put(SELECT, entry.getValue().getSelect().name());
                  tableMap.put(INSERT, entry.getValue().getInsert().name());
                  tableMap.put(UPDATE, entry.getValue().getUpdate().name());
                  tableMap.put(DELETE, entry.getValue().getDelete().name());
                  return tableMap;
                })
            .toList();
    roleMap.put(TABLES, tableList);
    return roleMap;
  }

  public static void requireManagerOrOwner(Database db, Schema schema) {
    if (db.isAdmin()) return;
    if (schema == null
        || (!schema.hasActiveUserRole(Privileges.MANAGER)
            && !schema.hasActiveUserRole(Privileges.OWNER))) {
      String schemaName = schema != null ? schema.getName() : "unknown";
      throw new MolgenisException(
          "Only Manager or Owner can grant custom roles on schema " + schemaName);
    }
  }

  private GraphqlPermissionFieldFactory() {}
}
