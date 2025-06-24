package org.molgenis.emx2.sql;

import org.jooq.DSLContext;

public interface JooqTransaction {
  void run(DSLContext jooq);
}
