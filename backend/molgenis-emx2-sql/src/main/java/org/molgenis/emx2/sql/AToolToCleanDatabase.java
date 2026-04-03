package org.molgenis.emx2.sql;

import java.io.IOException;
import java.util.Objects;
import org.jooq.DSLContext;
import org.molgenis.emx2.MolgenisException;

public class AToolToCleanDatabase {

  public static void main(String[] args) {
    deleteAll();
  }

  public static void deleteAll() {
    SqlDatabase db = new SqlDatabase(true);
    DSLContext jooq = db.getJooq();
    db.becomeAdmin();
    try {
      String sql =
          new String(
              Objects.requireNonNull(
                      AToolToCleanDatabase.class.getResourceAsStream(
                          "utility-sql/clean-molgenis-database.sql"))
                  .readAllBytes());
      jooq.execute(sql);
    } catch (IOException e) {
      throw new MolgenisException("Clean database failed", e);
    }

    MetadataUtils.resetVersion();
    new SqlDatabase(true);
  }
}
