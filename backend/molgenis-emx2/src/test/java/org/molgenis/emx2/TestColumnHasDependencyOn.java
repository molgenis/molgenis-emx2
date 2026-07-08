package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;

import org.junit.jupiter.api.Test;

class TestColumnHasDependencyOn {

  @Test
  void returnsFalseWhenNoExpressionsAreSet() {
    Column a = column("a");
    Column b = column("b");
    assertFalse(a.hasDependencyOn(b));
  }

  @Test
  void detectsDependencyViaComputedExpression() {
    Column a = column("a").setComputed("b + 1");
    Column b = column("b");
    assertTrue(a.hasDependencyOn(b));
  }

  @Test
  void detectsDependencyViaDefaultValue() {
    Column a = column("a").setDefaultValue("=b * 2");
    Column b = column("b");
    assertTrue(a.hasDependencyOn(b));
  }

  @Test
  void detectsDependencyViaRequired() {
    Column a = column("a").setRequired("b != null");
    Column b = column("b");
    assertTrue(a.hasDependencyOn(b));
  }

  @Test
  void detectsDependencyViaValidation() {
    Column a = column("a").setValidation("a > b");
    Column b = column("b");
    assertTrue(a.hasDependencyOn(b));
  }

  @Test
  void returnsFalseWhenComputedDoesNotReferenceColumn() {
    Column a = column("a").setComputed("c + 1");
    Column b = column("b");
    assertFalse(a.hasDependencyOn(b));
  }

  @Test
  void dependencyIsNotSymmetric() {
    Column a = column("a").setComputed("b + 1");
    Column b = column("b");
    assertTrue(a.hasDependencyOn(b));
    assertFalse(b.hasDependencyOn(a));
  }

  @Test
  void returnsTrueWhenAnyExpressionReferencesDependency() {
    Column a = column("a").setComputed("c + 1").setValidation("a > b");
    Column b = column("b");
    Column c = column("c");
    assertTrue(a.hasDependencyOn(b));
    assertTrue(a.hasDependencyOn(c));
  }
}
