package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.utils.JavaScriptParser.getReferencedVariables;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TestJavaScriptParser {

  @Test
  void findsVariablesThatAreRead() {
    assertEquals(Set.of("nameComputed"), getReferencedVariables("nameComputed == 'Piet'"));
  }

  @Test
  void findsVariablesInTemplateLiterals() {
    assertEquals(
        Set.of("firstName", "lastName"), getReferencedVariables("`${firstName} ${lastName}`"));
  }

  @Test
  void ignoresTextInStringLiterals() {
    assertEquals(Set.of(), getReferencedVariables("'otherNames is only a string here'"));
  }

  @Test
  void ignoresTextInComments() {
    assertEquals(Set.of("name"), getReferencedVariables("// otherNames is commented out\nname"));
  }

  @Test
  void ignoresTextInRegularExpressions() {
    assertEquals(Set.of("name"), getReferencedVariables("name.match(/otherNames/) != null"));
  }

  @Test
  void ignoresPropertiesThatAreReadFromAVariable() {
    assertEquals(Set.of("myRef"), getReferencedVariables("myRef.name"));
  }

  @Test
  void ignoresKeysOfAnObjectLiteral() {
    assertEquals(Set.of("name"), getReferencedVariables("({otherNames: name})"));
  }

  @Test
  void ignoresVariablesDeclaredInTheScript() {
    assertEquals(Set.of("a", "b"), getReferencedVariables("var x = a; x + b"));
  }

  @Test
  void ignoresParametersOfArrowFunctions() {
    assertEquals(
        Set.of("myList"), getReferencedVariables("myList.filter(name => name.length > 0)"));
  }

  @Test
  void ignoresParametersOfFunctions() {
    assertEquals(
        Set.of("myList"), getReferencedVariables("myList.map(function (name) { return name; })"));
  }

  @Test
  void returnsEmptyForLiteralsWithoutVariables() {
    assertEquals(Set.of(), getReferencedVariables("true"));
  }

  @Test
  void returnsEmptyForEmptyScript() {
    assertEquals(Set.of(), getReferencedVariables(null));
    assertEquals(Set.of(), getReferencedVariables("  "));
  }

  @Test
  void returnsEmptyForScriptThatCannotBeParsed() {
    assertEquals(Set.of(), getReferencedVariables("this is not javascript !!"));
  }
}
