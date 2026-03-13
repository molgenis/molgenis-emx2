package org.molgenis.emx2.utils.generator;

import org.jooq.Field;

public interface DSLIdGenerator {

  Field<String> generateId();
}
