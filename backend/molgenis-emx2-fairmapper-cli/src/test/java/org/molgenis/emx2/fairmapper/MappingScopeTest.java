package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MappingScopeTest {

  @Test
  void testRootScope() {
    MappingScope scope = new MappingScope();
    scope.put("var1", "value1");
    scope.put("var2", 42);

    assertEquals("value1", scope.get("var1"));
    assertEquals(42, scope.get("var2"));
    assertNull(scope.get("nonexistent"));
  }

  @Test
  void testChildScope() {
    MappingScope parent = new MappingScope();
    parent.put("parentVar", "parentValue");

    MappingScope child = parent.child();
    child.put("childVar", "childValue");

    assertEquals("parentValue", child.get("parentVar"));
    assertEquals("childValue", child.get("childVar"));

    assertNull(parent.get("childVar"));
  }

  @Test
  void testShadowing() {
    MappingScope parent = new MappingScope();
    parent.put("var", "parentValue");

    MappingScope child = parent.child();
    child.put("var", "childValue");

    assertEquals("childValue", child.get("var"));
    assertEquals("parentValue", parent.get("var"));
  }

  @Test
  void testMultipleLevels() {
    MappingScope root = new MappingScope();
    root.put("root", "rootValue");

    MappingScope level1 = root.child();
    level1.put("level1", "level1Value");

    MappingScope level2 = level1.child();
    level2.put("level2", "level2Value");

    assertEquals("rootValue", level2.get("root"));
    assertEquals("level1Value", level2.get("level1"));
    assertEquals("level2Value", level2.get("level2"));

    assertNull(root.get("level1"));
    assertNull(root.get("level2"));
    assertNull(level1.get("level2"));
  }

  @Test
  void testFlatten() {
    MappingScope parent = new MappingScope();
    parent.put("var1", "value1");
    parent.put("var2", "value2");

    MappingScope child = parent.child();
    child.put("var2", "overridden");
    child.put("var3", "value3");

    Map<String, Object> flattened = child.flatten();

    assertEquals(3, flattened.size());
    assertEquals("value1", flattened.get("var1"));
    assertEquals("overridden", flattened.get("var2"));
    assertEquals("value3", flattened.get("var3"));
  }

  @Test
  void testFlattenRoot() {
    MappingScope root = new MappingScope();
    root.put("var1", "value1");
    root.put("var2", "value2");

    Map<String, Object> flattened = root.flatten();

    assertEquals(2, flattened.size());
    assertEquals("value1", flattened.get("var1"));
    assertEquals("value2", flattened.get("var2"));
  }

  @Test
  void testNullValue() {
    MappingScope scope = new MappingScope();
    scope.put("nullVar", null);

    assertTrue(scope.flatten().containsKey("nullVar"));
    assertNull(scope.get("nullVar"));
  }

  @Test
  void testComplexValues() {
    MappingScope scope = new MappingScope();
    Map<String, Object> map = Map.of("key", "value");

    scope.put("map", map);
    scope.put("number", 123);
    scope.put("bool", true);

    assertEquals(map, scope.get("map"));
    assertEquals(123, scope.get("number"));
    assertEquals(true, scope.get("bool"));
  }
}
