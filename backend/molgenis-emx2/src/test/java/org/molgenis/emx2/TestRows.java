package org.molgenis.emx2;

import org.junit.Test;

import static org.junit.Assert.fail;

public class TestRows {
  @Test
  public void test1() {

    try {
      new Row(1, 2);
      fail("should fail because column names must be string");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getTitle() + ": " + e.getMessage());
    }

    try {
      new Row(true, 2);
      fail("should fail because column names must be string");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getTitle() + ": " + e.getMessage());
    }

    try {
      new Row(true, 2, 3);
      fail("should fail because name value pairs must be even number of parameters");
    } catch (MolgenisException e) {
      System.out.println("Error correct: " + e.getTitle() + ": " + e.getMessage());
    }

    new Row("col1", 1, "col2", 2);
  }
}
