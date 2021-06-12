package org.molgenis.emx2.sql;

public class AToolToCleanDatabase {

  public static void main(String[] args) {
    TestDatabaseFactory.deleteAll();
  }
}
