package org.molgenis.emx2.sql.resolvers;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;

import java.util.Map;

public class SystemRolePrefixResolver implements RowValueResolver {

  @Override
  public void apply(Map<String, Object> javascriptContext, Column column, Row row) {
    row.setString(
        column.getName(), Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
  }
}
