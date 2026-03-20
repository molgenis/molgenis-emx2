package org.molgenis.emx2.sql.autoid;

import org.jooq.Field;

public interface DSLIdGenerator {

  Field<String> generateId();
}
