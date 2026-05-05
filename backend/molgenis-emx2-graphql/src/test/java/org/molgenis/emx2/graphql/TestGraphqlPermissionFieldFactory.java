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
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.SelectScope;

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
  void toPermissionSet_roundTrip_preservesAllVerbScopes() {
    Map<String, Object> input =
        Map.of(
            TABLES,
            List.of(
                Map.of(
                    TABLE, "Pet",
                    SELECT, SelectScope.ALL,
                    INSERT, SelectScope.OWN,
                    UPDATE, SelectScope.GROUP,
                    DELETE, SelectScope.NONE),
                Map.of(
                    TABLE, "Order",
                    SELECT, SelectScope.AGGREGATE,
                    INSERT, SelectScope.NONE,
                    UPDATE, SelectScope.NONE,
                    DELETE, SelectScope.NONE)),
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
    assertEquals(SelectScope.OWN, pet.getInsert());
    assertEquals(SelectScope.GROUP, pet.getUpdate());
    assertEquals(SelectScope.NONE, pet.getDelete());

    PermissionSet.TablePermissions order = ps.getTables().get("Order");
    assertNotNull(order);
    assertEquals(SelectScope.AGGREGATE, order.getSelect());
    assertEquals(SelectScope.NONE, order.getInsert());
    assertEquals(SelectScope.NONE, order.getUpdate());
    assertEquals(SelectScope.NONE, order.getDelete());
  }

  @Test
  void toPermissionSet_viewModeOnInsert_throwsMolgenisException() {
    Map<String, Object> input =
        Map.of(TABLES, List.of(Map.of(TABLE, "Pet", INSERT, SelectScope.EXISTS)));

    assertThrows(
        MolgenisException.class, () -> GraphqlPermissionFieldFactory.toPermissionSet(input));
  }

  @Test
  void toPermissionSet_viewModeOnUpdate_throwsMolgenisException() {
    Map<String, Object> input =
        Map.of(TABLES, List.of(Map.of(TABLE, "Pet", UPDATE, SelectScope.COUNT)));

    assertThrows(
        MolgenisException.class, () -> GraphqlPermissionFieldFactory.toPermissionSet(input));
  }

  @Test
  void toPermissionSet_viewModeOnDelete_throwsMolgenisException() {
    Map<String, Object> input =
        Map.of(TABLES, List.of(Map.of(TABLE, "Pet", DELETE, SelectScope.RANGE)));

    assertThrows(
        MolgenisException.class, () -> GraphqlPermissionFieldFactory.toPermissionSet(input));
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
    assertEquals(SelectScope.NONE, pet.getInsert());
    assertEquals(SelectScope.NONE, pet.getUpdate());
    assertEquals(SelectScope.NONE, pet.getDelete());
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
}
