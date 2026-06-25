package org.molgenis.emx2.sql.resolvers;

import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;

public interface RowValueResolver {

  void apply(Map<String, Object> javascriptContext, Column column, Row row);
}
