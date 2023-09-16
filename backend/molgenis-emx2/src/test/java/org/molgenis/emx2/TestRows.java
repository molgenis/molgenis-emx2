package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class TestRows {
  @Test
  public void test1() {

    try {
      new Row("a", 2, 3, 4);
      fail("should fail because column names must be string");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getMessage());
    }

    try {
      new Row("a", 2, true, 2);
      fail("should fail because column names must be string");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getMessage());
    }

    try {
      new Row("a", 2, 3);
      fail("should fail because name value pairs must be even number of parameters");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getMessage());
    }

    new Row("col1", 1, "col2", 2);
  }
}
