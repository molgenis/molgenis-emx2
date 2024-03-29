package org.molgenis.emx2.sql;

/* used by gradle*/
public class InitDatabase {
  public static void main(String[] args) {
    System.out.println("INITIALIZING DATABASE");
    new SqlDatabase(true);
  }
}
