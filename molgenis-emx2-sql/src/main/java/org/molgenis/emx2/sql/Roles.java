package org.molgenis.emx2.sql;

import org.jooq.DSLContext;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;

public class Roles {
  static void executeCreateRole(DSLContext jooq, String role) {
    jooq.execute(
        "DO $$\n"
            + "BEGIN\n"
            + "    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = {0}) THEN\n"
            + "        CREATE ROLE {1};\n"
            + "    END IF;\n"
            + "END\n"
            + "$$;\n",
        inline(role), name(role));
  }
}
