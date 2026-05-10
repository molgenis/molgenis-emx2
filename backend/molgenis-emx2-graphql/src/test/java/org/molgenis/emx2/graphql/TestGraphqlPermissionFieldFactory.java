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
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.TablePermission;

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
            PERMISSIONS,
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

    TablePermission pet = ps.getTables().get("Pet");
    assertNotNull(pet);
    assertEquals(SelectScope.ALL, pet.select());
    assertEquals(UpdateScope.OWN, pet.insert());
    assertEquals(UpdateScope.GROUP, pet.update());
    assertEquals(UpdateScope.NONE, pet.delete());

    TablePermission order = ps.getTables().get("Order");
    assertNotNull(order);
    assertEquals(SelectScope.AGGREGATE, order.select());
    assertEquals(UpdateScope.NONE, order.insert());
    assertEquals(UpdateScope.NONE, order.update());
    assertEquals(UpdateScope.NONE, order.delete());
  }

  @Test
  void toPermissionSet_ownOnSelect_succeeds() {
    Map<String, Object> input =
        Map.of(PERMISSIONS, List.of(Map.of(TABLE, "Pet", SELECT, SelectScope.OWN)));

    PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(input);
    assertEquals(SelectScope.OWN, ps.getTables().get("Pet").select());
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
    Map<String, Object> input = Map.of(PERMISSIONS, List.of(Map.of(TABLE, "Pet")));

    PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(input);
    TablePermission pet = ps.getTables().get("Pet");

    assertNotNull(pet);
    assertEquals(SelectScope.NONE, pet.select());
    assertEquals(UpdateScope.NONE, pet.insert());
    assertEquals(UpdateScope.NONE, pet.update());
    assertEquals(UpdateScope.NONE, pet.delete());
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

  @Test
  void referenceScopeEnumType_declaresAllValues() {
    Set<String> enumValues =
        GraphqlPermissionFieldFactory.referenceScopeEnumType.getValues().stream()
            .map(GraphQLEnumValueDefinition::getName)
            .collect(Collectors.toSet());

    for (ReferenceScope scope : ReferenceScope.values()) {
      assertTrue(enumValues.contains(scope.name()), "Missing enum value: " + scope.name());
    }
    assertEquals(ReferenceScope.values().length, enumValues.size());
  }

  @Test
  void toReferenceScope_nullValue_returnsNone() {
    assertEquals(ReferenceScope.NONE, GraphqlPermissionFieldFactory.toReferenceScope(null));
  }

  @Test
  void toReferenceScope_enumValue_returnsDirectly() {
    assertEquals(
        ReferenceScope.ALL, GraphqlPermissionFieldFactory.toReferenceScope(ReferenceScope.ALL));
  }

  @Test
  void toReferenceScope_stringValue_parsesCorrectly() {
    assertEquals(ReferenceScope.GROUP, GraphqlPermissionFieldFactory.toReferenceScope("GROUP"));
  }

  @Test
  void toPermissionSet_referenceScope_roundTrips() {
    Map<String, Object> input =
        Map.of(PERMISSIONS, List.of(Map.of(TABLE, "Pet", REFERENCE, ReferenceScope.ALL)));

    PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(input);
    TablePermission pet = ps.getTables().get("Pet");

    assertNotNull(pet);
    assertEquals(ReferenceScope.ALL, pet.reference());
  }

  @Test
  void permissionSetToMap_emitsReferenceField() {
    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission("Pet").reference(ReferenceScope.OWN);
    ps.putTable("Pet", tp);

    Map<String, Object> map = GraphqlPermissionFieldFactory.permissionSetToMap("r", "s", false, ps);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tableList = (List<Map<String, Object>>) map.get(PERMISSIONS);
    assertEquals(1, tableList.size());
    assertEquals(ReferenceScope.OWN, tableList.get(0).get(REFERENCE));
  }
}
