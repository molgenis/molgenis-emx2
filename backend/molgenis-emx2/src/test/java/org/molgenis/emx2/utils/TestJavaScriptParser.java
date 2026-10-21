package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.utils.JavaScriptParser.getReferencedPaths;
import static org.molgenis.emx2.utils.JavaScriptParser.getReferencedVariables;

import java.util.List;
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
  void stillFindsOuterVariableWhenSameNameIsAlsoALambdaParameterElsewhere() {
    // "price" is read directly (outer reference to column "price") AND, separately, used as the
    // arrow function's own parameter name inside items.map(...). The parameter only shadows
    // "price" within that lambda's body; it must not hide the unrelated outer read of "price".
    assertEquals(
        Set.of("price", "items"),
        getReferencedVariables("price + items.map(price => price.discount).length"));
  }

  @Test
  void stillFindsOuterVariableWhenSameNameIsAlsoDeclaredInABlock() {
    assertEquals(Set.of("price"), getReferencedVariables("{ let price = 1; } price"));
  }

  @Test
  void ignoresVariablesDeclaredAfterTheyAreUsed() {
    // 'var' is hoisted, so x refers to the declaration below it and not to a column
    assertEquals(Set.of("a", "b"), getReferencedVariables("x + b; var x = a"));
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

  @Test
  void findsPathsOfPropertiesReadFromAVariable() {
    assertEquals(
        Set.of(List.of("doctor", "hospital"), List.of("doctor", "hospital", "city")),
        getReferencedPaths("doctor.hospital.city"));
  }

  @Test
  void findsPathsInTemplateLiterals() {
    assertEquals(
        Set.of(List.of("doctor", "name"), List.of("hospital", "city")),
        getReferencedPaths("`${doctor.name} in ${hospital.city}`"));
  }

  @Test
  void findsPathsThroughArrayIndexes() {
    assertEquals(Set.of(List.of("doctors", "name")), getReferencedPaths("doctors[0].name"));
  }

  @Test
  void findsPathsOfMethodsCalledOnAVariable() {
    assertEquals(Set.of(List.of("doctors", "map")), getReferencedPaths("doctors.map(d => d.name)"));
  }

  @Test
  void ignoresPathsOnVariablesDeclaredInTheExpressionItself() {
    assertEquals(Set.of(), getReferencedPaths("[1, 2].map(doctor => doctor.name)"));
  }

  @Test
  void ignoresVariablesWithoutProperties() {
    assertEquals(Set.of(), getReferencedPaths("doctor != null"));
  }

  @Test
  void returnsNoPathsForScriptThatCannotBeParsed() {
    assertEquals(Set.of(), getReferencedPaths("this is not javascript !!"));
  }
}
