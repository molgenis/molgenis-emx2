package org.molgenis.emx2.sql.resolvers.validators;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

import java.util.Map;

public interface RowValidator {

  void apply(Map<String, Object> context, Column columns, Row row) throws MolgenisException;
}
