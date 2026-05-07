package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.TABLE;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.schema.GraphQLEnumValueDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

class TestGraphqlPermissionFieldFactory {

  @Test
  void selectScopeEnumType_declaresAllEightValues() {
    Set<String> enumValues =
        GraphqlPermissionFieldFactory.selectScopeEnumType.getValues().stream()
            .map(GraphQLEnumValueDefinition::getName)
            .collect(Collectors.toSet());

    for (SelectScope scope : SelectScope.values()) {
      assertTrue(enumValues.contains(scope.name()), "Missing enum value: " + scope.name());
    }
    assertEquals(SelectScope.values().length, enumValues.size());
  }

  @Test
  void updateScopeEnumType_declaresFourValues() {
    Set<String> enumValues =
        GraphqlPermissionFieldFactory.updateScopeEnumType.getValues().stream()
            .map(GraphQLEnumValueDefinition::getName)
            .collect(Collectors.toSet());

    for (UpdateScope scope : UpdateScope.values()) {
      assertTrue(enumValues.contains(scope.name()), "Missing enum value: " + scope.name());
    }
    assertEquals(UpdateScope.values().length, enumValues.size());
  }

  @Test
  void toPermissionSet_roundTrip_preservesAllVerbScopes() {
    Map<String, Object> input =
        Map.of(
            TABLES,
            List.of(
                Map.of(
                    TABLE, "Pet",
                    SELECT, SelectScope.ALL,
                    INSERT, UpdateScope.OWN,
                    UPDATE, UpdateScope.GROUP,
                    DELETE, UpdateScope.NONE),
                Map.of(
                    TABLE, "Order",
                    SELECT, SelectScope.AGGREGATE,
                    INSERT, UpdateScope.NONE,
                    UPDATE, UpdateScope.NONE,
                    DELETE, UpdateScope.NONE)),
            "changeOwner",
            true,
            "changeGroup",
            false);

    PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(input);

    assertTrue(ps.isChangeOwner());
    assertFalse(ps.isChangeGroup());

    PermissionSet.TablePermissions pet = ps.getTables().get("Pet");
    assertNotNull(pet);
    assertEquals(SelectScope.ALL, pet.getSelect());
    assertEquals(UpdateScope.OWN, pet.getInsert());
    assertEquals(UpdateScope.GROUP, pet.getUpdate());
    assertEquals(UpdateScope.NONE, pet.getDelete());

    PermissionSet.TablePermissions order = ps.getTables().get("Order");
    assertNotNull(order);
    assertEquals(SelectScope.AGGREGATE, order.getSelect());
    assertEquals(UpdateScope.NONE, order.getInsert());
    assertEquals(UpdateScope.NONE, order.getUpdate());
    assertEquals(UpdateScope.NONE, order.getDelete());
  }

  @Test
  void toPermissionSet_ownOnSelect_succeeds() {
    Map<String, Object> input =
        Map.of(TABLES, List.of(Map.of(TABLE, "Pet", SELECT, SelectScope.OWN)));

    PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(input);
    assertEquals(SelectScope.OWN, ps.getTables().get("Pet").getSelect());
  }

  @Test
  void toPermissionSet_nullInput_returnsEmptyPermissionSet() {
    PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(null);

    assertNotNull(ps);
    assertTrue(ps.getTables().isEmpty());
    assertFalse(ps.isChangeOwner());
    assertFalse(ps.isChangeGroup());
  }

  @Test
  void toPermissionSet_missingVerbFields_defaultToNone() {
    Map<String, Object> input = Map.of(TABLES, List.of(Map.of(TABLE, "Pet")));

    PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(input);
    PermissionSet.TablePermissions pet = ps.getTables().get("Pet");

    assertNotNull(pet);
    assertEquals(SelectScope.NONE, pet.getSelect());
    assertEquals(UpdateScope.NONE, pet.getInsert());
    assertEquals(UpdateScope.NONE, pet.getUpdate());
    assertEquals(UpdateScope.NONE, pet.getDelete());
    assertFalse(ps.isChangeOwner());
    assertFalse(ps.isChangeGroup());
  }

  @Test
  void toSelectScope_nullValue_returnsNone() {
    assertEquals(SelectScope.NONE, GraphqlPermissionFieldFactory.toSelectScope(null));
  }

  @Test
  void toSelectScope_enumValue_returnsDirectly() {
    assertEquals(SelectScope.GROUP, GraphqlPermissionFieldFactory.toSelectScope(SelectScope.GROUP));
  }

  @Test
  void toSelectScope_stringValue_parsesCorrectly() {
    assertEquals(SelectScope.AGGREGATE, GraphqlPermissionFieldFactory.toSelectScope("AGGREGATE"));
  }

  @Test
  void toUpdateScope_nullValue_returnsNone() {
    assertEquals(UpdateScope.NONE, GraphqlPermissionFieldFactory.toUpdateScope(null));
  }

  @Test
  void toUpdateScope_enumValue_returnsDirectly() {
    assertEquals(UpdateScope.OWN, GraphqlPermissionFieldFactory.toUpdateScope(UpdateScope.OWN));
  }

  @Test
  void toUpdateScope_stringValue_parsesCorrectly() {
    assertEquals(UpdateScope.GROUP, GraphqlPermissionFieldFactory.toUpdateScope("GROUP"));
  }

  @Test
  void permissionSetToMap_includesSchemaName() {
    PermissionSet ps = new PermissionSet().setDescription("test role");
    Map<String, Object> map =
        GraphqlPermissionFieldFactory.permissionSetToMap("myRole", "mySchema", false, ps);

    assertEquals("myRole", map.get("name"));
    assertEquals("mySchema", map.get("schemaName"));
    assertEquals("test role", map.get("description"));
  }
}
