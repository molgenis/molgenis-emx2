package org.molgenis.emx2.sql;

import java.io.IOException;
import java.util.Objects;
import org.jooq.DSLContext;
import org.molgenis.emx2.MolgenisException;

public class AToolToCleanDatabase {
  private static DSLContext jooq;

  public static void main(String[] args) {
    deleteAll();
  }

  public static void deleteAll() {
    SqlDatabase db = new SqlDatabase(true);
    jooq = db.getJooq();
    db.becomeAdmin();

    executeSqlStep("clean-db-remove-molgenis-schema.sql");
    executeSqlStep("clean-db-remove-foreign-keys.sql");
    executeSqlStep("clean-db-remove-user-schemas.sql");
    executeSqlStep("clean-db-remove-all-roles.sql");
    MetadataUtils.resetVersion();
    new SqlDatabase(true);
  }

  private static void executeSqlStep(String step) {
    try {
      String sql =
          new String(
              Objects.requireNonNull(
                      AToolToCleanDatabase.class.getResourceAsStream("utility-sql/" + step))
                  .readAllBytes());
      jooq.execute(sql);
    } catch (IOException e) {
      throw new MolgenisException("Clean database failed", e);
    }
  }
}
