package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class TestNameValidation {
  @Test
  public void testNameValidation() {
    // should succeed
    Column c = new Column("first name");
    // '_ ' in name should fail
    try {
      c = new Column("first_  name");
      fail("should fail because '_ ' is not allowed");
    } catch (Exception e) {
      // error correct
    }

    // '_ ' in name should fail
    try {
      c = new Column("first  _  name");
      fail("should fail because ' _ ' is not allowed");
    } catch (Exception e) {
      // error correct
    }

    // '_ ' in name should fail
    try {
      c = new Column("first  __   name");
      fail("should fail because ' _ ' is not allowed");
    } catch (Exception e) {
      // error correct
    }

    // '_ ' in name should fail
    try {
      c = new Column("first   _name");
      fail("should fail because ' _ ' is not allowed");
    } catch (Exception e) {
      // error correct
    }

    // '_ ' in name should fail
    try {
      c = new Column("aa    ____      ");
      fail("should fail because ' _ ' is not allowed");
    } catch (Exception e) {
      // error correct
    }

    // should succeed
    TableMetadata t = new TableMetadata("first name");
    // '_ ' in name should fail
    try {
      t = new TableMetadata("first_  name");
      fail("should fail because '_ ' is not allowed");
    } catch (Exception e) {
      // error correct
    }
  }
}
