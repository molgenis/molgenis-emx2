package org.molgenis.emx2;

import static org.jooq.impl.DSL.name;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.ForeignKey;
import org.jooq.Schema;
import org.jooq.Table;
import org.molgenis.emx2.sql.MetadataUtils;
import org.molgenis.emx2.sql.SqlDatabase;

public class AToolToCleanDatabase {
  private static DSLContext jooq;

  public static void main(String[] args) {
    deleteAll();
  }

  public static void deleteAll() {
    SqlDatabase db = new SqlDatabase(true);
    jooq = db.getJooq();
    db.becomeAdmin();
    jooq.dropSchemaIfExists("MOLGENIS").cascade().execute();
    deleteAllForeignKeyConstraints();
    deleteAllSchemas();
    deleteAllRoles();
    MetadataUtils.resetVersion();
    new SqlDatabase(true);
  }

  private static void deleteAllRoles() {
    for (String roleName : jooq.selectFrom(name("pg_roles")).fetchSet("rolname", String.class)) {
      if (roleName.startsWith("MG_")
          || roleName.startsWith("test")
          || roleName.startsWith("user_")) {
        String dbName = jooq.fetchOne("SELECT current_database()").get(0, String.class);
        jooq.execute(
            "REVOKE ALL PRIVILEGES ON DATABASE {0} FROM {1}", name(dbName), name(roleName));
        jooq.execute("DROP ROLE {0}", name(roleName));
      }
    }
  }

  private static void deleteAllSchemas() {
    for (Schema s : jooq.meta().getSchemas()) {
      String schemaName = s.getName();
      if (!schemaName.startsWith("pg_")
          && !"information_schema".equals(schemaName)
          && !"public".equals(schemaName)) {
        jooq.dropSchema(name(s.getName())).cascade().execute();
      }
    }
  }

  private static void deleteAllForeignKeyConstraints() {
    for (Schema s : jooq.meta().getSchemas()) {
      String name = s.getName();
      if (!name.startsWith("pg_") && !"information_schema".equals(name) && !"public".equals(name)) {
        for (Table t : s.getTables()) {
          for (ForeignKey k : (List<ForeignKey>) t.getReferences()) {
            jooq.alterTable(t).dropConstraint(k.getName()).execute();
          }
        }
      }
    }
  }
}
